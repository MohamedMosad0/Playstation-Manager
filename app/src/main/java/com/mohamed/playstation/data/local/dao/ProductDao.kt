package com.mohamed.playstation.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mohamed.playstation.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object للمنتجات
 */
@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity): Long

    @Query("SELECT * FROM products WHERE sessionId = :sessionId ORDER BY createdAt DESC, id DESC")
    fun getProductsBySessionId(sessionId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE sessionId = :sessionId ORDER BY createdAt DESC, id DESC")
    suspend fun getProductsBySessionIdOnce(sessionId: Long): List<ProductEntity>

    @Query(
        """
        SELECT
            sessionId,
            COALESCE(SUM(quantity), 0) AS totalQuantity,
            COALESCE(SUM(price * quantity), 0) AS totalAmount
        FROM products
        GROUP BY sessionId
        """
    )
    fun getAllSessionProductSummaries(): Flow<List<ProductSummaryRow>>

    data class ProductSummaryRow(
        val sessionId: Long,
        val totalQuantity: Int,
        val totalAmount: Double
    )
}
