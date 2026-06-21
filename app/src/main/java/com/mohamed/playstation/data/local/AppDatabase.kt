package com.mohamed.playstation.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mohamed.playstation.data.local.dao.SessionDao
import com.mohamed.playstation.data.local.dao.InventoryItemDao
import com.mohamed.playstation.data.local.dao.SessionProductDao
import com.mohamed.playstation.data.local.dao.ReceiptDao
import com.mohamed.playstation.data.local.dao.StockMovementDao
import com.mohamed.playstation.data.local.dao.ExpenseDao
import com.mohamed.playstation.data.local.entity.SessionEntity
import com.mohamed.playstation.data.local.entity.InventoryItemEntity
import com.mohamed.playstation.data.local.entity.SessionProductEntity
import com.mohamed.playstation.data.local.entity.ReceiptEntity
import com.mohamed.playstation.data.local.entity.StockMovementEntity
import com.mohamed.playstation.data.local.entity.ExpenseEntity
import com.mohamed.playstation.data.local.converter.DateConverter

/**
 * قاعدة البيانات الرئيسية للتطبيق
 */
@Database(
    entities = [
        SessionEntity::class,
        InventoryItemEntity::class,
        SessionProductEntity::class,
        ReceiptEntity::class,
        StockMovementEntity::class,
        ExpenseEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun inventoryItemDao(): InventoryItemDao
    abstract fun sessionProductDao(): SessionProductDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Legacy migration
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Legacy migration
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Legacy migration
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Legacy migration
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Legacy migration
            }
        }
    }
}
