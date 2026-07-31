package com.mohamed.playstation.presentation.viewmodel

import app.cash.turbine.test
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.core.notifications.SessionAlarmScheduler
import com.mohamed.playstation.core.notifications.SessionNotificationHelper
import com.mohamed.playstation.core.utils.SessionTicker
import com.mohamed.playstation.data.local.SettingsManager
import com.mohamed.playstation.domain.model.Session
import com.mohamed.playstation.domain.usecase.InventoryUseCases
import com.mohamed.playstation.domain.usecase.SessionProductUseCases
import com.mohamed.playstation.domain.usecase.SessionUseCases
import com.mohamed.playstation.presentation.state.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockSessionUseCases: SessionUseCases
    private lateinit var mockSessionProductUseCases: SessionProductUseCases
    private lateinit var mockInventoryUseCases: InventoryUseCases
    private lateinit var mockSettingsManager: SettingsManager
    private lateinit var mockNotificationHelper: SessionNotificationHelper
    private lateinit var mockAlarmScheduler: SessionAlarmScheduler
    private lateinit var mockSessionTicker: SessionTicker

    private lateinit var viewModel: SessionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockSessionUseCases = mock()
        whenever(mockSessionUseCases.getActiveSessions()).thenReturn(flowOf(emptyList()))
        whenever(mockSessionUseCases.getPausedSessions()).thenReturn(flowOf(emptyList()))
        whenever(mockSessionUseCases.getEndedSessions()).thenReturn(flowOf(emptyList()))
        whenever(mockSessionUseCases.getActiveSessionsCount()).thenReturn(flowOf(0))

        mockSessionProductUseCases = mock()
        whenever(mockSessionProductUseCases.getProductsBySessionId(any())).thenReturn(flowOf(emptyList()))

        mockInventoryUseCases = mock()
        whenever(mockInventoryUseCases.getAllActiveItems()).thenReturn(flowOf(emptyList()))

        mockSettingsManager = mock()
        whenever(mockSettingsManager.singlePriceFlow).thenReturn(flowOf(AppConstants.DEFAULT_SINGLE_PRICE))
        whenever(mockSettingsManager.multiPriceFlow).thenReturn(flowOf(AppConstants.DEFAULT_MULTI_PRICE))
        whenever(mockSettingsManager.currencyFlow).thenReturn(flowOf(AppConstants.DEFAULT_CURRENCY))
        whenever(mockSettingsManager.ps4HourPriceFlow).thenReturn(flowOf(AppConstants.DEFAULT_PS4_HOUR_PRICE))
        whenever(mockSettingsManager.ps4HalfHourPriceFlow).thenReturn(flowOf(AppConstants.DEFAULT_PS4_HALF_HOUR_PRICE))
        whenever(mockSettingsManager.ps4MultiExtraFlow).thenReturn(flowOf(AppConstants.DEFAULT_PS4_MULTI_EXTRA))
        whenever(mockSettingsManager.ps5HourPriceFlow).thenReturn(flowOf(AppConstants.DEFAULT_PS5_HOUR_PRICE))
        whenever(mockSettingsManager.ps5HalfHourPriceFlow).thenReturn(flowOf(AppConstants.DEFAULT_PS5_HALF_HOUR_PRICE))
        whenever(mockSettingsManager.ps5MultiExtraFlow).thenReturn(flowOf(AppConstants.DEFAULT_PS5_MULTI_EXTRA))
        whenever(mockSettingsManager.defaultFixedMinutesFlow).thenReturn(flowOf(AppConstants.DEFAULT_FIXED_MINUTES))
        whenever(mockSettingsManager.sessionModeFlow).thenReturn(flowOf(AppConstants.DEFAULT_SESSION_MODE))

        mockNotificationHelper = mock()
        mockAlarmScheduler = mock()
        mockSessionTicker = mock()
        whenever(mockSessionTicker.tickerFlow).thenReturn(flowOf(System.currentTimeMillis()))

        viewModel = SessionViewModel(
            sessionUseCases = mockSessionUseCases,
            sessionProductUseCases = mockSessionProductUseCases,
            inventoryUseCases = mockInventoryUseCases,
            settingsManager = mockSettingsManager,
            sessionNotificationHelper = mockNotificationHelper,
            sessionAlarmScheduler = mockAlarmScheduler,
            sessionTicker = mockSessionTicker
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun activeSessions_whenEmpty_emitsEmptyUiState() = runTest {
        viewModel.activeSessions.test {
            val state = awaitItem()
            assertTrue(state is UiState.Loading || state is UiState.Empty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun startSession_invokesUseCaseAndSchedulerSync() = runTest {
        whenever(mockSessionUseCases.startSession(any(), any(), any(), any(), anyOrNull(), any()))
            .thenReturn(42L)

        val createdId = viewModel.startSession(
            deviceType = AppConstants.DEVICE_PS4,
            deviceNumber = 1,
            sessionMode = AppConstants.SESSION_MODE_OPEN,
            isMultiPlayer = false,
            fixedDurationMinutes = null
        )

        assertEquals(42L, createdId)
        verify(mockAlarmScheduler).syncSession(42L)
    }

    @Test
    fun pauseSession_invokesUseCaseAndCancelsAlarms() = runTest {
        val session = Session(
            id = 10L,
            deviceType = AppConstants.DEVICE_PS4,
            deviceNumber = 2,
            sessionType = AppConstants.SESSION_TYPE_SINGLE,
            sessionMode = AppConstants.SESSION_MODE_OPEN,
            isMultiPlayer = false,
            startTime = Date(),
            status = AppConstants.SESSION_STATUS_ACTIVE,
            pricePerHour = 30.0
        )

        viewModel.pauseSession(session)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(mockSessionUseCases).pauseSession(session)
        verify(mockAlarmScheduler).cancelSessionAlarms(10L)
    }

    @Test
    fun resumeSession_invokesUseCaseAndSyncsAlarms() = runTest {
        val session = Session(
            id = 12L,
            deviceType = AppConstants.DEVICE_PS4,
            deviceNumber = 2,
            sessionType = AppConstants.SESSION_TYPE_SINGLE,
            sessionMode = AppConstants.SESSION_MODE_OPEN,
            isMultiPlayer = false,
            startTime = Date(),
            status = AppConstants.SESSION_STATUS_PAUSED,
            pricePerHour = 30.0
        )

        viewModel.resumeSession(session)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(mockSessionUseCases).resumeSession(session)
        verify(mockAlarmScheduler).syncSession(12L, allowImmediateWarning = false)
    }
}
