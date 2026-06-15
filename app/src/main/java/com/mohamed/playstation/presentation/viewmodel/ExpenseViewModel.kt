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

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _totalAmount = MutableStateFlow(0.0)
    val totalAmount: StateFlow<Double> = _totalAmount.asStateFlow()

    private val _monthlyTotal = MutableStateFlow(0.0)
    val monthlyTotal: StateFlow<Double> = _monthlyTotal.asStateFlow()

    private val _expenseCount = MutableStateFlow(0)
    val expenseCount: StateFlow<Int> = _expenseCount.asStateFlow()

    private val _categoryTotals = MutableStateFlow<Map<ExpenseCategory, Double>>(emptyMap())
    val categoryTotals: StateFlow<Map<ExpenseCategory, Double>> = _categoryTotals.asStateFlow()

    init {
        loadExpenses()
    }

    private fun loadExpenses() {
        viewModelScope.launch {
            expenseUseCases.getAllExpenses().collect { list ->
                _expenses.value = list
                _totalAmount.value = list.sumOf { it.amount }
                _expenseCount.value = list.size
                
                // Calculate Category Totals
                _categoryTotals.value = list.groupBy { it.category }
                    .mapValues { (_, expenses) -> expenses.sumOf { it.amount } }

                // Calculate Monthly Total (Current Month)
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)
                
                _monthlyTotal.value = list.filter {
                    val expCal = Calendar.getInstance()
                    expCal.time = it.expenseDate
                    expCal.get(Calendar.MONTH) == currentMonth && expCal.get(Calendar.YEAR) == currentYear
                }.sumOf { it.amount }
            }
        }
    }

    fun addExpense(
        amount: Double,
        category: ExpenseCategory,
        description: String?,
        date: Date
    ) {
        viewModelScope.launch {
            try {
                expenseUseCases.addExpense(amount, category, description, date)
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseUseCases.deleteExpense(expense)
        }
    }
}
