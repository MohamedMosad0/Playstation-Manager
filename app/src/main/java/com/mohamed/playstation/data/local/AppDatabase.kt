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
    version = 1,
    exportSchema = true
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun inventoryItemDao(): InventoryItemDao
    abstract fun sessionProductDao(): SessionProductDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun expenseDao(): ExpenseDao

}
