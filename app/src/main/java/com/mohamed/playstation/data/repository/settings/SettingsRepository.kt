package com.mohamed.playstation.data.repository.settings

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val darkModeFlow: Flow<Boolean>
    suspend fun setDarkMode(enabled: Boolean)

    val languageFlow: Flow<String>
    suspend fun setLanguage(language: String)

    val currencyFlow: Flow<String>
    suspend fun setCurrency(code: String)

    val notificationsEnabledFlow: Flow<Boolean>
    suspend fun setNotificationsEnabled(enabled: Boolean)

    val reminderMinutesFlow: Flow<Int>
    suspend fun setReminderMinutes(minutes: Int)

    // PS4 Pricing
    val ps4HourPriceFlow: Flow<Double>
    suspend fun setPs4HourPrice(price: Double)
    val ps4MultiplayerPriceFlow: Flow<Double>
    suspend fun setPs4MultiplayerPrice(price: Double)

    // PS5 Pricing
    val ps5HourPriceFlow: Flow<Double>
    suspend fun setPs5HourPrice(price: Double)
    val ps5MultiplayerPriceFlow: Flow<Double>
    suspend fun setPs5MultiplayerPrice(price: Double)

    // Session Defaults
    val sessionModeFlow: Flow<String>
    suspend fun setSessionMode(mode: String)
    val defaultFixedMinutesFlow: Flow<Int>
    suspend fun setDefaultFixedMinutes(minutes: Int)

    suspend fun resetSettings()
}
