package com.mohamed.playstation.data.mapper

import com.mohamed.playstation.data.local.entity.InventoryItemEntity
import com.mohamed.playstation.domain.model.InventoryItem

fun InventoryItemEntity.toDomainModel() = InventoryItem(
    id = id,
    name = name,
    sellPrice = sellPrice,
    costPerUnit = costPerUnit,
    quantity = quantity,
    minimumQuantity = minimumQuantity,
    isPrepared = isPrepared,
    unitLabel = unitLabel,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun InventoryItem.toEntity() = InventoryItemEntity(
    id = id,
    name = name,
    sellPrice = sellPrice,
    costPerUnit = costPerUnit,
    quantity = quantity,
    minimumQuantity = minimumQuantity,
    isPrepared = isPrepared,
    unitLabel = unitLabel,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)
