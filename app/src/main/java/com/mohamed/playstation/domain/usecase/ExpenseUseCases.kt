package com.mohamed.playstation.domain.usecase

import com.mohamed.playstation.data.repository.ExpenseRepository
import com.mohamed.playstation.domain.model.Expense
import com.mohamed.playstation.domain.model.ExpenseCategory
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use Cases للمصروفات
 */
class ExpenseUseCases @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {

    suspend fun addExpense(
        amount: Double,
        category: ExpenseCategory,
        description: String?,
        expenseDate: Date
    ): Long {
        require(amount > 0) { "Amount must be greater than zero" }

        val expense = Expense(
            amount = amount,
            category = category,
            description = description?.trim(),
            expenseDate = expenseDate
        )

        return expenseRepository.insertExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseRepository.deleteExpense(expense)
    }

    fun getAllExpenses(): Flow<List<Expense>> {
        return expenseRepository.getAllExpenses()
    }

    fun getExpensesInRange(startTime: Long, endTime: Long): Flow<List<Expense>> {
        return expenseRepository.getExpensesInRange(startTime, endTime)
    }

    fun getTotalExpensesInRange(startTime: Long, endTime: Long): Flow<Double> {
        return expenseRepository.getTotalExpensesInRange(startTime, endTime)
    }
}
