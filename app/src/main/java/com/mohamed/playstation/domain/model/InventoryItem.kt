package com.mohamed.playstation.domain.model

import java.util.Date

data class InventoryItem(
    val id: Long = 0,
    val name: String,
    val sellPrice: Double,
    val costPerUnit: Double,
    val quantity: Int,
    val minimumQuantity: Int,
    val isPrepared: Boolean,
    val unitLabel: String,
    val isActive: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    val isOutOfStock: Boolean get() = quantity == 0
    val isLowStock: Boolean get() = quantity <= minimumQuantity && !isOutOfStock
}
