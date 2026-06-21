package com.mohamed.playstation.data.local.dao

import androidx.room.*
import com.mohamed.playstation.data.local.entity.InventoryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InventoryItemEntity): Long

    @Update
    suspend fun update(item: InventoryItemEntity)

    @Delete
    suspend fun delete(item: InventoryItemEntity)

    @Query("SELECT * FROM inventory_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): InventoryItemEntity?

    @Query("SELECT * FROM inventory_items WHERE isActive = 1 ORDER BY name")
    fun getAllActiveItems(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE isActive = 0 ORDER BY name")
    fun getAllArchivedItems(): Flow<List<InventoryItemEntity>>

    @Query("UPDATE inventory_items SET isActive = 0 WHERE id = :id")
    suspend fun archiveItem(id: Long)

    @Query("UPDATE inventory_items SET isActive = 1 WHERE id = :id")
    suspend fun restoreItem(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM inventory_items WHERE name = :name AND isActive = 1)")
    suspend fun existsByName(name: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM inventory_items WHERE name = :name AND id != :excludeId AND isActive = 1)")
    suspend fun existsByNameExcluding(name: String, excludeId: Long): Boolean

    @Query("UPDATE inventory_items SET quantity = quantity + :delta WHERE id = :id")
    suspend fun adjustQuantity(id: Long, delta: Int)
}
