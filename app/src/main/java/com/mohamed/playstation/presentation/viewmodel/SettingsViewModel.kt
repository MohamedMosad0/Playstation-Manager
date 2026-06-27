package com.mohamed.playstation.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.domain.usecase.settings.GetSettingsFlowsUseCase
import com.mohamed.playstation.domain.usecase.settings.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.first

import com.mohamed.playstation.core.backup.BackupManager
import com.mohamed.playstation.presentation.ui.settings.BackupUiState
import android.net.Uri
import com.mohamed.playstation.R
import com.mohamed.playstation.core.utils.UiText
import com.mohamed.playstation.core.backup.BackupResult
import com.mohamed.playstation.domain.usecase.SessionUseCases
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSettingsFlowsUseCase: GetSettingsFlowsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val backupManager: BackupManager,
    private val sessionUseCases: SessionUseCases
) : ViewModel() {

    val darkMode: StateFlow<Boolean> = getSettingsFlowsUseCase.darkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_DARK_MODE)

    val language: StateFlow<String> = getSettingsFlowsUseCase.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_LANGUAGE)

    val currency: StateFlow<String> = getSettingsFlowsUseCase.currencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_CURRENCY)

    val notificationsEnabled: StateFlow<Boolean> = getSettingsFlowsUseCase.notificationsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_WARNING_NOTIFICATION_ENABLED)

    val reminderMinutes: StateFlow<Int> = getSettingsFlowsUseCase.reminderMinutesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_WARNING_MINUTES)

    val ps4HourPrice: StateFlow<Double> = getSettingsFlowsUseCase.ps4HourPriceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_PS4_HOUR_PRICE)

    val ps4MultiExtra: StateFlow<Double> = getSettingsFlowsUseCase.ps4MultiExtraFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_PS4_MULTI_EXTRA)

    val ps5HourPrice: StateFlow<Double> = getSettingsFlowsUseCase.ps5HourPriceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_PS5_HOUR_PRICE)

    val ps5MultiExtra: StateFlow<Double> = getSettingsFlowsUseCase.ps5MultiExtraFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_PS5_MULTI_EXTRA)

    init {
        migrateLegacyPricing()
    }

    private fun migrateLegacyPricing() {
        viewModelScope.launch {
            // PS4 Migration
            combine(
                getSettingsFlowsUseCase.ps4HourPriceFlow,
                getSettingsFlowsUseCase.ps4MultiExtraFlow,
                getSettingsFlowsUseCase.ps4MultiplayerPriceFlow
            ) { hour, extra, legacyMultiHour ->
                Triple(hour, extra, legacyMultiHour)
            }.first().let { (hour, extra, legacyMultiHour) ->
                if (extra == AppConstants.DEFAULT_PS4_MULTI_EXTRA && legacyMultiHour > hour) {
                    val migrated = (legacyMultiHour - hour).coerceAtLeast(0.0)
                    updateSettingsUseCase.setPs4MultiExtra(migrated)
                }
            }

            // PS5 Migration
            combine(
                getSettingsFlowsUseCase.ps5HourPriceFlow,
                getSettingsFlowsUseCase.ps5MultiExtraFlow,
                getSettingsFlowsUseCase.ps5MultiplayerPriceFlow
            ) { hour, extra, legacyMultiHour ->
                Triple(hour, extra, legacyMultiHour)
            }.first().let { (hour, extra, legacyMultiHour) ->
                if (extra == AppConstants.DEFAULT_PS5_MULTI_EXTRA && legacyMultiHour > hour) {
                    val migrated = (legacyMultiHour - hour).coerceAtLeast(0.0)
                    updateSettingsUseCase.setPs5MultiExtra(migrated)
                }
            }
        }
    }

    // Validation State
    data class ValidationErrors(
        val ps4HourError: Int? = null,
        val ps4MultiError: Int? = null,
        val ps5HourError: Int? = null,
        val ps5MultiError: Int? = null,
        val reminderError: Int? = null
    )

    private val _validationErrors = MutableStateFlow(ValidationErrors())
    val validationErrors: StateFlow<ValidationErrors> = _validationErrors.asStateFlow()

    private val _backupUiState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val backupUiState: StateFlow<BackupUiState> = _backupUiState.asStateFlow()

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

    data class LanguageItem(val code: String, val nameResId: Int)
    val languageList = listOf(
        LanguageItem("system", com.mohamed.playstation.R.string.language_system),
        LanguageItem("ar", com.mohamed.playstation.R.string.language_arabic),
        LanguageItem("en", com.mohamed.playstation.R.string.language_english)
    )

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { updateSettingsUseCase.setDarkMode(enabled) }
    }

    fun setLanguage(languageCode: String) {
        viewModelScope.launch { updateSettingsUseCase.setLanguage(languageCode) }
    }

    fun setCurrency(code: String) {
        viewModelScope.launch { updateSettingsUseCase.setCurrency(code) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { updateSettingsUseCase.setNotificationsEnabled(enabled) }
    }

    fun setReminderMinutes(minutes: Int) {
        if (minutes <= 0) {
            _validationErrors.update { it.copy(reminderError = com.mohamed.playstation.R.string.error_invalid_price) }
        } else {
            _validationErrors.update { it.copy(reminderError = null) }
            viewModelScope.launch { updateSettingsUseCase.setReminderMinutes(minutes) }
        }
    }

    fun setPs4HourPrice(price: Double?) {
        validateAndSavePrice(price, allowZero = false, errorSetter = { err -> _validationErrors.update { it.copy(ps4HourError = err) } }) {
            viewModelScope.launch { updateSettingsUseCase.setPs4HourPrice(it) }
        }
    }

    fun setPs4MultiExtra(price: Double?) {
        validateAndSavePrice(price, allowZero = true, errorSetter = { err -> _validationErrors.update { it.copy(ps4MultiError = err) } }) {
            viewModelScope.launch { updateSettingsUseCase.setPs4MultiExtra(it) }
        }
    }

    fun setPs5HourPrice(price: Double?) {
        validateAndSavePrice(price, allowZero = false, errorSetter = { err -> _validationErrors.update { it.copy(ps5HourError = err) } }) {
            viewModelScope.launch { updateSettingsUseCase.setPs5HourPrice(it) }
        }
    }

    fun setPs5MultiExtra(price: Double?) {
        validateAndSavePrice(price, allowZero = true, errorSetter = { err -> _validationErrors.update { it.copy(ps5MultiError = err) } }) {
            viewModelScope.launch { updateSettingsUseCase.setPs5MultiExtra(it) }
        }
    }

    private fun validateAndSavePrice(price: Double?, allowZero: Boolean, errorSetter: (Int?) -> Unit, onSave: (Double) -> Unit) {
        when {
            price == null -> {
                errorSetter(com.mohamed.playstation.R.string.error_empty_price)
            }
            (!allowZero && price <= 0) || (allowZero && price < 0) -> {
                errorSetter(com.mohamed.playstation.R.string.error_invalid_price)
            }
            else -> {
                errorSetter(null)
                onSave(price)
            }
        }
    }

    fun resetSettings() {
        viewModelScope.launch { updateSettingsUseCase.resetSettings() }
    }

    suspend fun hasActiveSessions(): Boolean {
        return sessionUseCases.getActiveSessionsCount().first() > 0
    }

    fun exportBackup(uri: Uri) {
        _backupUiState.value = BackupUiState.Loading
        viewModelScope.launch {
            when (val result = backupManager.exportBackup(uri)) {
                is BackupResult.Success -> {
                    _backupUiState.value = BackupUiState.Success
                }
                is BackupResult.Error -> {
                    _backupUiState.value = BackupUiState.Error(UiText.StringResource(R.string.backup_failed))
                }
                else -> {
                    _backupUiState.value = BackupUiState.Error(UiText.StringResource(R.string.backup_failed))
                }
            }
        }
    }

    fun importBackup(uri: Uri, inputStream: java.io.InputStream?) {
        if (inputStream == null) {
            _backupUiState.value = BackupUiState.Error(UiText.StringResource(R.string.restore_failed))
            return
        }
        _backupUiState.value = BackupUiState.Loading
        viewModelScope.launch {
            when (val result = backupManager.importBackup(inputStream)) {
                is BackupResult.Success -> {
                    _backupUiState.value = BackupUiState.RestoreSuccess(
                        language = result.restoredLanguage
                    )
                }
                is BackupResult.PartialSuccess -> {
                    _backupUiState.value = BackupUiState.Error(result.warning) // Treat partial as error/warning message
                }
                is BackupResult.InvalidFile -> {
                    _backupUiState.value = BackupUiState.Error(UiText.StringResource(R.string.invalid_backup_file))
                }
                is BackupResult.InvalidChecksum -> {
                    _backupUiState.value = BackupUiState.Error(UiText.StringResource(R.string.checksum_invalid))
                }
                is BackupResult.UnsupportedVersion -> {
                    _backupUiState.value = BackupUiState.Error(UiText.StringResource(R.string.unsupported_backup_version))
                }
                is BackupResult.WrongApplication -> {
                    _backupUiState.value = BackupUiState.Error(UiText.StringResource(R.string.restore_failed))
                }
                is BackupResult.Error -> {
                    _backupUiState.value = BackupUiState.Error(UiText.StringResource(R.string.restore_failed))
                }
            }
        }
    }

    fun resetBackupUiState() {
        _backupUiState.value = BackupUiState.Idle
    }
}
