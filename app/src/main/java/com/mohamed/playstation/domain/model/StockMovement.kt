package com.mohamed.playstation.domain.model

import java.util.Date

/**
 * Stock movement domain model
 */
data class StockMovement(
    val id: Long = 0,
    val inventoryItemId: Long,
    val quantityChange: Int,
    val movementType: MovementType,
    val timestamp: Date = Date()
)

enum class MovementType {
    STOCK_IN,
    STOCK_OUT,
    INITIAL,
    SALE,
    SALE_REVERT
}
