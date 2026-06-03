package com.mohamed.playstation.domain.model

/**
 * ملخص المنتجات الخاص بالجلسة
 */
data class SessionProductSummary(
    val sessionId: Long,
    val totalQuantity: Int,
    val totalAmount: Double
)
