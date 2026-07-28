package com.mohamed.playstation.data.repository

import com.mohamed.playstation.data.local.dao.StockMovementDao
import com.mohamed.playstation.data.mapper.StockMovementMapper
import com.mohamed.playstation.domain.model.StockMovement
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class StockMovementRepository @Inject constructor(
    private val stockMovementDao: StockMovementDao
) {
    suspend fun insertMovement(movement: StockMovement): Long {
        val entity = StockMovementMapper.toEntity(movement)
        return stockMovementDao.insert(entity)
    }

    fun getMovementsByInventoryItemId(inventoryItemId: Long): Flow<List<StockMovement>> {
        return stockMovementDao.getMovementsByInventoryItemId(inventoryItemId).map(StockMovementMapper::toModelList)
    }

    fun getAllMovements(): Flow<List<StockMovement>> {
        return stockMovementDao.getAllMovements().map(StockMovementMapper::toModelList)
    }

    fun getAllMovementsWithNames(): Flow<List<StockMovementDao.StockMovementWithName>> {
        return stockMovementDao.getAllMovementsWithNames()
    }

    suspend fun deleteByInventoryItemId(inventoryItemId: Long) {
        stockMovementDao.deleteByInventoryItemId(inventoryItemId)
    }
}
