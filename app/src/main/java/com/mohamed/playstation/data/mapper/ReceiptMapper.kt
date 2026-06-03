package com.mohamed.playstation.data.mapper

import com.mohamed.playstation.data.local.entity.ReceiptEntity
import com.mohamed.playstation.domain.model.Receipt

/**
 * Mapper للتحويل بين ReceiptEntity و Receipt
 */
object ReceiptMapper {

    /**
     * تحويل Entity إلى Domain Model
     */
    fun toModel(entity: ReceiptEntity): Receipt {
        return Receipt(
            id = entity.id,
            sessionId = entity.sessionId,
            receiptNumber = entity.receiptNumber,
            deviceType = entity.deviceType,
            deviceNumber = entity.deviceNumber,
            sessionType = entity.sessionType,
            startTime = entity.startTime,
            endTime = entity.endTime,
            durationMinutes = entity.durationMinutes,
            pricePerHour = entity.pricePerHour,
            totalAmount = entity.totalAmount,
            currencyCode = entity.currencyCode,
            paymentMethod = entity.paymentMethod,
            notes = entity.notes,
            createdAt = entity.createdAt
        )
    }

    /**
     * تحويل Domain Model إلى Entity
     */
    fun toEntity(model: Receipt): ReceiptEntity {
        return ReceiptEntity(
            id = model.id,
            sessionId = model.sessionId,
            receiptNumber = model.receiptNumber,
            deviceType = model.deviceType,
            deviceNumber = model.deviceNumber,
            sessionType = model.sessionType,
            startTime = model.startTime,
            endTime = model.endTime,
            durationMinutes = model.durationMinutes,
            pricePerHour = model.pricePerHour,
            totalAmount = model.totalAmount,
            currencyCode = model.currencyCode,
            paymentMethod = model.paymentMethod,
            notes = model.notes,
            createdAt = model.createdAt
        )
    }

    /**
     * تحويل قائمة Entities إلى قائمة Models
     */
    fun toModelList(entities: List<ReceiptEntity>): List<Receipt> {
        return entities.map { toModel(it) }
    }
}