package com.mohamed.playstation.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mohamed.playstation.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: StockMovementEntity): Long

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY timestamp DESC, id DESC")
    fun getMovementsByProductId(productId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC, id DESC")
    fun getAllMovements(): Flow<List<StockMovementEntity>>

    @Query("DELETE FROM stock_movements WHERE productId = :productId")
    suspend fun deleteByProductId(productId: Long)
}
