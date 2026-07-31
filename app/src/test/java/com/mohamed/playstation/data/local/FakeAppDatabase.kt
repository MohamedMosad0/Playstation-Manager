package com.mohamed.playstation.data.local

import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.mohamed.playstation.data.local.dao.ExpenseDao
import com.mohamed.playstation.data.local.dao.InventoryItemDao
import com.mohamed.playstation.data.local.dao.ReceiptDao
import com.mohamed.playstation.data.local.dao.SessionDao
import com.mohamed.playstation.data.local.dao.SessionProductDao
import com.mohamed.playstation.data.local.dao.StockMovementDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.mockito.kotlin.mock
import java.util.concurrent.Executor

open class FakeAppDatabase : AppDatabase() {

    init {
        val directExecutor = Executor { command -> command.run() }
        var cls: Class<*>? = RoomDatabase::class.java
        while (cls != null) {
            for (field in cls.declaredFields) {
                try {
                    field.isAccessible = true
                    if (field.name == "internalTransactionExecutor" || field.name == "internalQueryExecutor") {
                        field.set(this, directExecutor)
                    } else if (field.type == CoroutineScope::class.java) {
                        field.set(this, CoroutineScope(Dispatchers.Unconfined))
                    } else if (field.type.name.contains("CoroutineDispatcher")) {
                        field.set(this, Dispatchers.Unconfined)
                    }
                } catch (e: Throwable) {
                    // Ignore
                }
            }
            cls = cls.superclass
        }
    }

    override fun sessionDao(): SessionDao = mock()
    override fun inventoryItemDao(): InventoryItemDao = mock()
    override fun sessionProductDao(): SessionProductDao = mock()
    override fun receiptDao(): ReceiptDao = mock()
    override fun stockMovementDao(): StockMovementDao = mock()
    override fun expenseDao(): ExpenseDao = mock()

    override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper = mock()
    override fun createInvalidationTracker(): InvalidationTracker = mock()

    @Suppress("OVERRIDE_DEPRECATION")
    override fun beginTransaction() {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun endTransaction() {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun setTransactionSuccessful() {}
    override fun inTransaction(): Boolean = true
    override fun clearAllTables() {}
}
