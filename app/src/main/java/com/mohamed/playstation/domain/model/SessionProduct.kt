package com.mohamed.playstation.domain.model

import java.util.Date

/**
 * Domain Model لمنتج مرتبط بجلسة (مبيعات الجلسة)
 */
data class SessionProduct(
    val id: Long = 0,
    val sessionId: Long,
    val inventoryItemId: Long,
    val nameSnapshot: String,
    val sellPriceSnapshot: Double,
    val costSnapshot: Double,
    val unitLabelSnapshot: String,
    val isPreparedSnapshot: Boolean,
    val quantitySold: Int,
    val createdAt: Date = Date()
) {
    fun getLineTotal(): Double = sellPriceSnapshot * quantitySold
    fun getLineCost(): Double = costSnapshot * quantitySold
    fun getLineProfit(): Double = getLineTotal() - getLineCost()
}
