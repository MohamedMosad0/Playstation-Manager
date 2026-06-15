package com.mohamed.playstation.data.local.dao

import androidx.room.*
import com.mohamed.playstation.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object للمصروفات
 */
@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses ORDER BY expenseDate DESC, id DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE expenseDate >= :startTime AND expenseDate <= :endTime ORDER BY expenseDate DESC, id DESC")
    fun getExpensesInRange(startTime: Long, endTime: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT SUM(amount) FROM expenses WHERE expenseDate >= :startTime AND expenseDate <= :endTime")
    fun getTotalExpensesInRange(startTime: Long, endTime: Long): Flow<Double?>
}
