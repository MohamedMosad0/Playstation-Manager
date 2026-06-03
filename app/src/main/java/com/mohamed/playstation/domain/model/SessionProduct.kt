package com.mohamed.playstation.domain.model

import java.util.Date

/**
 * Domain Model لمنتج مرتبط بجلسة
 */
data class SessionProduct(
    val id: Long = 0,
    val sessionId: Long,
    val name: String,
    val price: Double,
    val quantity: Int,
    val createdAt: Date = Date()
) {
    fun getLineTotal(): Double = price * quantity
}
