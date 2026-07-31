package com.mohamed.playstation.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * جدول الجلسات في قاعدة البيانات
 */
@Entity(
    tableName = "sessions",
    indices = [
        Index(value = ["status", "startTime"]),
        Index(value = ["deviceType", "deviceNumber", "status"])
    ]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val deviceType: String,
    val deviceNumber: Int,

    val sessionType: String,
    val sessionMode: String = "open",
    val isMultiPlayer: Boolean = false,
    val fixedDurationMinutes: Int? = null,

    val startTime: Date,
    val endTime: Date? = null,
    val pausedAt: Date? = null,
    val totalPausedMinutes: Long = 0,

    val status: String,
    val pricePerHour: Double,

    val notes: String? = null,

    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
