package com.mohamed.playstation.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mohamed.playstation.data.local.dao.SessionDao
import com.mohamed.playstation.data.local.dao.ProductDao
import com.mohamed.playstation.data.local.dao.ReceiptDao
import com.mohamed.playstation.data.local.dao.ExpenseDao
import com.mohamed.playstation.data.local.entity.SessionEntity
import com.mohamed.playstation.data.local.entity.ProductEntity
import com.mohamed.playstation.data.local.entity.ReceiptEntity
import com.mohamed.playstation.data.local.entity.ExpenseEntity
import com.mohamed.playstation.data.local.converter.DateConverter

/**
 * قاعدة البيانات الرئيسية للتطبيق
 */
@Database(
    entities = [
        SessionEntity::class,
        ProductEntity::class,
        ReceiptEntity::class,
        com.mohamed.playstation.data.local.entity.StockMovementEntity::class,
        ExpenseEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun productDao(): ProductDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun stockMovementDao(): com.mohamed.playstation.data.local.dao.StockMovementDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `products` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `price` REAL NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_products_sessionId` ON `products` (`sessionId`)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `sessionMode` TEXT NOT NULL DEFAULT 'open'"
                )
                database.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `isMultiPlayer` INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE `sessions` ADD COLUMN `fixedDurationMinutes` INTEGER"
                )
                database.execSQL(
                    "UPDATE `sessions` SET `isMultiPlayer` = 1 WHERE `sessionType` = 'multi'"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add minimumQuantity column with default 0 for existing rows
                database.execSQL(
                    "ALTER TABLE `products` ADD COLUMN `minimumQuantity` INTEGER NOT NULL DEFAULT 0"
                )

                // Create stock_movements table for tracking stock changes
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `stock_movements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `productId` INTEGER NOT NULL,
                        `quantityChange` INTEGER NOT NULL,
                        `movementType` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_stock_movements_productId` ON `stock_movements` (`productId`)"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `expenses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amount` REAL NOT NULL,
                        `category` TEXT NOT NULL,
                        `description` TEXT,
                        `expenseDate` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_expenses_expenseDate` ON `expenses` (`expenseDate`)"
                )
            }
        }
    }
}
