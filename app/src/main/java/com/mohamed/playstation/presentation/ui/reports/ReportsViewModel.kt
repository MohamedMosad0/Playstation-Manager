package com.mohamed.playstation.presentation.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.playstation.R
import com.mohamed.playstation.core.utils.AppFormatters
import com.mohamed.playstation.core.utils.DateUtils
import com.mohamed.playstation.data.repository.ExpenseRepository
import com.mohamed.playstation.data.repository.ReceiptRepository
import com.mohamed.playstation.data.repository.SessionProductRepository
import com.mohamed.playstation.data.repository.settings.SettingsRepository
import com.mohamed.playstation.domain.model.Expense
import com.mohamed.playstation.domain.model.Receipt
import com.mohamed.playstation.domain.model.SessionProduct
import com.mohamed.playstation.domain.model.filter.DateRangeFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val expenseRepository: ExpenseRepository,
    private val sessionProductRepository: SessionProductRepository,
    private val settingsRepository: SettingsRepository,
    @Named("reports_computation_dispatcher")
    private val computationDispatcher: CoroutineDispatcher
) : ViewModel() {

    // Filter state
    private val _dateRange = MutableStateFlow(DateRangeFilter.ALL_TIME)
    private val _customStart = MutableStateFlow(0L)
    private val _customEnd = MutableStateFlow(Long.MAX_VALUE)

    private data class FilterTrigger(
        val filter: DateRangeFilter,
        val customStart: Long,
        val customEnd: Long,
        val rolloverTick: Long
    )

    private val filterTrigger = combine(
        _dateRange,
        _customStart,
        _customEnd,
        DateUtils.dayRolloverFlow()
    ) { range, start, end, tick ->
        FilterTrigger(range, start, end, tick)
    }

    private data class ReportRawData(
        val receipts: List<Receipt>,
        val expenses: List<Expense>,
        val products: List<SessionProduct>,
        val filter: DateRangeFilter
    )

    private val reportDataFlow: Flow<ReportRawData> =
        filterTrigger.flatMapLatest { trigger ->
            val (start, end) = getTimestampsForRange(trigger.filter, trigger.customStart, trigger.customEnd, trigger.rolloverTick)
            val receipts = if (start == 0L && end == Long.MAX_VALUE) {
                receiptRepository.getAllReceipts()
            } else {
                // ReceiptDao uses createdAt
                receiptRepository.getReceiptsInRange(start, end)
            }
            val expenses = if (start == 0L && end == Long.MAX_VALUE) {
                expenseRepository.getAllExpenses()
            } else {
                // ExpenseDao uses expenseDate
                expenseRepository.getExpensesInRange(start, end)
            }
            val products = if (start == 0L && end == Long.MAX_VALUE) {
                sessionProductRepository.getAllSessionProducts()
            } else {
                sessionProductRepository.getProductsByReceiptDateRange(start, end)
            }
            combine(receipts, expenses, products) { r, e, p ->
                ReportRawData(r, e, p, trigger.filter)
            }
        }

    private val settingsFlow = combine(
        settingsRepository.currencyFlow.distinctUntilChanged(),
        settingsRepository.languageFlow.distinctUntilChanged()
    ) { currency, language ->
        currency to language
    }

    val uiState: StateFlow<ReportsUiState> = combine(
        reportDataFlow,
        settingsFlow
    ) { rawData, (currency, language) ->
        val receipts = rawData.receipts
        val expenses = rawData.expenses
        val products = rawData.products
        val dateRange = rawData.filter
        // Single-pass aggregation over receipts — replaces 6 separate iterations
        var totalRevenue = 0.0
        var productRevenue = 0.0
        var sessionRevenue = 0.0
        var totalDiscounts = 0.0
        var totalTaxes = 0.0
        var durationSum = 0L

        val revenueLast7DaysMap = mutableMapOf<Long, Double>()
        val deviceCountMap = mutableMapOf<String, Int>()

        for (receipt in receipts) {
            totalRevenue += receipt.totalAmount
            productRevenue += receipt.productsAmount
            sessionRevenue += receipt.playAmount
            totalDiscounts += receipt.discountAmount
            totalTaxes += receipt.taxAmount
            durationSum += receipt.durationMinutes

            // Chart data (was a separate forEach loop)
            val dayStartMillis = startOfDay(receipt.createdAt)
            revenueLast7DaysMap[dayStartMillis] =
                (revenueLast7DaysMap[dayStartMillis] ?: 0.0) + receipt.totalAmount

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
            .map { (dayStartMillis, revenue) ->
                Pair(AppFormatters.formatChartDay(java.util.Date(dayStartMillis), language), revenue)
            }

        val rangeLabel = when (dateRange) {
            DateRangeFilter.TODAY -> R.string.filter_today
            DateRangeFilter.THIS_WEEK -> R.string.filter_this_week
            DateRangeFilter.LAST_7_DAYS -> R.string.filter_last_7_days
            DateRangeFilter.THIS_MONTH -> R.string.filter_this_month
            DateRangeFilter.LAST_MONTH -> R.string.filter_last_month
            DateRangeFilter.LAST_30_DAYS -> R.string.filter_last_30_days
            DateRangeFilter.LAST_3_MONTHS -> R.string.filter_last_3_months
            DateRangeFilter.ALL_TIME -> R.string.filter_all
            DateRangeFilter.CUSTOM -> R.string.filter_custom
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
    }.flowOn(computationDispatcher)
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

    private fun startOfDay(date: java.util.Date): Long = Calendar.getInstance().run {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

    private fun getTimestampsForRange(
        range: DateRangeFilter,
        customStart: Long,
        customEnd: Long,
        now: Long = DateUtils.currentTimeMillis()
    ): Pair<Long, Long> {
        return when (range) {
            DateRangeFilter.TODAY -> DateUtils.todayRange(now)
            DateRangeFilter.THIS_WEEK -> DateUtils.thisWeekRange(now)
            DateRangeFilter.LAST_7_DAYS -> DateUtils.last7DaysRange(now)
            DateRangeFilter.THIS_MONTH -> DateUtils.thisMonthRange(now)
            DateRangeFilter.LAST_MONTH -> DateUtils.lastMonthRange(now)
            DateRangeFilter.LAST_30_DAYS -> DateUtils.last30DaysRange(now)
            DateRangeFilter.LAST_3_MONTHS -> DateUtils.last3MonthsRange(now)
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
