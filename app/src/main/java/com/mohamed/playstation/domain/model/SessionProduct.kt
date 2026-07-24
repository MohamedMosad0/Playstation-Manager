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
    val createdAt: Date = Date(),
    val originalIds: List<Long> = emptyList()
) {
    fun getLineTotal(): Double = sellPriceSnapshot * quantitySold
    fun getLineCost(): Double = costSnapshot * quantitySold
    fun getLineProfit(): Double = getLineTotal() - getLineCost()

    companion object {
        fun calculateTotalAmount(products: List<SessionProduct>): Double {
            return products.sumOf { it.getLineTotal() }
        }
    }
}

/**
 * Aggregates a list of SessionProduct objects by grouping identical items.
 * Products are considered identical if they share the same inventoryItemId, nameSnapshot, and sellPriceSnapshot.
 * The aggregated product retains the sum of quantitySold and a list of all original database IDs for future delete/refund operations.
 */
fun List<SessionProduct>.aggregate(): List<SessionProduct> {
    return this.groupBy { Triple(it.inventoryItemId, it.nameSnapshot, it.sellPriceSnapshot) }
        .map { (_, group) ->
            val first = group.first()
            first.copy(
                quantitySold = group.sumOf { it.quantitySold },
                originalIds = group.map { it.id }
            )
        }
}
