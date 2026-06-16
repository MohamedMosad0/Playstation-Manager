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

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY timestamp DESC, id DESC")
    fun getMovementsByProductId(productId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC, id DESC")
    fun getAllMovements(): Flow<List<StockMovementEntity>>

    @Query("""
        SELECT sm.id,
               sm.productId,
               p.name    AS productName,
               sm.quantityChange,
               sm.movementType,
               sm.timestamp
        FROM stock_movements sm
        LEFT JOIN products p ON sm.productId = p.id
        ORDER BY sm.timestamp DESC, sm.id DESC
    """)
    fun getAllMovementsWithNames(): Flow<List<StockMovementWithName>>

    @Query("DELETE FROM stock_movements WHERE productId = :productId")
    suspend fun deleteByProductId(productId: Long)

    /** Result type for the LEFT JOIN query — mirrors the ProductDao.ProductSummaryRow pattern. */
    data class StockMovementWithName(
        val id: Long,
        val productId: Long,
        val productName: String?,
        val quantityChange: Int,
        val movementType: String,
        val timestamp: Date
    )
}
