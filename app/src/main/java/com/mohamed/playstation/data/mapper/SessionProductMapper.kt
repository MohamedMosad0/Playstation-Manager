package com.mohamed.playstation.data.mapper

import com.mohamed.playstation.data.local.entity.SessionProductEntity
import com.mohamed.playstation.domain.model.SessionProduct

fun SessionProductEntity.toDomainModel() = SessionProduct(
    id = id,
    sessionId = sessionId,
    inventoryItemId = inventoryItemId,
    nameSnapshot = nameSnapshot,
    sellPriceSnapshot = sellPriceSnapshot,
    costSnapshot = costSnapshot,
    unitLabelSnapshot = unitLabelSnapshot,
    isPreparedSnapshot = isPreparedSnapshot,
    quantitySold = quantitySold,
    createdAt = createdAt
)

fun SessionProduct.toEntity() = SessionProductEntity(
    id = id,
    sessionId = sessionId,
    inventoryItemId = inventoryItemId,
    nameSnapshot = nameSnapshot,
    sellPriceSnapshot = sellPriceSnapshot,
    costSnapshot = costSnapshot,
    unitLabelSnapshot = unitLabelSnapshot,
    isPreparedSnapshot = isPreparedSnapshot,
    quantitySold = quantitySold,
    createdAt = createdAt
)
