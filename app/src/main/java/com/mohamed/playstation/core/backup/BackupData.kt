package com.mohamed.playstation.core.backup

import com.mohamed.playstation.core.utils.UiText
import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val backupVersion: Int,
    val schemaVersion: Int,
    val appVersion: String,
    val packageName: String,
    val deviceName: String,
    val exportDate: Long,
    val checksum: String,

    val inventoryItems: List<InventoryItemBackup>,
    val sessions: List<SessionBackup>,
    val receipts: List<ReceiptBackup>,
    val expenses: List<ExpenseBackup>,
    val sessionProducts: List<SessionProductBackup>,
    val stockMovements: List<StockMovementBackup>,
    val settings: SettingsBackup
)

@Serializable
data class SettingsBackup(
    val darkMode: Boolean,
    val language: String,
    val currency: String,
    val notificationsEnabled: Boolean,
    val reminderMinutes: Int,
    val ps4HourPrice: Double,
    val ps4MultiExtra: Double,
    val ps5HourPrice: Double,
    val ps5MultiExtra: Double
)

@Serializable
data class SessionBackup(
    val id: Long,
    val deviceType: String,
    val deviceNumber: Int,
    val sessionType: String,
    val sessionMode: String,
    val isMultiPlayer: Boolean,
    val fixedDurationMinutes: Int?,
    val startTime: Long,
    val endTime: Long?,
    val pausedAt: Long?,
    val totalPausedMinutes: Long,
    val status: String,
    val pricePerHour: Double,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class InventoryItemBackup(
    val id: Long,
    val name: String,
    val sellPrice: Double,
    val costPerUnit: Double,
    val quantity: Int,
    val minimumQuantity: Int,
    val isPrepared: Boolean,
    val unitLabel: String,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class SessionProductBackup(
    val id: Long,
    val sessionId: Long,
    val inventoryItemId: Long,
    val nameSnapshot: String,
    val sellPriceSnapshot: Double,
    val costSnapshot: Double,
    val unitLabelSnapshot: String,
    val isPreparedSnapshot: Boolean,
    val quantitySold: Int,
    val createdAt: Long
)

@Serializable
data class ReceiptBackup(
    val id: Long,
    val sessionId: Long,
    val receiptNumber: String,
    val deviceType: String,
    val deviceNumber: Int,
    val sessionType: String,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Long,
    val pricePerHour: Double,
    val playAmount: Double = 0.0,
    val productsAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double,
    val currencyCode: String,
    val paymentMethod: String?,
    val notes: String?,
    val createdAt: Long
)

@Serializable
data class StockMovementBackup(
    val id: Long,
    val inventoryItemId: Long,
    val quantityChange: Int,
    val movementType: String,
    val timestamp: Long
)

@Serializable
data class ExpenseBackup(
    val id: Long,
    val amount: Double,
    val category: String,
    val description: String?,
    val expenseDate: Long,
    val createdAt: Long
)

sealed interface BackupResult {
    data class Success(val restoredLanguage: String? = null) : BackupResult
    data object InvalidFile : BackupResult
    data object InvalidChecksum : BackupResult
    data object UnsupportedVersion : BackupResult
    data object WrongApplication : BackupResult
    data class PartialSuccess(val warning: UiText) : BackupResult
    data class Error(val throwable: Throwable) : BackupResult
}
