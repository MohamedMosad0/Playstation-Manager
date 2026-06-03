package com.mohamed.playstation.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.core.notifications.SessionNotificationHelper
import com.mohamed.playstation.core.utils.SessionPricing
import com.mohamed.playstation.core.utils.SessionTicker
import com.mohamed.playstation.core.utils.SessionTimer
import com.mohamed.playstation.data.local.SettingsManager
import com.mohamed.playstation.domain.model.Session
import com.mohamed.playstation.domain.model.SessionProduct
import com.mohamed.playstation.domain.usecase.ProductUseCases
import com.mohamed.playstation.domain.usecase.SessionUseCases
import com.mohamed.playstation.presentation.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    internal val sessionUseCases: SessionUseCases,
    private val productUseCases: ProductUseCases,
    private val settingsManager: SettingsManager,
    private val sessionNotificationHelper: SessionNotificationHelper,
    sessionTicker: SessionTicker
) : ViewModel() {

    private val tickerFlow: Flow<Long> = sessionTicker.tickerFlow

    private val _activeSessions = MutableStateFlow<UiState<Pair<List<Session>, Long>>>(UiState.Loading)
    val activeSessions: StateFlow<UiState<Pair<List<Session>, Long>>> = _activeSessions.asStateFlow()

    private val _pausedSessions = MutableStateFlow<UiState<Pair<List<Session>, Long>>>(UiState.Loading)
    val pausedSessions: StateFlow<UiState<Pair<List<Session>, Long>>> = _pausedSessions.asStateFlow()

    private val _activeSessionsCount = MutableStateFlow(0)
    val activeSessionsCount: StateFlow<Int> = _activeSessionsCount.asStateFlow()

    val singlePrice: StateFlow<Double> = settingsManager.singlePriceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_SINGLE_PRICE)

    val multiPrice: StateFlow<Double> = settingsManager.multiPriceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_MULTI_PRICE)

    val currency: StateFlow<String> = settingsManager.currencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_CURRENCY)

    val ps4HourPrice: StateFlow<Double> = settingsManager.ps4HourPriceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_PS4_HOUR_PRICE)

    val ps4HalfHourPrice: StateFlow<Double> = settingsManager.ps4HalfHourPriceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_PS4_HALF_HOUR_PRICE)

    val ps4MultiExtra: StateFlow<Double> = settingsManager.ps4MultiExtraFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_PS4_MULTI_EXTRA)

    val ps5HourPrice: StateFlow<Double> = settingsManager.ps5HourPriceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_PS5_HOUR_PRICE)

    val ps5HalfHourPrice: StateFlow<Double> = settingsManager.ps5HalfHourPriceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_PS5_HALF_HOUR_PRICE)

    val ps5MultiExtra: StateFlow<Double> = settingsManager.ps5MultiExtraFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_PS5_MULTI_EXTRA)

    val defaultFixedMinutes: StateFlow<Int> = settingsManager.defaultFixedMinutesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_FIXED_MINUTES)

    val defaultSessionMode: StateFlow<String> = settingsManager.sessionModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_SESSION_MODE)

    private val warningSettings: StateFlow<WarningSettings> = combine(
        settingsManager.warningsEnabledFlow,
        settingsManager.warningSoundEnabledFlow,
        settingsManager.warningNotificationEnabledFlow,
        settingsManager.warningMinutesFlow
    ) { warningsEnabled, soundEnabled, notificationEnabled, minutes ->
        WarningSettings(
            warningsEnabled = warningsEnabled,
            soundEnabled = soundEnabled,
            notificationEnabled = notificationEnabled,
            warningMinutes = minutes
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        WarningSettings(
            warningsEnabled = AppConstants.DEFAULT_WARNINGS_ENABLED,
            soundEnabled = AppConstants.DEFAULT_WARNING_SOUND_ENABLED,
            notificationEnabled = AppConstants.DEFAULT_WARNING_NOTIFICATION_ENABLED,
            warningMinutes = AppConstants.DEFAULT_WARNING_MINUTES
        )
    )

    val pricingSettings: StateFlow<SessionPricing.PricingSettings> = combine(
        singlePrice,
        multiPrice,
        ps4HourPrice,
        ps4HalfHourPrice,
        ps4MultiExtra,
        ps5HourPrice,
        ps5HalfHourPrice,
        ps5MultiExtra
    ) { values ->
        SessionPricing.PricingSettings(
            ps4HourPrice = values[2],
            ps4HalfHourPrice = values[3],
            ps4MultiExtra = values[4],
            ps5HourPrice = values[5],
            ps5HalfHourPrice = values[6],
            ps5MultiExtra = values[7],
            legacySinglePrice = values[0],
            legacyMultiPrice = values[1]
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SessionPricing.PricingSettings(
            ps4HourPrice = AppConstants.DEFAULT_PS4_HOUR_PRICE,
            ps4HalfHourPrice = AppConstants.DEFAULT_PS4_HALF_HOUR_PRICE,
            ps4MultiExtra = AppConstants.DEFAULT_PS4_MULTI_EXTRA,
            ps5HourPrice = AppConstants.DEFAULT_PS5_HOUR_PRICE,
            ps5HalfHourPrice = AppConstants.DEFAULT_PS5_HALF_HOUR_PRICE,
            ps5MultiExtra = AppConstants.DEFAULT_PS5_MULTI_EXTRA,
            legacySinglePrice = AppConstants.DEFAULT_SINGLE_PRICE,
            legacyMultiPrice = AppConstants.DEFAULT_MULTI_PRICE
        )
    )

    /** Prevents duplicate auto-end calls for the same session. */
    private val endingSessionIds = mutableSetOf<Long>()

    /** Prevents repeated warning notifications for the same session. */
    private val warnedSessionIds = mutableSetOf<Long>()

    init {
        loadActiveSessions()
        loadPausedSessions()
        loadActiveSessionsCount()
    }

    private fun loadActiveSessions() {
        viewModelScope.launch {
            combine(
                sessionUseCases.getActiveSessions(),
                tickerFlow,
                pricingSettings,
                currency,
                warningSettings
            ) { sessions, tick, pricing, currencyCode, warnings ->
                SessionMonitorState(sessions, tick, pricing, currencyCode, warnings)
            }
                .catch { e ->
                    Timber.e(e, "Error loading active sessions")
                    _activeSessions.value = UiState.Error(e.message ?: "Unknown error")
                }
                .collect { state ->
                    checkFixedSessionWarnings(state.sessions, state.tick, state.warnings)
                    checkFixedSessionsAutoEnd(
                        state.sessions,
                        state.tick,
                        state.pricing,
                        state.currencyCode,
                        state.warnings
                    )
                    _activeSessions.value = if (state.sessions.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(state.sessions to state.tick)
                    }
                }
        }
    }

    private fun loadPausedSessions() {
        viewModelScope.launch {
            combine(
                sessionUseCases.getPausedSessions(),
                tickerFlow,
                warningSettings
            ) { sessions, tick, warnings ->
                Triple(sessions, tick, warnings)
            }
                .catch { e ->
                    Timber.e(e, "Error loading paused sessions")
                    _pausedSessions.value = UiState.Error(e.message ?: "Unknown error")
                }
                .collect { (sessions, tick, warnings) ->
                    checkFixedSessionWarnings(sessions, tick, warnings)
                    _pausedSessions.value = if (sessions.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(sessions to tick)
                    }
                }
        }
    }

    /**
     * Fires once per session when remaining time enters the warning window.
     */
    private fun checkFixedSessionWarnings(
        sessions: List<Session>,
        tick: Long,
        warnings: WarningSettings
    ) {
        if (!warnings.shouldShowNotifications()) return

        val thresholdMs = warnings.warningMinutes * 60_000L

        sessions
            .asSequence()
            .filter { it.isFixed() && (it.isActive() || it.isPaused()) }
            .filter { it.id !in warnedSessionIds }
            .forEach { session ->
                val remainingMs = SessionTimer.getRemainingMs(session, tick) ?: return@forEach
                if (remainingMs in 1..thresholdMs) {
                    warnedSessionIds.add(session.id)
                    sessionNotificationHelper.showSessionEndingWarning(
                        session = session,
                        warningMinutes = warnings.warningMinutes,
                        soundEnabled = warnings.soundEnabled
                    )
                    Timber.d("Warning notification sent for session ${session.id}")
                }
            }
    }

    private fun checkFixedSessionsAutoEnd(
        sessions: List<Session>,
        tick: Long,
        pricing: SessionPricing.PricingSettings,
        currencyCode: String,
        warnings: WarningSettings
    ) {
        sessions
            .filter { SessionTimer.isFixedExpired(it, tick) && it.id !in endingSessionIds }
            .forEach { session ->
                endingSessionIds.add(session.id)
                endSession(
                    session = session,
                    pricing = pricing,
                    currencyCode = currencyCode,
                    warnings = warnings,
                    isAutoEnd = true
                )
            }
    }

    private fun loadActiveSessionsCount() {
        viewModelScope.launch {
            sessionUseCases.getActiveSessionsCount()
                .catch { e -> Timber.e(e, "Error loading active sessions count") }
                .collect { count -> _activeSessionsCount.value = count }
        }
    }

    suspend fun startSession(
        deviceType: String,
        deviceNumber: Int,
        sessionMode: String,
        isMultiPlayer: Boolean,
        fixedDurationMinutes: Int?
    ): Long {
        return sessionUseCases.startSession(
            deviceType = deviceType,
            deviceNumber = deviceNumber,
            sessionMode = sessionMode,
            isMultiPlayer = isMultiPlayer,
            fixedDurationMinutes = fixedDurationMinutes,
            pricing = pricingSettings.value
        )
    }

    fun pauseSession(session: Session) {
        viewModelScope.launch {
            try {
                sessionUseCases.pauseSession(session)
                Timber.d("Session paused: ${session.id}")
            } catch (e: Exception) {
                Timber.e(e, "Error pausing session")
            }
        }
    }

    fun resumeSession(session: Session) {
        viewModelScope.launch {
            try {
                sessionUseCases.resumeSession(session)
                Timber.d("Session resumed: ${session.id}")
            } catch (e: Exception) {
                Timber.e(e, "Error resuming session")
            }
        }
    }

    fun endSession(
        session: Session,
        onReceiptCreated: (Long) -> Unit = {}
    ) {
        endSession(
            session = session,
            pricing = pricingSettings.value,
            currencyCode = currency.value,
            warnings = warningSettings.value,
            isAutoEnd = false,
            onReceiptCreated = onReceiptCreated
        )
    }

    private fun endSession(
        session: Session,
        pricing: SessionPricing.PricingSettings,
        currencyCode: String,
        warnings: WarningSettings,
        isAutoEnd: Boolean,
        onReceiptCreated: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val receiptId = sessionUseCases.endSessionAndCreateReceipt(
                    session = session,
                    currencyCode = currencyCode,
                    pricing = pricing
                )
                endingSessionIds.remove(session.id)
                warnedSessionIds.remove(session.id)
                sessionNotificationHelper.cancelSessionNotifications(session.id)

                if (isAutoEnd && session.isFixed() && warnings.shouldShowNotifications()) {
                    sessionNotificationHelper.showSessionEnded(
                        session = session,
                        receiptId = receiptId,
                        soundEnabled = warnings.soundEnabled
                    )
                    Timber.d("End notification sent for session ${session.id}")
                }

                Timber.d("Session ended and receipt created: $receiptId")
                onReceiptCreated(receiptId)
            } catch (e: Exception) {
                endingSessionIds.remove(session.id)
                Timber.e(e, "Error ending session")
            }
        }
    }

    fun deleteSession(session: Session) {
        viewModelScope.launch {
            try {
                sessionUseCases.deleteSession(session)
                warnedSessionIds.remove(session.id)
                endingSessionIds.remove(session.id)
                sessionNotificationHelper.cancelSessionNotifications(session.id)
                Timber.d("Session deleted: ${session.id}")
            } catch (e: Exception) {
                Timber.e(e, "Error deleting session")
            }
        }
    }

    fun addProduct(
        sessionId: Long,
        name: String,
        price: Double,
        quantity: Int
    ) {
        viewModelScope.launch {
            try {
                productUseCases.addProductToSession(
                    sessionId = sessionId,
                    name = name,
                    price = price,
                    quantity = quantity
                )
                Timber.d("Product added to session: $sessionId")
            } catch (e: Exception) {
                Timber.e(e, "Error adding product to session")
            }
        }
    }

    fun getProductsForSession(sessionId: Long): Flow<List<SessionProduct>> {
        return productUseCases.getProductsBySessionId(sessionId)
    }

    fun playCostForSession(session: Session, tick: Long, pricing: SessionPricing.PricingSettings): Double {
        return if (session.isOpen()) {
            SessionPricing.openSessionCost(
                pricing,
                session.deviceType,
                session.isMultiPlayer,
                session.getElapsedMinutes(tick)
            )
        } else {
            SessionPricing.fixedPackagePrice(
                pricing,
                session.deviceType,
                session.isMultiPlayer,
                session.fixedDurationMinutes ?: AppConstants.DEFAULT_FIXED_MINUTES
            )
        }
    }

    private data class WarningSettings(
        val warningsEnabled: Boolean,
        val soundEnabled: Boolean,
        val notificationEnabled: Boolean,
        val warningMinutes: Int
    ) {
        fun shouldShowNotifications(): Boolean =
            warningsEnabled && notificationEnabled
    }

    private data class SessionMonitorState(
        val sessions: List<Session>,
        val tick: Long,
        val pricing: SessionPricing.PricingSettings,
        val currencyCode: String,
        val warnings: WarningSettings
    )
}
