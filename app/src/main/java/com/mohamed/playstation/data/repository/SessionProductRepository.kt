package com.mohamed.playstation.data.repository

import androidx.room.withTransaction
import com.mohamed.playstation.data.local.AppDatabase
import com.mohamed.playstation.data.local.dao.InventoryItemDao
import com.mohamed.playstation.data.local.dao.SessionProductDao
import com.mohamed.playstation.data.local.dao.StockMovementDao
import com.mohamed.playstation.data.local.entity.SessionProductEntity
import com.mohamed.playstation.data.local.entity.StockMovementEntity
import com.mohamed.playstation.data.mapper.toDomainModel
import com.mohamed.playstation.domain.model.SessionProduct
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionProductRepository @Inject constructor(
    private val sessionProductDao: SessionProductDao,
    private val inventoryItemDao: InventoryItemDao,
    private val stockMovementDao: StockMovementDao,
    private val database: AppDatabase
) {
    fun getProductsBySessionId(sessionId: Long): Flow<List<SessionProduct>> {
        return sessionProductDao.getProductsBySessionId(sessionId).map { list -> list.map { it.toDomainModel() } }
    }

    suspend fun getProductsBySessionIdOnce(sessionId: Long): List<SessionProduct> {
        return sessionProductDao.getProductsBySessionIdOnce(sessionId).map { it.toDomainModel() }
    }

    fun getAllSessionProducts(): Flow<List<SessionProduct>> {
        return sessionProductDao.getAllSessionProducts().map { list -> list.map { it.toDomainModel() } }
    }

    suspend fun addInventoryProductToSession(
        sessionId: Long,
        inventoryItemId: Long,
        quantityToSell: Int
    ) {
        database.withTransaction {
            val inventoryItem = inventoryItemDao.getById(inventoryItemId)
                ?: throw IllegalArgumentException("المنتج غير موجود في المخزون")

            if (inventoryItem.quantity < quantityToSell) {
                throw IllegalStateException("الكمية المطلوبة غير متوفرة في المخزون")
            }

            // Decrease stock and log movement
            inventoryItemDao.adjustQuantity(inventoryItemId, -quantityToSell)
            stockMovementDao.insert(
                StockMovementEntity(
                    inventoryItemId = inventoryItemId,
                    quantityChange = -quantityToSell,
                    movementType = "SALE"
                )
            )

            // Insert Snapshot
            val sessionProduct = SessionProductEntity(
                sessionId = sessionId,
                inventoryItemId = inventoryItemId,
                nameSnapshot = inventoryItem.name,
                sellPriceSnapshot = inventoryItem.sellPrice,
                costSnapshot = inventoryItem.costPerUnit,
                unitLabelSnapshot = inventoryItem.unitLabel,
                isPreparedSnapshot = inventoryItem.isPrepared,
                quantitySold = quantityToSell
            )
            sessionProductDao.insert(sessionProduct)
        }
    }

    suspend fun removeSessionProductAndRestoreStock(sessionProductId: Long) {
        database.withTransaction {
            val sessionProduct = sessionProductDao.getById(sessionProductId)
                ?: throw IllegalArgumentException("المنتج غير موجود")

            sessionProductDao.delete(sessionProduct)

            // Restore stock
            // If inventory item was physically deleted, we skip restoring. But since we soft-delete, it's usually there.
            val inventoryItem = inventoryItemDao.getById(sessionProduct.inventoryItemId)
            if (inventoryItem != null) {
                inventoryItemDao.adjustQuantity(sessionProduct.inventoryItemId, sessionProduct.quantitySold)
                stockMovementDao.insert(
                    StockMovementEntity(
                        inventoryItemId = sessionProduct.inventoryItemId,
                        quantityChange = sessionProduct.quantitySold,
                        movementType = "SALE_REVERT"
                    )
                )
            }
        }
    }
}
