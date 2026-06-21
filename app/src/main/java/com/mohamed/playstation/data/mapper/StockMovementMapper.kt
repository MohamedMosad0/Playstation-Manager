package com.mohamed.playstation.data.mapper

import com.mohamed.playstation.data.local.entity.StockMovementEntity
import com.mohamed.playstation.domain.model.MovementType
import com.mohamed.playstation.domain.model.StockMovement

object StockMovementMapper {

    fun toModel(entity: StockMovementEntity): StockMovement {
        val type = try {
            MovementType.valueOf(entity.movementType)
        } catch (e: Exception) {
            MovementType.STOCK_IN
        }

        return StockMovement(
            id = entity.id,
            inventoryItemId = entity.inventoryItemId,
            quantityChange = entity.quantityChange,
            movementType = type,
            timestamp = entity.timestamp
        )
    }

    fun toEntity(model: StockMovement): StockMovementEntity {
        return StockMovementEntity(
            id = model.id,
            inventoryItemId = model.inventoryItemId,
            quantityChange = model.quantityChange,
            movementType = model.movementType.name,
            timestamp = model.timestamp
        )
    }

    fun toModelList(entities: List<StockMovementEntity>): List<StockMovement> = entities.map(::toModel)
}
