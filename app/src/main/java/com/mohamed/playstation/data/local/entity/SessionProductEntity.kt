package com.mohamed.playstation.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "session_products",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
        // Note: No ForeignKey for inventoryItemId to allow soft references.
    ],
    indices = [
        Index("sessionId"),
        Index("inventoryItemId")
    ]
)
data class SessionProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val inventoryItemId: Long, // NOT NULL, soft reference
    val nameSnapshot: String,
    val sellPriceSnapshot: Double,
    val costSnapshot: Double,
    val unitLabelSnapshot: String,
    val isPreparedSnapshot: Boolean,
    val quantitySold: Int,
    val createdAt: Date = Date()
)
