package com.mohamed.playstation.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.data.local.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel للإعدادات — Phase 1
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    // ======================== Existing StateFlows ========================

    val darkMode: StateFlow<Boolean> = settingsManager.darkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_DARK_MODE)

    val singlePrice: StateFlow<Double> = settingsManager.singlePriceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_SINGLE_PRICE)

    val multiPrice: StateFlow<Double> = settingsManager.multiPriceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_MULTI_PRICE)

    val currency: StateFlow<String> = settingsManager.currencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_CURRENCY)

    // ======================== PS4 Pricing ========================

    val ps4HourPrice: StateFlow<Double> = settingsManager.ps4HourPriceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_PS4_HOUR_PRICE)

    val ps4HalfHourPrice: StateFlow<Double> = settingsManager.ps4HalfHourPriceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_PS4_HALF_HOUR_PRICE)

    val ps4MultiExtra: StateFlow<Double> = settingsManager.ps4MultiExtraFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_PS4_MULTI_EXTRA)

    // ======================== PS5 Pricing ========================

    val ps5HourPrice: StateFlow<Double> = settingsManager.ps5HourPriceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_PS5_HOUR_PRICE)

    val ps5HalfHourPrice: StateFlow<Double> = settingsManager.ps5HalfHourPriceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_PS5_HALF_HOUR_PRICE)

    val ps5MultiExtra: StateFlow<Double> = settingsManager.ps5MultiExtraFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_PS5_MULTI_EXTRA)

    // ======================== Session Defaults ========================

    val sessionMode: StateFlow<String> = settingsManager.sessionModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_SESSION_MODE)

    val defaultFixedMinutes: StateFlow<Int> = settingsManager.defaultFixedMinutesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_FIXED_MINUTES)

    // ======================== Warning Settings ========================

    val warningsEnabled: StateFlow<Boolean> = settingsManager.warningsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_WARNINGS_ENABLED)

    val warningSoundEnabled: StateFlow<Boolean> = settingsManager.warningSoundEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_WARNING_SOUND_ENABLED)

    val warningNotificationEnabled: StateFlow<Boolean> = settingsManager.warningNotificationEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_WARNING_NOTIFICATION_ENABLED)

    val warningMinutes: StateFlow<Int> = settingsManager.warningMinutesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_WARNING_MINUTES)

    // ======================== Currency List ========================

    data class CurrencyItem(val code: String, val displayResName: String)

    val currencyList: List<CurrencyItem> = listOf(
        CurrencyItem(AppConstants.CURRENCY_EGP, "currency_egp"),
        CurrencyItem(AppConstants.CURRENCY_SAR, "currency_sar"),
        CurrencyItem(AppConstants.CURRENCY_AED, "currency_aed"),
        CurrencyItem(AppConstants.CURRENCY_USD, "currency_usd"),
        CurrencyItem(AppConstants.CURRENCY_EUR, "currency_eur"),
        CurrencyItem(AppConstants.CURRENCY_KWD, "currency_kwd"),
        CurrencyItem(AppConstants.CURRENCY_BHD, "currency_bhd"),
        CurrencyItem(AppConstants.CURRENCY_OMR, "currency_omr"),
        CurrencyItem(AppConstants.CURRENCY_QAR, "currency_qar"),
        CurrencyItem(AppConstants.CURRENCY_JOD, "currency_jod"),
        CurrencyItem(AppConstants.CURRENCY_IQD, "currency_iqd"),
        CurrencyItem(AppConstants.CURRENCY_LBP, "currency_lbp"),
        CurrencyItem(AppConstants.CURRENCY_MAD, "currency_mad"),
        CurrencyItem(AppConstants.CURRENCY_TND, "currency_tnd"),
        CurrencyItem(AppConstants.CURRENCY_DZD, "currency_dzd"),
        CurrencyItem(AppConstants.CURRENCY_LYD, "currency_lyd")
    )

    // ======================== Existing Setters ========================

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setDarkMode(enabled) }
    }

    fun setSinglePrice(price: Double) {
        viewModelScope.launch { settingsManager.setSinglePrice(price) }
    }

    fun setMultiPrice(price: Double) {
        viewModelScope.launch { settingsManager.setMultiPrice(price) }
    }

    fun setCurrency(code: String) {
        viewModelScope.launch { settingsManager.setCurrency(code) }
    }

    // ======================== PS4 Pricing Setters ========================

    fun setPs4HourPrice(price: Double) {
        viewModelScope.launch { settingsManager.setPs4HourPrice(price) }
    }

    fun setPs4HalfHourPrice(price: Double) {
        viewModelScope.launch { settingsManager.setPs4HalfHourPrice(price) }
    }

    fun setPs4MultiExtra(price: Double) {
        viewModelScope.launch { settingsManager.setPs4MultiExtra(price) }
    }

    // ======================== PS5 Pricing Setters ========================

    fun setPs5HourPrice(price: Double) {
        viewModelScope.launch { settingsManager.setPs5HourPrice(price) }
    }

    fun setPs5HalfHourPrice(price: Double) {
        viewModelScope.launch { settingsManager.setPs5HalfHourPrice(price) }
    }

    fun setPs5MultiExtra(price: Double) {
        viewModelScope.launch { settingsManager.setPs5MultiExtra(price) }
    }

    // ======================== Session Defaults Setters ========================

    fun setSessionMode(mode: String) {
        viewModelScope.launch { settingsManager.setSessionMode(mode) }
    }

    fun setDefaultFixedMinutes(minutes: Int) {
        viewModelScope.launch { settingsManager.setDefaultFixedMinutes(minutes) }
    }

    // ======================== Warning Settings Setters ========================

    fun setWarningsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setWarningsEnabled(enabled) }
    }

    fun setWarningSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setWarningSoundEnabled(enabled) }
    }

    fun setWarningNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setWarningNotificationEnabled(enabled) }
    }

    fun setWarningMinutes(minutes: Int) {
        viewModelScope.launch { settingsManager.setWarningMinutes(minutes) }
    }
}
