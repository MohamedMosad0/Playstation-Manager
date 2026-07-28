package com.mohamed.playstation.data.repository

import androidx.room.withTransaction
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.data.local.AppDatabase
import com.mohamed.playstation.data.local.dao.InventoryItemDao
import com.mohamed.playstation.data.local.dao.SessionDao
import com.mohamed.playstation.data.local.dao.SessionProductDao
import com.mohamed.playstation.data.local.dao.StockMovementDao
import com.mohamed.playstation.data.local.entity.SessionProductEntity
import com.mohamed.playstation.data.local.entity.StockMovementEntity
import com.mohamed.playstation.data.mapper.toDomainModel
import com.mohamed.playstation.domain.model.SessionProduct
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SessionProductRepository @Inject constructor(
    private val sessionProductDao: SessionProductDao,
    private val sessionDao: SessionDao,
    private val inventoryItemDao: InventoryItemDao,
    private val stockMovementDao: StockMovementDao,
    private val database: AppDatabase
) {
    fun getProductsBySessionId(sessionId: Long): Flow<List<SessionProduct>> {
        return sessionProductDao.getProductsBySessionId(sessionId).map { list -> list.map { it.toDomainModel() } }
    }

    fun getProductsBySessionIds(sessionIds: List<Long>): Flow<List<SessionProduct>> {
        return sessionProductDao.getProductsBySessionIds(sessionIds).map { list -> list.map { it.toDomainModel() } }
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
            requireSessionCanBeMutated(sessionId)

            val inventoryItem = inventoryItemDao.getById(inventoryItemId)
                ?: throw IllegalArgumentException("PRODUCT_NOT_FOUND")

            if (inventoryItem.quantity < quantityToSell) {
                throw IllegalStateException("INSUFFICIENT_STOCK")
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
                ?: throw IllegalArgumentException("PRODUCT_NOT_FOUND")

            requireSessionCanBeMutated(sessionProduct.sessionId)

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

    private suspend fun requireSessionCanBeMutated(sessionId: Long) {
        val session = sessionDao.getSessionById(sessionId)
            ?: throw IllegalStateException("SESSION_NOT_ACTIVE")
        if (session.status != AppConstants.SESSION_STATUS_ACTIVE &&
            session.status != AppConstants.SESSION_STATUS_PAUSED
        ) {
            throw IllegalStateException("SESSION_NOT_ACTIVE")
        }
    }
}
