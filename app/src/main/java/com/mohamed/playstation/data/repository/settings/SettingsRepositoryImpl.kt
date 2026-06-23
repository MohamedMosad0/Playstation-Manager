package com.mohamed.playstation.data.repository.settings

import com.mohamed.playstation.data.local.SettingsManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsManager: SettingsManager
) : SettingsRepository {

    override val darkModeFlow: Flow<Boolean> = settingsManager.darkModeFlow
    override suspend fun setDarkMode(enabled: Boolean) = settingsManager.setDarkMode(enabled)

    override val languageFlow: Flow<String> = settingsManager.languageFlow
    override suspend fun setLanguage(language: String) = settingsManager.setLanguage(language)

    override val currencyFlow: Flow<String> = settingsManager.currencyFlow
    override suspend fun setCurrency(code: String) = settingsManager.setCurrency(code)

    override val notificationsEnabledFlow: Flow<Boolean> = settingsManager.warningNotificationEnabledFlow
    override suspend fun setNotificationsEnabled(enabled: Boolean) = settingsManager.setWarningNotificationEnabled(enabled)

    override val reminderMinutesFlow: Flow<Int> = settingsManager.warningMinutesFlow
    override suspend fun setReminderMinutes(minutes: Int) = settingsManager.setWarningMinutes(minutes)

    // PS4 Pricing
    override val ps4HourPriceFlow: Flow<Double> = settingsManager.ps4HourPriceFlow
    override suspend fun setPs4HourPrice(price: Double) = settingsManager.setPs4HourPrice(price)
    
    @Suppress("OVERRIDE_DEPRECATION")
    override val ps4MultiplayerPriceFlow: Flow<Double> = settingsManager.ps4MultiHourPriceFlow
    @Suppress("OVERRIDE_DEPRECATION")
    override suspend fun setPs4MultiplayerPrice(price: Double) = settingsManager.setPs4MultiHourPrice(price)

    override val ps4MultiExtraFlow: Flow<Double> = settingsManager.ps4MultiExtraFlow
    override suspend fun setPs4MultiExtra(price: Double) = settingsManager.setPs4MultiExtra(price)

    // PS5 Pricing
    override val ps5HourPriceFlow: Flow<Double> = settingsManager.ps5HourPriceFlow
    override suspend fun setPs5HourPrice(price: Double) = settingsManager.setPs5HourPrice(price)
    
    @Suppress("OVERRIDE_DEPRECATION")
    override val ps5MultiplayerPriceFlow: Flow<Double> = settingsManager.ps5MultiHourPriceFlow
    @Suppress("OVERRIDE_DEPRECATION")
    override suspend fun setPs5MultiplayerPrice(price: Double) = settingsManager.setPs5MultiHourPrice(price)

    override val ps5MultiExtraFlow: Flow<Double> = settingsManager.ps5MultiExtraFlow
    override suspend fun setPs5MultiExtra(price: Double) = settingsManager.setPs5MultiExtra(price)

    // Session Defaults
    override val sessionModeFlow: Flow<String> = settingsManager.sessionModeFlow
    override suspend fun setSessionMode(mode: String) = settingsManager.setSessionMode(mode)
    override val defaultFixedMinutesFlow: Flow<Int> = settingsManager.defaultFixedMinutesFlow
    override suspend fun setDefaultFixedMinutes(minutes: Int) = settingsManager.setDefaultFixedMinutes(minutes)

    override suspend fun resetSettings() = settingsManager.clearAll()
}
