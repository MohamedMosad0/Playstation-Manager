package com.mohamed.playstation.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.playstation.core.utils.DateUtils
import com.mohamed.playstation.data.local.SettingsManager
import com.mohamed.playstation.domain.model.Expense
import com.mohamed.playstation.domain.model.ExpenseCategory
import com.mohamed.playstation.domain.model.filter.DateRangeFilter
import com.mohamed.playstation.domain.usecase.ExpenseUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseUseCases: ExpenseUseCases,
    settingsManager: SettingsManager
) : ViewModel() {

    val currency: StateFlow<String> = settingsManager.currencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "EGP")

    private val _dateFilterFlow = MutableStateFlow(DateRangeFilter.TODAY)
    val dateFilterFlow: StateFlow<DateRangeFilter> = _dateFilterFlow.asStateFlow()

    private val _customStart = MutableStateFlow<Long>(0L)
    private val _customEnd = MutableStateFlow<Long>(0L)

    private val filterTrigger = combine(
        dateFilterFlow,
        _customStart,
        _customEnd
    ) { filter, start, end ->
        Triple(filter, start, end)
    }

    val expenses: StateFlow<List<Expense>> = filterTrigger.flatMapLatest { (filter, customStart, customEnd) ->
        val (start, end) = getRangeForFilter(filter, customStart, customEnd)
        expenseUseCases.getExpensesInRange(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalAmount: StateFlow<Double> = expenses.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val expenseCount: StateFlow<Int> = expenses.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val categoryTotals: StateFlow<Map<ExpenseCategory, Double>> = expenses.map { list ->
        list.groupBy { it.category }.mapValues { (_, expenses) -> expenses.sumOf { it.amount } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    
    fun setDateFilter(filter: DateRangeFilter) {
        _dateFilterFlow.value = filter
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _customStart.value = start
        _customEnd.value = end
        setDateFilter(DateRangeFilter.CUSTOM)
    }

    private fun getRangeForFilter(
        range: DateRangeFilter,
        customStart: Long,
        customEnd: Long
    ): Pair<Long, Long> {
        return when (range) {
            DateRangeFilter.TODAY -> DateUtils.todayRange()
            DateRangeFilter.THIS_WEEK -> DateUtils.thisWeekRange()
            DateRangeFilter.LAST_7_DAYS -> DateUtils.last7DaysRange()
            DateRangeFilter.THIS_MONTH -> DateUtils.thisMonthRange()
            DateRangeFilter.LAST_MONTH -> DateUtils.lastMonthRange()
            DateRangeFilter.LAST_30_DAYS -> DateUtils.last30DaysRange()
            DateRangeFilter.LAST_3_MONTHS -> DateUtils.last3MonthsRange()
            DateRangeFilter.ALL_TIME -> Pair(0L, Long.MAX_VALUE)
            DateRangeFilter.CUSTOM -> {
                val endCal = Calendar.getInstance()
                endCal.timeInMillis = customEnd
                if (endCal.get(Calendar.HOUR_OF_DAY) == 0 && endCal.get(Calendar.MINUTE) == 0) {
                    endCal.add(Calendar.DAY_OF_YEAR, 1)
                    endCal.set(Calendar.HOUR_OF_DAY, 0)
                    endCal.set(Calendar.MINUTE, 0)
                    endCal.set(Calendar.SECOND, 0)
                    endCal.set(Calendar.MILLISECOND, 0)
                }
                Pair(customStart, endCal.timeInMillis)
            }
        }
    }

    fun addExpense(
        amount: Double,
        category: ExpenseCategory,
        description: String?,
        date: Date,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                expenseUseCases.addExpense(amount, category, description, date)
                onSuccess()
            } catch (e: Exception) {
                onError()
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseUseCases.deleteExpense(expense)
        }
    }
}
