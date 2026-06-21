package com.mohamed.playstation.data.local.dao

import androidx.room.*
import com.mohamed.playstation.data.local.entity.SessionProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sessionProduct: SessionProductEntity): Long

    @Update
    suspend fun update(sessionProduct: SessionProductEntity)

    @Delete
    suspend fun delete(sessionProduct: SessionProductEntity)

    @Query("SELECT * FROM session_products WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SessionProductEntity?

    @Query("SELECT * FROM session_products WHERE sessionId = :sessionId")
    fun getProductsBySessionId(sessionId: Long): Flow<List<SessionProductEntity>>

    @Query("SELECT * FROM session_products WHERE sessionId = :sessionId")
    suspend fun getProductsBySessionIdOnce(sessionId: Long): List<SessionProductEntity>

    @Query("SELECT * FROM session_products")
    fun getAllSessionProducts(): Flow<List<SessionProductEntity>>

    @Query("SELECT * FROM session_products WHERE inventoryItemId = :inventoryItemId")
    fun getProductsByInventoryItemId(inventoryItemId: Long): Flow<List<SessionProductEntity>>
}
