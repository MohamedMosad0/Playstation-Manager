package com.mohamed.playstation.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mohamed.playstation.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface StockMovementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: StockMovementEntity): Long

    @Query("SELECT * FROM stock_movements WHERE inventoryItemId = :inventoryItemId ORDER BY timestamp DESC, id DESC")
    fun getMovementsByInventoryItemId(inventoryItemId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC, id DESC")
    fun getAllMovements(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements")
    suspend fun getAllOnce(): List<StockMovementEntity>

    @Query("""
        SELECT sm.id,
               sm.inventoryItemId,
               p.name    AS productName,
               sm.quantityChange,
               sm.movementType,
               sm.timestamp
        FROM stock_movements sm
        LEFT JOIN inventory_items p ON sm.inventoryItemId = p.id
        ORDER BY sm.timestamp DESC, sm.id DESC
    """)
    fun getAllMovementsWithNames(): Flow<List<StockMovementWithName>>

    @Query("DELETE FROM stock_movements WHERE inventoryItemId = :inventoryItemId")
    suspend fun deleteByInventoryItemId(inventoryItemId: Long)

    data class StockMovementWithName(
        val id: Long,
        val inventoryItemId: Long,
        val productName: String?,
        val quantityChange: Int,
        val movementType: String,
        val timestamp: Date
    )
}
