package com.mohamed.playstation.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mohamed.playstation.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object للمنتجات
 */
@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Query("SELECT * FROM products WHERE sessionId = :sessionId ORDER BY createdAt DESC, id DESC")
    fun getProductsBySessionId(sessionId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE sessionId = :sessionId ORDER BY createdAt DESC, id DESC")
    suspend fun getProductsBySessionIdOnce(sessionId: Long): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: Long): ProductEntity?

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

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteById(productId: Long)

    @Query("SELECT * FROM products WHERE sessionId = :sessionId AND LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getProductByNameInSession(sessionId: Long, name: String): ProductEntity?

    @Query("SELECT * FROM products WHERE sessionId = :sessionId AND LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND id != :excludeId LIMIT 1")
    suspend fun getProductByNameInSessionExcluding(sessionId: Long, name: String, excludeId: Long): ProductEntity?

    data class ProductSummaryRow(
        val sessionId: Long,
        val totalQuantity: Int,
        val totalAmount: Double
    )
}
