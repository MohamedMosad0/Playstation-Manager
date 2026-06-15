package com.mohamed.playstation.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * جدول المصروفات في قاعدة البيانات
 */
@Entity(
    tableName = "expenses",
    indices = [Index("expenseDate")]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val category: String, // Enum name stored as string
    val description: String? = null,
    val expenseDate: Date,
    val createdAt: Date = Date()
)
