package com.mohamed.playstation.data.repository

import com.mohamed.playstation.data.local.dao.ExpenseDao
import com.mohamed.playstation.data.mapper.ExpenseMapper
import com.mohamed.playstation.domain.model.Expense
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository للتعامل مع المصروفات
 */
@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao
) {

    suspend fun insertExpense(expense: Expense): Long {
        return expenseDao.insert(ExpenseMapper.toEntity(expense))
    }

    suspend fun updateExpense(expense: Expense) {
        expenseDao.update(ExpenseMapper.toEntity(expense))
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.delete(ExpenseMapper.toEntity(expense))
    }

    fun getAllExpenses(): Flow<List<Expense>> {
        return expenseDao.getAllExpenses().map(ExpenseMapper::toModelList)
    }

    fun getExpensesInRange(startTime: Long, endTime: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesInRange(startTime, endTime).map(ExpenseMapper::toModelList)
    }

    fun getTotalExpensesInRange(startTime: Long, endTime: Long): Flow<Double> {
        return expenseDao.getTotalExpensesInRange(startTime, endTime).map { it ?: 0.0 }
    }
}
