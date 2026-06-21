package com.mohamed.playstation.presentation.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.playstation.data.local.SettingsManager
import com.mohamed.playstation.data.repository.ExpenseRepository
import com.mohamed.playstation.data.repository.SessionProductRepository
import com.mohamed.playstation.data.repository.ReceiptRepository
import com.mohamed.playstation.domain.model.Receipt
import com.mohamed.playstation.core.constants.AppConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val expenseRepository: ExpenseRepository,
    private val sessionProductRepository: SessionProductRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    // Filter state
    private val _dateRange = MutableStateFlow(DateRangeFilter.ALL_TIME)
    private val _customStart = MutableStateFlow(0L)
    private val _customEnd = MutableStateFlow(Long.MAX_VALUE)

    // Using flatMapLatest to react to date range changes and fetch receipts and expenses
    private val receiptsFlow: Flow<List<Receipt>> = combine(_dateRange, _customStart, _customEnd) { range, start, end ->
        Triple(range, start, end)
    }.flatMapLatest { (range, customStart, customEnd) ->
        val (start, end) = getTimestampsForRange(range, customStart, customEnd)
        if (start == 0L && end == Long.MAX_VALUE) {
            receiptRepository.getAllReceipts()
        } else {
            // ReceiptDao uses createdAt
            receiptRepository.getReceiptsInRange(start, end)
        }
    }

    private val expensesFlow = combine(_dateRange, _customStart, _customEnd) { range, start, end ->
        Triple(range, start, end)
    }.flatMapLatest { (range, customStart, customEnd) ->
        val (start, end) = getTimestampsForRange(range, customStart, customEnd)
        if (start == 0L && end == Long.MAX_VALUE) {
            expenseRepository.getAllExpenses()
        } else {
            // ExpenseDao uses expenseDate
            expenseRepository.getExpensesInRange(start, end)
        }
    }

    // Product filtering based on Phase 5.1 approval: 
    // Load receipts -> extract sessionIds -> filter all products in memory
    private val productsFlow = combine(sessionProductRepository.getAllSessionProducts(), receiptsFlow) { allProducts, receipts ->
        val sessionIds = receipts.map { it.sessionId }.toSet()
        val soldProducts = allProducts.filter { 
            it.sessionId in sessionIds 
        }
        soldProducts
    }

    val uiState: StateFlow<ReportsUiState> = combine(
        receiptsFlow,
        expensesFlow,
        productsFlow,
        _dateRange,
        settingsManager.currencyFlow
    ) { receipts, expenses, products, dateRange, currency ->
        val sessionRevenue = receipts.sumOf { it.totalAmount }
        val productRevenue = products.sumOf { it.getLineTotal() }
        val productCost    = products.sumOf { it.getLineCost() }
        val productProfit  = productRevenue - productCost
        val totalRevenue   = sessionRevenue + productRevenue
        val totalExpenses  = expenses.sumOf { it.amount }
        val netProfit      = sessionRevenue + productProfit - totalExpenses

        // Show warning banner if any product in this range has no cost info
        val hasHistoricalCostGap = products.any { it.costSnapshot == 0.0 }
        
        val avgDuration = if (receipts.isNotEmpty()) {
            receipts.map { it.durationMinutes }.average().toLong()
        } else {
            0L
        }

        // Top 5 Products
        val topProducts = products
            .groupBy { it.nameSnapshot }
            .map { (name, group) ->
                val qty = group.sumOf { it.quantitySold }
                val rev = group.sumOf { it.getLineTotal() }
                val sample = group.first()
                TopProductItem(name, qty, rev, sample.isPreparedSnapshot, sample.unitLabelSnapshot)
            }
            .sortedByDescending { it.quantitySold }
            .take(5)

        // Revenue Distribution Pie
        val revenueDistribution = mapOf(
            "إيرادات الجلسات" to sessionRevenue,
            "إيرادات المنتجات" to productRevenue
        ).filterValues { it > 0 }

        // Device Distribution Pie
        val deviceDistribution = receipts
            .groupBy { it.deviceType }
            .mapValues { it.value.size }

        // Bar Chart (Last 7 Days Revenue - dynamic based on the dates of receipts)
        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
        val revenueLast7DaysMap = mutableMapOf<String, Double>()
        
        // Group by day of year (using receipt.endTime or createdAt)
        receipts.forEach { receipt ->
            val dayLabel = dateFormat.format(receipt.createdAt)
            revenueLast7DaysMap[dayLabel] = (revenueLast7DaysMap[dayLabel] ?: 0.0) + receipt.totalAmount
        }
        
        // Match products to the correct day using their sessionId
        val sessionDateMap = receipts.associate { it.sessionId to dateFormat.format(it.createdAt) }
        products.forEach { product ->
            val dayLabel = sessionDateMap[product.sessionId]
            if (dayLabel != null) {
                revenueLast7DaysMap[dayLabel] = (revenueLast7DaysMap[dayLabel] ?: 0.0) + product.getLineTotal()
            }
        }
        
        val revenueLast7DaysList = revenueLast7DaysMap.entries
            .sortedBy { it.key }
            .takeLast(7)
            .map { Pair(it.key, it.value) }

        val rangeLabel = when (dateRange) {
            DateRangeFilter.TODAY -> "اليوم"
            DateRangeFilter.LAST_7_DAYS -> "آخر 7 أيام"
            DateRangeFilter.THIS_MONTH -> "هذا الشهر"
            DateRangeFilter.LAST_30_DAYS -> "آخر 30 يوم"
            DateRangeFilter.ALL_TIME -> "الكل"
            DateRangeFilter.CUSTOM -> "مخصص"
        }

        ReportsUiState(
            isLoading = false,
            dateRangeLabel = rangeLabel,
            totalRevenue = totalRevenue,
            sessionRevenue = sessionRevenue,
            productRevenue = productRevenue,
            productCost = productCost,
            productProfit = productProfit,
            netProfit = netProfit,
            totalExpenses = totalExpenses,
            totalSessions = receipts.size,
            avgSessionDurationMinutes = avgDuration,
            revenueLast7Days = revenueLast7DaysList,
            revenueDistribution = revenueDistribution,
            deviceDistribution = deviceDistribution,
            topProducts = topProducts,
            isProfitAvailable = true,
            hasHistoricalCostGap = hasHistoricalCostGap,
            currency = currency
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportsUiState(isLoading = true))

    fun setDateFilter(filter: DateRangeFilter) {
        _dateRange.value = filter
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _customStart.value = start
        _customEnd.value = end
        _dateRange.value = DateRangeFilter.CUSTOM
    }

    private fun getTimestampsForRange(range: DateRangeFilter, customStart: Long, customEnd: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        return when (range) {
            DateRangeFilter.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            DateRangeFilter.LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                Pair(calendar.timeInMillis, now)
            }
            DateRangeFilter.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            DateRangeFilter.LAST_30_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                Pair(calendar.timeInMillis, now)
            }
            DateRangeFilter.ALL_TIME -> {
                Pair(0L, Long.MAX_VALUE)
            }
            DateRangeFilter.CUSTOM -> {
                // For custom, ensure end is end of day
                val endCal = Calendar.getInstance().apply { 
                    timeInMillis = customEnd
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                Pair(customStart, endCal.timeInMillis)
            }
        }
    }
}

enum class DateRangeFilter {
    TODAY, LAST_7_DAYS, THIS_MONTH, LAST_30_DAYS, ALL_TIME, CUSTOM
}
