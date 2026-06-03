package com.mohamed.playstation.core.di

import android.content.Context
import com.mohamed.playstation.data.local.SettingsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module لتوفير المكونات العامة
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * توفير Context
     */
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    /**
     * توفير SettingsManager
     */
    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }
}