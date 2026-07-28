package com.mohamed.playstation.core.backup

import android.content.Context
import android.os.Build
import androidx.room.withTransaction
import com.mohamed.playstation.BuildConfig
import com.mohamed.playstation.R
import com.mohamed.playstation.core.utils.UiText
import com.mohamed.playstation.data.local.AppDatabase
import com.mohamed.playstation.data.local.SettingsManager
import com.mohamed.playstation.data.local.entity.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import java.security.MessageDigest
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

@OptIn(ExperimentalSerializationApi::class)
@Singleton
class BackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val settingsManager: SettingsManager
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportBackup(uri: android.net.Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            // Read Settings
            val pricing = settingsManager.getPricingSettings()
            val currency = settingsManager.getCurrencyCode()
            val darkMode = settingsManager.darkModeFlow.first()
            val language = settingsManager.languageFlow.first()
            val warningSettings = settingsManager.getWarningNotificationSettings()

            val settingsBackup = SettingsBackup(
                darkMode = darkMode,
                language = language,
                currency = currency,
                notificationsEnabled = warningSettings.notificationEnabled,
                reminderMinutes = warningSettings.warningMinutes,
                ps4HourPrice = pricing.ps4HourPrice,
                ps4MultiExtra = pricing.ps4MultiExtra,
                ps5HourPrice = pricing.ps5HourPrice,
                ps5MultiExtra = pricing.ps5MultiExtra
            )

            // Read Database
            val sessions = database.sessionDao().getAllOnce().map {
                SessionBackup(
                    id = it.id,
                    deviceType = it.deviceType,
                    deviceNumber = it.deviceNumber,
                    sessionType = it.sessionType,
                    sessionMode = it.sessionMode,
                    isMultiPlayer = it.isMultiPlayer,
                    fixedDurationMinutes = it.fixedDurationMinutes,
                    startTime = it.startTime.time,
                    endTime = it.endTime?.time,
                    pausedAt = it.pausedAt?.time,
                    totalPausedMinutes = it.totalPausedMinutes,
                    status = it.status,
                    pricePerHour = it.pricePerHour,
                    notes = it.notes,
                    createdAt = it.createdAt.time,
                    updatedAt = it.updatedAt.time
                )
            }

            val sessionProducts = database.sessionProductDao().getAllOnce().map {
                SessionProductBackup(
                    id = it.id,
                    sessionId = it.sessionId,
                    inventoryItemId = it.inventoryItemId,
                    nameSnapshot = it.nameSnapshot,
                    sellPriceSnapshot = it.sellPriceSnapshot,
                    costSnapshot = it.costSnapshot,
                    unitLabelSnapshot = it.unitLabelSnapshot,
                    isPreparedSnapshot = it.isPreparedSnapshot,
                    quantitySold = it.quantitySold,
                    createdAt = it.createdAt.time
                )
            }

            val receipts = database.receiptDao().getAllOnce().map {
                ReceiptBackup(
                    id = it.id,
                    sessionId = it.sessionId,
                    receiptNumber = it.receiptNumber,
                    deviceType = it.deviceType,
                    deviceNumber = it.deviceNumber,
                    sessionType = it.sessionType,
                    startTime = it.startTime.time,
                    endTime = it.endTime.time,
                    durationMinutes = it.durationMinutes,
                    pricePerHour = it.pricePerHour,
                    playAmount = it.playAmount,
                    productsAmount = it.productsAmount,
                    discountAmount = it.discountAmount,
                    taxAmount = it.taxAmount,
                    totalAmount = it.totalAmount,
                    currencyCode = it.currencyCode,
                    paymentMethod = it.paymentMethod,
                    notes = it.notes,
                    createdAt = it.createdAt.time
                )
            }

            val inventoryItems = database.inventoryItemDao().getAllOnce().map {
                InventoryItemBackup(
                    id = it.id,
                    name = it.name,
                    sellPrice = it.sellPrice,
                    costPerUnit = it.costPerUnit,
                    quantity = it.quantity,
                    minimumQuantity = it.minimumQuantity,
                    isPrepared = it.isPrepared,
                    unitLabel = it.unitLabel,
                    isActive = it.isActive,
                    createdAt = it.createdAt.time,
                    updatedAt = it.updatedAt.time
                )
            }

            val stockMovements = database.stockMovementDao().getAllOnce().map {
                StockMovementBackup(
                    id = it.id,
                    inventoryItemId = it.inventoryItemId,
                    quantityChange = it.quantityChange,
                    movementType = it.movementType,
                    timestamp = it.timestamp.time
                )
            }

            val expenses = database.expenseDao().getAllOnce().map {
                ExpenseBackup(
                    id = it.id,
                    amount = it.amount,
                    category = it.category,
                    description = it.description,
                    expenseDate = it.expenseDate.time,
                    createdAt = it.createdAt.time
                )
            }

            val backupData = BackupData(
                backupVersion = 1,
                schemaVersion = 1,
                appVersion = BuildConfig.VERSION_NAME,
                packageName = BuildConfig.APPLICATION_ID,
                deviceName = Build.MODEL,
                exportDate = System.currentTimeMillis(),
                checksum = "",
                inventoryItems = inventoryItems,
                sessions = sessions,
                receipts = receipts,
                expenses = expenses,
                sessionProducts = sessionProducts,
                stockMovements = stockMovements,
                settings = settingsBackup
            )

            val canonicalString = json.encodeToString(BackupData.serializer(), backupData)
            val checksum = generateSHA256(canonicalString)
            val finalBackupData = backupData.copy(checksum = checksum)

            context.contentResolver.openOutputStream(uri)?.use { output ->
                json.encodeToStream(finalBackupData, output)
                output.flush()
            } ?: return@withContext BackupResult.Error(Exception("Could not open OutputStream"))

            BackupResult.Success()
        } catch (t: Throwable) {
            BackupResult.Error(Exception(t.message, t))
        }
    }

    suspend fun importBackup(inputStream: InputStream): BackupResult = withContext(Dispatchers.IO) {
        try {
            val backupData = try {
                json.decodeFromStream<BackupData>(inputStream)
            } catch (e: Exception) {
                return@withContext BackupResult.InvalidFile
            }

            // Validations
            if (backupData.packageName != BuildConfig.APPLICATION_ID) {
                return@withContext BackupResult.WrongApplication
            }
            if (backupData.backupVersion > 1) { // SUPPORTED_VERSION
                return@withContext BackupResult.UnsupportedVersion
            }
            if (backupData.schemaVersion > 1) {
                return@withContext BackupResult.UnsupportedVersion
            }

            val canonicalBackupData = backupData.copy(checksum = "")
            val canonicalString = json.encodeToString(BackupData.serializer(), canonicalBackupData)
            val calculatedChecksum = generateSHA256(canonicalString)

            if (calculatedChecksum != backupData.checksum) {
                return@withContext BackupResult.InvalidChecksum
            }

            // Restore Database
            database.withTransaction {
                database.clearAllTables()

                backupData.inventoryItems.forEach {
                    database.inventoryItemDao().insert(
                        InventoryItemEntity(
                            id = it.id,
                            name = it.name,
                            sellPrice = it.sellPrice,
                            costPerUnit = it.costPerUnit,
                            quantity = it.quantity,
                            minimumQuantity = it.minimumQuantity,
                            isPrepared = it.isPrepared,
                            unitLabel = it.unitLabel,
                            isActive = it.isActive,
                            createdAt = Date(it.createdAt),
                            updatedAt = Date(it.updatedAt)
                        )
                    )
                }

                backupData.sessions.forEach {
                    database.sessionDao().insert(
                        SessionEntity(
                            id = it.id,
                            deviceType = it.deviceType,
                            deviceNumber = it.deviceNumber,
                            sessionType = it.sessionType,
                            sessionMode = it.sessionMode,
                            isMultiPlayer = it.isMultiPlayer,
                            fixedDurationMinutes = it.fixedDurationMinutes,
                            startTime = Date(it.startTime),
                            endTime = it.endTime?.let { t -> Date(t) },
                            pausedAt = it.pausedAt?.let { t -> Date(t) },
                            totalPausedMinutes = it.totalPausedMinutes,
                            status = it.status,
                            pricePerHour = it.pricePerHour,
                            notes = it.notes,
                            createdAt = Date(it.createdAt),
                            updatedAt = Date(it.updatedAt)
                        )
                    )
                }

                backupData.expenses.forEach {
                    database.expenseDao().insert(
                        ExpenseEntity(
                            id = it.id,
                            amount = it.amount,
                            category = it.category,
                            description = it.description,
                            expenseDate = Date(it.expenseDate),
                            createdAt = Date(it.createdAt)
                        )
                    )
                }

                backupData.receipts.forEach {
                    database.receiptDao().insert(
                        ReceiptEntity(
                            id = it.id,
                            sessionId = it.sessionId,
                            receiptNumber = it.receiptNumber,
                            deviceType = it.deviceType,
                            deviceNumber = it.deviceNumber,
                            sessionType = it.sessionType,
                            startTime = Date(it.startTime),
                            endTime = Date(it.endTime),
                            durationMinutes = it.durationMinutes,
                            pricePerHour = it.pricePerHour,
                            playAmount = it.playAmount,
                            productsAmount = it.productsAmount,
                            discountAmount = it.discountAmount,
                            taxAmount = it.taxAmount,
                            totalAmount = it.totalAmount,
                            currencyCode = it.currencyCode,
                            paymentMethod = it.paymentMethod,
                            notes = it.notes,
                            createdAt = Date(it.createdAt)
                        )
                    )
                }

                backupData.sessionProducts.forEach {
                    database.sessionProductDao().insert(
                        SessionProductEntity(
                            id = it.id,
                            sessionId = it.sessionId,
                            inventoryItemId = it.inventoryItemId,
                            nameSnapshot = it.nameSnapshot,
                            sellPriceSnapshot = it.sellPriceSnapshot,
                            costSnapshot = it.costSnapshot,
                            unitLabelSnapshot = it.unitLabelSnapshot,
                            isPreparedSnapshot = it.isPreparedSnapshot,
                            quantitySold = it.quantitySold,
                            createdAt = Date(it.createdAt)
                        )
                    )
                }

                backupData.stockMovements.forEach {
                    database.stockMovementDao().insert(
                        StockMovementEntity(
                            id = it.id,
                            inventoryItemId = it.inventoryItemId,
                            quantityChange = it.quantityChange,
                            movementType = it.movementType,
                            timestamp = Date(it.timestamp)
                        )
                    )
                }
            }

            // Restore Settings
            try {
                settingsManager.setDarkMode(backupData.settings.darkMode)
                settingsManager.setLanguage(backupData.settings.language)
                settingsManager.setCurrency(backupData.settings.currency)
                settingsManager.setWarningNotificationEnabled(backupData.settings.notificationsEnabled)
                settingsManager.setWarningMinutes(backupData.settings.reminderMinutes)
                settingsManager.setPs4HourPrice(backupData.settings.ps4HourPrice)
                settingsManager.setPs4MultiExtra(backupData.settings.ps4MultiExtra)
                settingsManager.setPs5HourPrice(backupData.settings.ps5HourPrice)
                settingsManager.setPs5MultiExtra(backupData.settings.ps5MultiExtra)
            } catch (e: Exception) {
                return@withContext BackupResult.PartialSuccess(
                    UiText.StringResource(R.string.backup_settings_restore_warning)
                )
            }

            BackupResult.Success(
                restoredLanguage = backupData.settings.language
            )
        } catch (e: Exception) {
            BackupResult.Error(e)
        }
    }

    private fun generateSHA256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
