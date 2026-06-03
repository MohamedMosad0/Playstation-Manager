package com.mohamed.playstation.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mohamed.playstation.data.local.dao.SessionDao
import com.mohamed.playstation.data.local.dao.ProductDao
import com.mohamed.playstation.data.local.dao.ReceiptDao
import com.mohamed.playstation.data.local.entity.SessionEntity
import com.mohamed.playstation.data.local.entity.ProductEntity
import com.mohamed.playstation.data.local.entity.ReceiptEntity
import com.mohamed.playstation.data.local.converter.DateConverter

/**
 * قاعدة البيانات الرئيسية للتطبيق
 */
@Database(
    entities = [
        SessionEntity::class,
        ProductEntity::class,
        ReceiptEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun productDao(): ProductDao
    abstract fun receiptDao(): ReceiptDao

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
    }
}
