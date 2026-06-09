package com.mohamed.playstation.data.mapper

import com.mohamed.playstation.data.local.entity.ProductEntity
import com.mohamed.playstation.domain.model.SessionProduct

/**
 * Mapper للتحويل بين ProductEntity و SessionProduct
 */
object ProductMapper {

    fun toModel(entity: ProductEntity): SessionProduct {
        return SessionProduct(
            id = entity.id,
            sessionId = entity.sessionId,
            name = entity.name,
            price = entity.price,
            quantity = entity.quantity,
            minimumQuantity = entity.minimumQuantity,
            createdAt = entity.createdAt
        )
    }

    fun toEntity(model: SessionProduct): ProductEntity {
        return ProductEntity(
            id = model.id,
            sessionId = model.sessionId,
            name = model.name,
            price = model.price,
            quantity = model.quantity,
            minimumQuantity = model.minimumQuantity,
            createdAt = model.createdAt
        )
    }

    fun toModelList(entities: List<ProductEntity>): List<SessionProduct> {
        return entities.map(::toModel)
    }
}
