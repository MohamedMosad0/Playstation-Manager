package com.mohamed.playstation.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mohamed.playstation.core.constants.AppConstants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsManagerTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var testFile: File
    private lateinit var dataStore: DataStore<Preferences>

    private val KEY_LANGUAGE = stringPreferencesKey(AppConstants.KEY_LANGUAGE)
    private val KEY_CURRENCY = stringPreferencesKey(AppConstants.KEY_CURRENCY)
    private val KEY_DARK_MODE = booleanPreferencesKey(AppConstants.KEY_DARK_MODE)
    private val KEY_PS4_HOUR_PRICE = doublePreferencesKey(AppConstants.KEY_PS4_HOUR_PRICE)

    @Before
    fun setUp() {
        testFile = tmpFolder.newFile("test_settings.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
    }

    @Test
    fun defaultValues_emittedFromDataStore() = testScope.runTest {
        val language = dataStore.data.map { it[KEY_LANGUAGE] ?: AppConstants.DEFAULT_LANGUAGE }.first()
        val currency = dataStore.data.map { it[KEY_CURRENCY] ?: AppConstants.DEFAULT_CURRENCY }.first()
        val isDark = dataStore.data.map { it[KEY_DARK_MODE] ?: true }.first()

        assertEquals("system", language)
        assertEquals("EGP", currency)
        assertTrue(isDark)
    }

    @Test
    fun saveAndReadValues_dataStoreEditsAndUpdateFlows() = testScope.runTest {
        dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = "en"
            preferences[KEY_CURRENCY] = "USD"
            preferences[KEY_DARK_MODE] = false
            preferences[KEY_PS4_HOUR_PRICE] = 45.0
        }

        val language = dataStore.data.map { it[KEY_LANGUAGE] }.first()
        val currency = dataStore.data.map { it[KEY_CURRENCY] }.first()
        val isDark = dataStore.data.map { it[KEY_DARK_MODE] }.first()
        val ps4Price = dataStore.data.map { it[KEY_PS4_HOUR_PRICE] }.first()

        assertEquals("en", language)
        assertEquals("USD", currency)
        assertFalse(isDark!!)
        assertEquals(45.0, ps4Price!!, 0.001)
    }

    @Test
    fun updateValue_overwritesExistingPreferenceInStorage() = testScope.runTest {
        dataStore.edit { preferences ->
            preferences[KEY_CURRENCY] = "SAR"
        }
        assertEquals("SAR", dataStore.data.map { it[KEY_CURRENCY] }.first())

        dataStore.edit { preferences ->
            preferences[KEY_CURRENCY] = "AED"
        }
        assertEquals("AED", dataStore.data.map { it[KEY_CURRENCY] }.first())
    }

    @Test
    fun warningNotificationSettings_shouldScheduleWarning_logic() {
        val enabledSettings = SettingsManager.WarningNotificationSettings(
            warningsEnabled = true,
            soundEnabled = true,
            notificationEnabled = true,
            warningMinutes = 5
        )
        assertTrue(enabledSettings.shouldScheduleWarning())

        val disabledSettings = SettingsManager.WarningNotificationSettings(
            warningsEnabled = false,
            soundEnabled = true,
            notificationEnabled = true,
            warningMinutes = 5
        )
        assertFalse(disabledSettings.shouldScheduleWarning())
    }
}
