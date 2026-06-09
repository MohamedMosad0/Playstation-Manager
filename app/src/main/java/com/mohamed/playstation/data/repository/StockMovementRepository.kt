package com.mohamed.playstation.data.repository

import com.mohamed.playstation.data.local.dao.StockMovementDao
import com.mohamed.playstation.data.mapper.StockMovementMapper
import com.mohamed.playstation.domain.model.StockMovement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockMovementRepository @Inject constructor(
    private val stockMovementDao: StockMovementDao
) {

    suspend fun insertMovement(movement: StockMovement): Long {
        return stockMovementDao.insert(StockMovementMapper.toEntity(movement))
    }

    fun getMovementsByProductId(productId: Long): Flow<List<StockMovement>> {
        return stockMovementDao.getMovementsByProductId(productId).map(StockMovementMapper::toModelList)
    }

    fun getAllMovements(): Flow<List<StockMovement>> {
        return stockMovementDao.getAllMovements().map(StockMovementMapper::toModelList)
    }

    suspend fun deleteByProductId(productId: Long) {
        stockMovementDao.deleteByProductId(productId)
    }
}
