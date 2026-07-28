package com.mohamed.playstation.domain.usecase

import com.mohamed.playstation.data.repository.InventoryRepository
import com.mohamed.playstation.domain.model.InventoryItem
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class InventoryUseCases @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    fun getAllActiveItems(): Flow<List<InventoryItem>> = inventoryRepository.getAllActiveItems()

    fun getAllArchivedItems(): Flow<List<InventoryItem>> = inventoryRepository.getAllArchivedItems()

    suspend fun insertItem(item: InventoryItem): Long {
        if (inventoryRepository.existsByName(item.name)) {
            throw IllegalArgumentException("DUPLICATE_PRODUCT_NAME")
        }
        return inventoryRepository.insertItem(item)
    }

    suspend fun updateItem(item: InventoryItem) {
        if (inventoryRepository.existsByNameExcluding(item.name, item.id)) {
            throw IllegalArgumentException("DUPLICATE_PRODUCT_NAME")
        }
        inventoryRepository.updateItem(item)
    }

    suspend fun getById(id: Long): InventoryItem? = inventoryRepository.getById(id)

    suspend fun archiveItem(id: Long) {
        inventoryRepository.archiveItem(id)
    }

    suspend fun restoreItem(id: Long) {
        inventoryRepository.restoreItem(id)
    }

    suspend fun adjustStock(id: Long, delta: Int, movementType: String) {
        inventoryRepository.adjustStock(id, delta, movementType)
    }
}
