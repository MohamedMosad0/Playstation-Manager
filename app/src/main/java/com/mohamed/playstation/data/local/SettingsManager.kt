package com.mohamed.playstation.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mohamed.playstation.core.constants.AppConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extension property لإنشاء DataStore
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = AppConstants.PREFERENCES_NAME
)

/**
 * مدير الإعدادات باستخدام DataStore
 */
@Singleton
class SettingsManager @Inject constructor(
    private val context: Context
) {

    private val dataStore = context.dataStore

    // Keys — existing (backward compatible)
    private object PreferencesKeys {
        val DARK_MODE = booleanPreferencesKey(AppConstants.KEY_DARK_MODE)
        val LANGUAGE = stringPreferencesKey(AppConstants.KEY_LANGUAGE)
        val CURRENCY = stringPreferencesKey(AppConstants.KEY_CURRENCY)
        val SINGLE_PRICE = doublePreferencesKey(AppConstants.KEY_SINGLE_PRICE)
        val MULTI_PRICE = doublePreferencesKey(AppConstants.KEY_MULTI_PRICE)
        val ALERT_TIME = intPreferencesKey(AppConstants.KEY_ALERT_TIME)
        val ALERT_SOUND = stringPreferencesKey(AppConstants.KEY_ALERT_SOUND)

        // PS4 Pricing
        val PS4_HOUR_PRICE = doublePreferencesKey(AppConstants.KEY_PS4_HOUR_PRICE)
        val PS4_HALF_HOUR_PRICE = doublePreferencesKey(AppConstants.KEY_PS4_HALF_HOUR_PRICE)
        val PS4_MULTI_EXTRA = doublePreferencesKey(AppConstants.KEY_PS4_MULTI_EXTRA)

        // PS5 Pricing
        val PS5_HOUR_PRICE = doublePreferencesKey(AppConstants.KEY_PS5_HOUR_PRICE)
        val PS5_HALF_HOUR_PRICE = doublePreferencesKey(AppConstants.KEY_PS5_HALF_HOUR_PRICE)
        val PS5_MULTI_EXTRA = doublePreferencesKey(AppConstants.KEY_PS5_MULTI_EXTRA)

        // Session Defaults
        val SESSION_MODE = stringPreferencesKey(AppConstants.KEY_SESSION_MODE)
        val DEFAULT_FIXED_MINUTES = intPreferencesKey(AppConstants.KEY_DEFAULT_FIXED_MINUTES)

        // Warning Settings
        val WARNINGS_ENABLED = booleanPreferencesKey(AppConstants.KEY_WARNINGS_ENABLED)
        val WARNING_SOUND_ENABLED = booleanPreferencesKey(AppConstants.KEY_WARNING_SOUND_ENABLED)
        val WARNING_NOTIFICATION_ENABLED = booleanPreferencesKey(AppConstants.KEY_WARNING_NOTIFICATION_ENABLED)
        val WARNING_MINUTES = intPreferencesKey(AppConstants.KEY_WARNING_MINUTES)
    }

    // ======================== Dark Mode ========================

    val darkModeFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DARK_MODE] ?: true
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = enabled
        }
    }

    // ======================== Language ========================

    val languageFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LANGUAGE] ?: AppConstants.DEFAULT_LANGUAGE
    }

    suspend fun setLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
    }

    // ======================== Currency ========================

    val currencyFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CURRENCY] ?: AppConstants.DEFAULT_CURRENCY
    }

    suspend fun setCurrency(currencyCode: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY] = currencyCode
        }
    }

    // ======================== Single Player Price (legacy) ========================

    val singlePriceFlow: Flow<Double> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SINGLE_PRICE] ?: AppConstants.DEFAULT_SINGLE_PRICE
    }

    suspend fun setSinglePrice(price: Double) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SINGLE_PRICE] = price
        }
    }

    // ======================== Multiplayer Price (legacy) ========================

    val multiPriceFlow: Flow<Double> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.MULTI_PRICE] ?: AppConstants.DEFAULT_MULTI_PRICE
    }

    suspend fun setMultiPrice(price: Double) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.MULTI_PRICE] = price
        }
    }

    // ======================== Alert Time (legacy) ========================

    val alertTimeFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ALERT_TIME] ?: AppConstants.DEFAULT_ALERT_TIME
    }

    suspend fun setAlertTime(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALERT_TIME] = minutes
        }
    }

    // ======================== Alert Sound (legacy) ========================

    val alertSoundFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ALERT_SOUND]
    }

    suspend fun setAlertSound(soundUri: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALERT_SOUND] = soundUri
        }
    }

    // ======================== PS4 Pricing ========================

    val ps4HourPriceFlow: Flow<Double> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PS4_HOUR_PRICE] ?: AppConstants.DEFAULT_PS4_HOUR_PRICE
    }

    suspend fun setPs4HourPrice(price: Double) {
        dataStore.edit { it[PreferencesKeys.PS4_HOUR_PRICE] = price }
    }

    val ps4HalfHourPriceFlow: Flow<Double> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PS4_HALF_HOUR_PRICE] ?: AppConstants.DEFAULT_PS4_HALF_HOUR_PRICE
    }

    suspend fun setPs4HalfHourPrice(price: Double) {
        dataStore.edit { it[PreferencesKeys.PS4_HALF_HOUR_PRICE] = price }
    }

    val ps4MultiExtraFlow: Flow<Double> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PS4_MULTI_EXTRA] ?: AppConstants.DEFAULT_PS4_MULTI_EXTRA
    }

    suspend fun setPs4MultiExtra(price: Double) {
        dataStore.edit { it[PreferencesKeys.PS4_MULTI_EXTRA] = price }
    }

    // ======================== PS5 Pricing ========================

    val ps5HourPriceFlow: Flow<Double> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PS5_HOUR_PRICE] ?: AppConstants.DEFAULT_PS5_HOUR_PRICE
    }

    suspend fun setPs5HourPrice(price: Double) {
        dataStore.edit { it[PreferencesKeys.PS5_HOUR_PRICE] = price }
    }

    val ps5HalfHourPriceFlow: Flow<Double> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PS5_HALF_HOUR_PRICE] ?: AppConstants.DEFAULT_PS5_HALF_HOUR_PRICE
    }

    suspend fun setPs5HalfHourPrice(price: Double) {
        dataStore.edit { it[PreferencesKeys.PS5_HALF_HOUR_PRICE] = price }
    }

    val ps5MultiExtraFlow: Flow<Double> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PS5_MULTI_EXTRA] ?: AppConstants.DEFAULT_PS5_MULTI_EXTRA
    }

    suspend fun setPs5MultiExtra(price: Double) {
        dataStore.edit { it[PreferencesKeys.PS5_MULTI_EXTRA] = price }
    }

    // ======================== Session Defaults ========================

    val sessionModeFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SESSION_MODE] ?: AppConstants.DEFAULT_SESSION_MODE
    }

    suspend fun setSessionMode(mode: String) {
        dataStore.edit { it[PreferencesKeys.SESSION_MODE] = mode }
    }

    val defaultFixedMinutesFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DEFAULT_FIXED_MINUTES] ?: AppConstants.DEFAULT_FIXED_MINUTES
    }

    suspend fun setDefaultFixedMinutes(minutes: Int) {
        dataStore.edit { it[PreferencesKeys.DEFAULT_FIXED_MINUTES] = minutes }
    }

    // ======================== Warning Settings ========================

    val warningsEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.WARNINGS_ENABLED] ?: AppConstants.DEFAULT_WARNINGS_ENABLED
    }

    suspend fun setWarningsEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.WARNINGS_ENABLED] = enabled }
    }

    val warningSoundEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.WARNING_SOUND_ENABLED] ?: AppConstants.DEFAULT_WARNING_SOUND_ENABLED
    }

    suspend fun setWarningSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.WARNING_SOUND_ENABLED] = enabled }
    }

    val warningNotificationEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.WARNING_NOTIFICATION_ENABLED] ?: AppConstants.DEFAULT_WARNING_NOTIFICATION_ENABLED
    }

    suspend fun setWarningNotificationEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.WARNING_NOTIFICATION_ENABLED] = enabled }
    }

    val warningMinutesFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.WARNING_MINUTES] ?: AppConstants.DEFAULT_WARNING_MINUTES
    }

    suspend fun setWarningMinutes(minutes: Int) {
        dataStore.edit { it[PreferencesKeys.WARNING_MINUTES] = minutes }
    }

    // ======================== Clear All ========================

    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}