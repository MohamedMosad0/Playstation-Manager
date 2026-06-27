package com.mohamed.playstation.core.di

import android.content.Context
import androidx.room.Room
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.data.local.AppDatabase
import com.mohamed.playstation.data.local.dao.SessionDao
import com.mohamed.playstation.data.local.dao.InventoryItemDao
import com.mohamed.playstation.data.local.dao.SessionProductDao
import com.mohamed.playstation.data.local.dao.ReceiptDao
import com.mohamed.playstation.data.local.dao.StockMovementDao
import com.mohamed.playstation.data.local.dao.ExpenseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module لتوفير قاعدة البيانات والـ DAOs
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * توفير قاعدة البيانات
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        // NOTE:
        // Database schema was intentionally squashed to version 1 before production release.
        // Existing development databases created with older schema versions (2-8)
        // are unsupported and must be cleared or the app reinstalled.
        // Never reintroduce migrations for pre-release development databases.
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppConstants.DATABASE_NAME
        ).build()
    }

    /**
     * توفير SessionDao
     */
    @Provides
    @Singleton
    fun provideSessionDao(database: AppDatabase): SessionDao {
        return database.sessionDao()
    }

    /**
     * توفير InventoryItemDao
     */
    @Provides
    @Singleton
    fun provideInventoryItemDao(database: AppDatabase): InventoryItemDao {
        return database.inventoryItemDao()
    }

    /**
     * توفير SessionProductDao
     */
    @Provides
    @Singleton
    fun provideSessionProductDao(database: AppDatabase): SessionProductDao {
        return database.sessionProductDao()
    }

    /**
     * توفير ReceiptDao
     */
    @Provides
    @Singleton
    fun provideReceiptDao(database: AppDatabase): ReceiptDao {
        return database.receiptDao()
    }

    /**
     * توفير StockMovementDao
     */
    @Provides
    @Singleton
    fun provideStockMovementDao(database: AppDatabase): StockMovementDao {
        return database.stockMovementDao()
    }

    /**
     * توفير ExpenseDao
     */
    @Provides
    @Singleton
    fun provideExpenseDao(database: AppDatabase): ExpenseDao {
        return database.expenseDao()
    }
}
