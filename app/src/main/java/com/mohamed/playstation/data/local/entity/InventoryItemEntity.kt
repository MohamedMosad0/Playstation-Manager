package com.mohamed.playstation.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "inventory_items",
    indices = [
        Index("name")
    ]
)
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sellPrice: Double,
    val costPerUnit: Double,
    val quantity: Int,
    val minimumQuantity: Int,
    val isPrepared: Boolean,
    val unitLabel: String,
    val isActive: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
