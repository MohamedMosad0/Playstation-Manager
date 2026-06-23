package com.mohamed.playstation.domain.usecase.settings

import com.mohamed.playstation.data.repository.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSettingsFlowsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    val darkModeFlow: Flow<Boolean> = repository.darkModeFlow
    val languageFlow: Flow<String> = repository.languageFlow
    val currencyFlow: Flow<String> = repository.currencyFlow
    val notificationsEnabledFlow: Flow<Boolean> = repository.notificationsEnabledFlow
    val reminderMinutesFlow: Flow<Int> = repository.reminderMinutesFlow
    val ps4HourPriceFlow: Flow<Double> = repository.ps4HourPriceFlow
    
    @Deprecated("Legacy migration only")
    val ps4MultiplayerPriceFlow: Flow<Double> = repository.ps4MultiplayerPriceFlow
    
    val ps4MultiExtraFlow: Flow<Double> = repository.ps4MultiExtraFlow
    
    val ps5HourPriceFlow: Flow<Double> = repository.ps5HourPriceFlow
    
    @Deprecated("Legacy migration only")
    val ps5MultiplayerPriceFlow: Flow<Double> = repository.ps5MultiplayerPriceFlow

    val ps5MultiExtraFlow: Flow<Double> = repository.ps5MultiExtraFlow
}

class UpdateSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend fun setDarkMode(enabled: Boolean) = repository.setDarkMode(enabled)
    suspend fun setLanguage(language: String) = repository.setLanguage(language)
    suspend fun setCurrency(code: String) = repository.setCurrency(code)
    suspend fun setNotificationsEnabled(enabled: Boolean) = repository.setNotificationsEnabled(enabled)
    suspend fun setReminderMinutes(minutes: Int) = repository.setReminderMinutes(minutes)
    suspend fun setPs4HourPrice(price: Double) = repository.setPs4HourPrice(price)
    
    @Deprecated("Legacy migration only")
    suspend fun setPs4MultiplayerPrice(price: Double) = repository.setPs4MultiplayerPrice(price)
    
    suspend fun setPs4MultiExtra(price: Double) = repository.setPs4MultiExtra(price)

    suspend fun setPs5HourPrice(price: Double) = repository.setPs5HourPrice(price)
    
    @Deprecated("Legacy migration only")
    suspend fun setPs5MultiplayerPrice(price: Double) = repository.setPs5MultiplayerPrice(price)
    
    suspend fun setPs5MultiExtra(price: Double) = repository.setPs5MultiExtra(price)
    suspend fun resetSettings() = repository.resetSettings()
}
