package com.mohamed.playstation.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.playstation.data.local.SettingsManager
import com.mohamed.playstation.domain.model.Expense
import com.mohamed.playstation.domain.model.ExpenseCategory
import com.mohamed.playstation.domain.usecase.ExpenseUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseUseCases: ExpenseUseCases,
    settingsManager: SettingsManager
) : ViewModel() {

    val currency: StateFlow<String> = settingsManager.currencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "EGP")

    private val _dateFilterFlow = MutableStateFlow(com.mohamed.playstation.domain.model.filter.DateRangeFilter.TODAY)
    val dateFilterFlow: StateFlow<com.mohamed.playstation.domain.model.filter.DateRangeFilter> = _dateFilterFlow.asStateFlow()

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
    
    fun setDateFilter(filter: com.mohamed.playstation.domain.model.filter.DateRangeFilter) {
        _dateFilterFlow.value = filter
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _customStart.value = start
        _customEnd.value = end
        setDateFilter(com.mohamed.playstation.domain.model.filter.DateRangeFilter.CUSTOM)
    }

    private fun getRangeForFilter(
        range: com.mohamed.playstation.domain.model.filter.DateRangeFilter,
        customStart: Long,
        customEnd: Long
    ): Pair<Long, Long> {
        return when (range) {
            com.mohamed.playstation.domain.model.filter.DateRangeFilter.TODAY -> com.mohamed.playstation.core.utils.DateUtils.todayRange()
            com.mohamed.playstation.domain.model.filter.DateRangeFilter.THIS_WEEK -> com.mohamed.playstation.core.utils.DateUtils.thisWeekRange()
            com.mohamed.playstation.domain.model.filter.DateRangeFilter.LAST_7_DAYS -> com.mohamed.playstation.core.utils.DateUtils.last7DaysRange()
            com.mohamed.playstation.domain.model.filter.DateRangeFilter.THIS_MONTH -> com.mohamed.playstation.core.utils.DateUtils.thisMonthRange()
            com.mohamed.playstation.domain.model.filter.DateRangeFilter.LAST_MONTH -> com.mohamed.playstation.core.utils.DateUtils.lastMonthRange()
            com.mohamed.playstation.domain.model.filter.DateRangeFilter.LAST_30_DAYS -> com.mohamed.playstation.core.utils.DateUtils.last30DaysRange()
            com.mohamed.playstation.domain.model.filter.DateRangeFilter.LAST_3_MONTHS -> com.mohamed.playstation.core.utils.DateUtils.last3MonthsRange()
            com.mohamed.playstation.domain.model.filter.DateRangeFilter.ALL_TIME -> Pair(0L, Long.MAX_VALUE)
            com.mohamed.playstation.domain.model.filter.DateRangeFilter.CUSTOM -> {
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
