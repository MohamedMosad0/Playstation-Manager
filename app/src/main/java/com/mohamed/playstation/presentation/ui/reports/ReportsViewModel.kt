package com.mohamed.playstation.presentation.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.playstation.domain.model.filter.DateRangeFilter
import com.mohamed.playstation.data.local.SettingsManager
import com.mohamed.playstation.data.repository.ExpenseRepository
import com.mohamed.playstation.data.repository.ReceiptRepository
import com.mohamed.playstation.data.repository.SessionProductRepository
import com.mohamed.playstation.domain.model.Receipt
import com.mohamed.playstation.domain.model.SessionProduct
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
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
    private val receiptsFlow: Flow<List<Receipt>> =
        combine(_dateRange, _customStart, _customEnd) { range, start, end ->
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
    // Filtered by DB instead of in-memory to improve performance.
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val productsFlow = receiptsFlow.flatMapLatest { receipts ->
        val sessionIds = receipts.map { it.sessionId }
        if (sessionIds.isEmpty()) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            sessionProductRepository.getProductsBySessionIds(sessionIds)
        }
    }

    val uiState: StateFlow<ReportsUiState> = combine(
        receiptsFlow,
        expensesFlow,
        productsFlow,
        _dateRange,
        settingsManager.currencyFlow
    ) { receipts, expenses, products, dateRange, currency ->
        // Single-pass aggregation over receipts — replaces 6 separate iterations
        var totalRevenue = 0.0
        var productRevenue = 0.0
        var sessionRevenue = 0.0
        var totalDiscounts = 0.0
        var totalTaxes = 0.0
        var durationSum = 0L

        val revenueLast7DaysMap = mutableMapOf<String, Double>()
        val deviceCountMap = mutableMapOf<String, Int>()

        for (receipt in receipts) {
            totalRevenue += receipt.totalAmount
            productRevenue += receipt.productsAmount
            sessionRevenue += receipt.playAmount
            totalDiscounts += receipt.discountAmount
            totalTaxes += receipt.taxAmount
            durationSum += receipt.durationMinutes

            // Chart data (was a separate forEach loop)
            val dayLabel = com.mohamed.playstation.core.utils.AppFormatters.formatChartDay(receipt.createdAt)
            revenueLast7DaysMap[dayLabel] =
                (revenueLast7DaysMap[dayLabel] ?: 0.0) + receipt.totalAmount

            // Device distribution (was a separate groupBy + mapValues)
            deviceCountMap[receipt.deviceType] =
                (deviceCountMap[receipt.deviceType] ?: 0) + 1
        }

        val totalExpenses = expenses.sumOf { it.amount }
        val productCost = products.sumOf { it.getLineCost() }
        val productProfit = productRevenue - productCost
        val netProfit = sessionRevenue + productProfit - totalExpenses

        // Avoid intermediate List allocation from map{}.average()
        val avgDuration = if (receipts.isNotEmpty()) {
            durationSum / receipts.size
        } else {
            0L
        }

        // Top 5 Products
        val topProducts = products
            .groupBy { it.nameSnapshot }
            .map { (name, group) ->
                val qty = group.sumOf { it.quantitySold }
                val rev = SessionProduct.calculateTotalAmount(group)
                val sample = group.first()
                TopProductItem(name, qty, rev, sample.isPreparedSnapshot, sample.unitLabelSnapshot)
            }
            .sortedByDescending { it.quantitySold }
            .take(5)

        // Revenue Distribution Pie
        val revenueDistribution = buildMap<String, Double>(2) {
            if (sessionRevenue > 0) put("session_revenue", sessionRevenue)
            if (productRevenue > 0) put("product_revenue", productRevenue)
        }

        // Bar Chart — reduce intermediate allocations
        val revenueLast7DaysList = revenueLast7DaysMap.entries
            .sortedBy { it.key }
            .takeLast(7)
            .map { Pair(it.key, it.value) }

        val rangeLabel = when (dateRange) {
            DateRangeFilter.TODAY -> com.mohamed.playstation.R.string.filter_today
            DateRangeFilter.THIS_WEEK -> com.mohamed.playstation.R.string.filter_this_week
            DateRangeFilter.LAST_7_DAYS -> com.mohamed.playstation.R.string.filter_last_7_days
            DateRangeFilter.THIS_MONTH -> com.mohamed.playstation.R.string.filter_this_month
            DateRangeFilter.LAST_MONTH -> com.mohamed.playstation.R.string.filter_last_month
            DateRangeFilter.LAST_30_DAYS -> com.mohamed.playstation.R.string.filter_last_30_days
            DateRangeFilter.LAST_3_MONTHS -> com.mohamed.playstation.R.string.filter_last_3_months
            DateRangeFilter.ALL_TIME -> com.mohamed.playstation.R.string.filter_all
            DateRangeFilter.CUSTOM -> com.mohamed.playstation.R.string.filter_custom
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
            totalDiscounts = totalDiscounts,
            totalTaxes = totalTaxes,
            totalSessions = receipts.size,
            avgSessionDurationMinutes = avgDuration,
            revenueLast7Days = revenueLast7DaysList,
            revenueDistribution = revenueDistribution,
            deviceDistribution = deviceCountMap,
            topProducts = topProducts,
            isProfitAvailable = true,

            currency = currency
        )
    }.flowOn(Dispatchers.Default)
    .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ReportsUiState(isLoading = true)
    )

    fun setDateFilter(filter: DateRangeFilter) {
        _dateRange.value = filter
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _customStart.value = start
        _customEnd.value = end
        _dateRange.value = DateRangeFilter.CUSTOM
    }

    private fun getTimestampsForRange(
        range: DateRangeFilter,
        customStart: Long,
        customEnd: Long
    ): Pair<Long, Long> {
        return when (range) {
            DateRangeFilter.TODAY -> com.mohamed.playstation.core.utils.DateUtils.todayRange()
            DateRangeFilter.THIS_WEEK -> com.mohamed.playstation.core.utils.DateUtils.thisWeekRange()
            DateRangeFilter.LAST_7_DAYS -> com.mohamed.playstation.core.utils.DateUtils.last7DaysRange()
            DateRangeFilter.THIS_MONTH -> com.mohamed.playstation.core.utils.DateUtils.thisMonthRange()
            DateRangeFilter.LAST_MONTH -> com.mohamed.playstation.core.utils.DateUtils.lastMonthRange()
            DateRangeFilter.LAST_30_DAYS -> com.mohamed.playstation.core.utils.DateUtils.last30DaysRange()
            DateRangeFilter.LAST_3_MONTHS -> com.mohamed.playstation.core.utils.DateUtils.last3MonthsRange()
            DateRangeFilter.ALL_TIME -> Pair(0L, Long.MAX_VALUE)
            DateRangeFilter.CUSTOM -> {
                // For custom, ensure end is start of next day for exclusive bound
                val endCal = Calendar.getInstance().apply {
                    timeInMillis = customEnd
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                Pair(customStart, endCal.timeInMillis)
            }
        }
    }
}

