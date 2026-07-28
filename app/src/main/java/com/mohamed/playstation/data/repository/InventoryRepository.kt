package com.mohamed.playstation.data.repository

import androidx.room.withTransaction
import com.mohamed.playstation.data.local.AppDatabase
import com.mohamed.playstation.data.local.dao.InventoryItemDao
import com.mohamed.playstation.data.local.dao.StockMovementDao
import com.mohamed.playstation.data.local.entity.StockMovementEntity
import com.mohamed.playstation.data.mapper.toDomainModel
import com.mohamed.playstation.data.mapper.toEntity
import com.mohamed.playstation.domain.model.InventoryItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class InventoryRepository @Inject constructor(
    private val inventoryItemDao: InventoryItemDao,
    private val stockMovementDao: StockMovementDao,
    private val database: AppDatabase
) {
    fun getAllActiveItems(): Flow<List<InventoryItem>> {
        return inventoryItemDao.getAllActiveItems().map { it.map { entity -> entity.toDomainModel() } }
    }

    fun getAllArchivedItems(): Flow<List<InventoryItem>> {
        return inventoryItemDao.getAllArchivedItems().map { it.map { entity -> entity.toDomainModel() } }
    }

    fun getActiveInventoryItemsCount(): Flow<Int> {
        return inventoryItemDao.getActiveInventoryItemsCount()
    }

    fun getLowStockInventoryItemsCount(): Flow<Int> {
        return inventoryItemDao.getLowStockInventoryItemsCount()
    }

    suspend fun insertItem(item: InventoryItem): Long {
        return database.withTransaction {
            val id = inventoryItemDao.insert(item.toEntity())
            stockMovementDao.insert(
                StockMovementEntity(
                    inventoryItemId = id,
                    quantityChange = item.quantity,
                    movementType = "INITIAL"
                )
            )
            id
        }
    }

    suspend fun updateItem(item: InventoryItem) {
        database.withTransaction {
            val oldEntity = inventoryItemDao.getById(item.id) ?: throw IllegalArgumentException("Item not found")
            val delta = item.quantity - oldEntity.quantity
            inventoryItemDao.update(item.toEntity())
            if (delta > 0) {
                stockMovementDao.insert(
                    StockMovementEntity(
                        inventoryItemId = item.id,
                        quantityChange = delta,
                        movementType = "MANUAL_ADD"
                    )
                )
            } else if (delta < 0) {
                stockMovementDao.insert(
                    StockMovementEntity(
                        inventoryItemId = item.id,
                        quantityChange = delta,
                        movementType = "MANUAL_DEDUCT"
                    )
                )
            }
        }
    }

    suspend fun getById(id: Long): InventoryItem? {
        return inventoryItemDao.getById(id)?.toDomainModel()
    }

    suspend fun archiveItem(id: Long) {
        inventoryItemDao.archiveItem(id)
    }

    suspend fun restoreItem(id: Long) {
        inventoryItemDao.restoreItem(id)
    }

    suspend fun adjustStock(id: Long, delta: Int, movementType: String) {
        database.withTransaction {
            val item = inventoryItemDao.getById(id) ?: throw IllegalArgumentException("Item not found")
            if (item.quantity + delta < 0) {
                throw IllegalStateException("INSUFFICIENT_STOCK")
            }
            inventoryItemDao.adjustQuantity(id, delta)
            stockMovementDao.insert(
                StockMovementEntity(
                    inventoryItemId = id,
                    quantityChange = delta,
                    movementType = movementType
                )
            )
        }
    }

    suspend fun existsByName(name: String): Boolean {
        return inventoryItemDao.existsByName(name)
    }

    suspend fun existsByNameExcluding(name: String, excludeId: Long): Boolean {
        return inventoryItemDao.existsByNameExcluding(name, excludeId)
    }
}
