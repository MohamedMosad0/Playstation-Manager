package com.mohamed.playstation.domain.model

import java.util.*

/**
 * Domain Model للفاتورة
 */
data class Receipt(
    val id: Long = 0,
    val sessionId: Long,
    val receiptNumber: String,
    val deviceType: String,
    val deviceNumber: Int,
    val sessionType: String,
    val startTime: Date,
    val endTime: Date,
    val durationMinutes: Long,
    val pricePerHour: Double,
    val playAmount: Double,
    val productsAmount: Double,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double,
    val currencyCode: String,
    val paymentMethod: String? = null,
    val notes: String? = null,
    val createdAt: Date = Date()
)
