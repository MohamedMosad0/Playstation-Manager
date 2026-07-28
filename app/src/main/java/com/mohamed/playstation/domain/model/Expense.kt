package com.mohamed.playstation.domain.model

import java.util.Date

/**
 * Domain Model للمصروفات
 */
data class Expense(
    val id: Long = 0,
    val amount: Double,
    val category: ExpenseCategory,
    val description: String? = null,
    val expenseDate: Date,
    val createdAt: Date = Date()
)
