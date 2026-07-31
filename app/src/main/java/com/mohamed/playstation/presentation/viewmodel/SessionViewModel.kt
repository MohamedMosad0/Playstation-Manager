package com.mohamed.playstation.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.playstation.R
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.core.notifications.SessionAlarmScheduler
import com.mohamed.playstation.core.notifications.SessionNotificationHelper
import com.mohamed.playstation.core.utils.SessionPricing
import com.mohamed.playstation.core.utils.SessionTicker
import com.mohamed.playstation.core.utils.UiText
import com.mohamed.playstation.data.local.SettingsManager
import com.mohamed.playstation.domain.model.InventoryItem
import com.mohamed.playstation.domain.model.Session
import com.mohamed.playstation.domain.model.SessionProduct
import com.mohamed.playstation.domain.model.aggregate
import com.mohamed.playstation.domain.usecase.InventoryUseCases
import com.mohamed.playstation.domain.usecase.SessionProductUseCases
import com.mohamed.playstation.domain.usecase.SessionUseCases
import com.mohamed.playstation.presentation.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionUseCases: SessionUseCases,
    private val sessionProductUseCases: SessionProductUseCases,
    private val inventoryUseCases: InventoryUseCases,
    private val settingsManager: SettingsManager,
    private val sessionNotificationHelper: SessionNotificationHelper,
    private val sessionAlarmScheduler: SessionAlarmScheduler,
    sessionTicker: SessionTicker
) : ViewModel() {

    private val tickerFlow: Flow<Long> = sessionTicker.tickerFlow

    private val _activeSessions = MutableStateFlow<UiState<Pair<List<Session>, Long>>>(UiState.Loading)
    val activeSessions: StateFlow<UiState<Pair<List<Session>, Long>>> = _activeSessions.asStateFlow()

    private val _pausedSessions = MutableStateFlow<UiState<Pair<List<Session>, Long>>>(UiState.Loading)
    val pausedSessions: StateFlow<UiState<Pair<List<Session>, Long>>> = _pausedSessions.asStateFlow()

    private val _activeSessionsCount = MutableStateFlow(0)
    val activeSessionsCount: StateFlow<Int> = _activeSessionsCount.asStateFlow()

    private val _sessionProducts = MutableStateFlow<List<SessionProduct>>(emptyList())
    val sessionProducts: StateFlow<List<SessionProduct>> = _sessionProducts.asStateFlow()

    /** Completed sessions — reuses existing getEndedSessions() flow. No new architecture. */
    val completedSessions: StateFlow<List<Session>> = sessionUseCases.getEndedSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val inventoryProducts: StateFlow<List<InventoryItem>> = inventoryUseCases.getAllActiveItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    init {
        loadActiveSessions()
        loadPausedSessions()
        loadActiveSessionsCount()
    }

    private fun loadActiveSessions() {
        viewModelScope.launch {
            combine(
                sessionUseCases.getActiveSessions(),
                tickerFlow
            ) { sessions, tick ->
                sessions to tick
            }
                .catch { e ->
                    Timber.e(e, "Error loading active sessions")
                    _activeSessions.value = UiState.Error(e.message?.let { UiText.DynamicString(it) } ?: UiText.StringResource(R.string.error_occurred))
                }
                .collect { (sessions, tick) ->
                    _activeSessions.value = if (sessions.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(sessions to tick)
                    }
                }
        }
    }

    private fun loadPausedSessions() {
        viewModelScope.launch {
            combine(
                sessionUseCases.getPausedSessions(),
                tickerFlow
            ) { sessions, tick ->
                sessions to tick
            }
                .catch { e ->
                    Timber.e(e, "Error loading paused sessions")
                    _pausedSessions.value = UiState.Error(e.message?.let { UiText.DynamicString(it) } ?: UiText.StringResource(R.string.error_occurred))
                }
                .collect { (sessions, tick) ->
                    _pausedSessions.value = if (sessions.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(sessions to tick)
                    }
                }
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
        val sessionId = sessionUseCases.startSession(
            deviceType = deviceType,
            deviceNumber = deviceNumber,
            sessionMode = sessionMode,
            isMultiPlayer = isMultiPlayer,
            fixedDurationMinutes = fixedDurationMinutes,
            pricing = pricingSettings.value
        )
        sessionAlarmScheduler.syncSession(sessionId)
        return sessionId
    }

    fun pauseSession(session: Session) {
        viewModelScope.launch {
            try {
                sessionUseCases.pauseSession(session)
                sessionAlarmScheduler.cancelSessionAlarms(session.id)
            } catch (e: Exception) {
                Timber.e(e, "Error pausing session")
            }
        }
    }

    fun resumeSession(session: Session) {
        viewModelScope.launch {
            try {
                sessionUseCases.resumeSession(session)
                sessionAlarmScheduler.syncSession(session.id, allowImmediateWarning = false)
            } catch (e: Exception) {
                Timber.e(e, "Error resuming session")
            }
        }
    }

    fun endSession(
        session: Session,
        onReceiptCreated: (Long) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val receiptId = sessionUseCases.endSessionAndCreateReceipt(
                    session = session,
                    currencyCode = currency.value,
                    pricing = pricingSettings.value
                )
                sessionAlarmScheduler.cancelSessionAlarms(session.id)
                sessionNotificationHelper.cancelSessionNotifications(session.id)

                onReceiptCreated(receiptId)
            } catch (e: Exception) {
                Timber.e(e, "Error ending session")
                onError(e)
            }
        }
    }

    fun deleteSession(session: Session) {
        viewModelScope.launch {
            try {
                sessionUseCases.deleteSession(session)
                sessionAlarmScheduler.cancelSessionAlarms(session.id)
                sessionNotificationHelper.cancelSessionNotifications(session.id)
            } catch (e: Exception) {
                Timber.e(e, "Error deleting session")
            }
        }
    }

    suspend fun getSessionById(sessionId: Long): Session? {
        return sessionUseCases.getSessionById(sessionId)
    }

    fun addInventoryProductToSession(
        sessionId: Long,
        inventoryProductId: Long,
        quantity: Int,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                sessionProductUseCases.addInventoryProductToSession(
                    sessionId = sessionId,
                    inventoryItemId = inventoryProductId,
                    quantity = quantity
                )
                onSuccess()
            } catch (e: Exception) {
                Timber.e(e, "Error adding inventory product to session")
                onError(e)
            }
        }
    }

    fun loadProductsForSession(sessionId: Long) {
        viewModelScope.launch {
            sessionProductUseCases.getProductsBySessionId(sessionId)
                .distinctUntilChanged()
                .collect { products ->
                    _sessionProducts.value = products.aggregate()
                }
        }
    }

    fun removeSessionProduct(sessionProductId: Long) {
        viewModelScope.launch {
            try {
                sessionProductUseCases.removeSessionProductAndRestoreStock(sessionProductId)
            } catch (e: Exception) {
                Timber.e(e, "Error removing session product")
            }
        }
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

}
