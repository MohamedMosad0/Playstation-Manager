package com.mohamed.playstation.presentation.viewmodel

import app.cash.turbine.test
import com.mohamed.playstation.R
import com.mohamed.playstation.core.utils.UiText
import com.mohamed.playstation.data.local.SettingsManager
import com.mohamed.playstation.domain.model.dashboard.DashboardData
import com.mohamed.playstation.domain.usecase.dashboard.GetDashboardDataUseCase
import com.mohamed.playstation.presentation.state.UiState
import com.mohamed.playstation.presentation.viewmodel.dashboard.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockUseCase: GetDashboardDataUseCase
    private lateinit var mockSettingsManager: SettingsManager

    private val dummyDashboardData = DashboardData(
        todayRevenue = 100.0,
        todayExpenses = 20.0,
        sessionsToday = 3,
        netProfit = 80.0,
        totalProducts = 10,
        lowStockProducts = 2,
        revenueChartData = emptyList(),
        expenseChartData = emptyList(),
        recentSessions = emptyList(),
        recentExpenses = emptyList()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockUseCase = mock()
        mockSettingsManager = mock()
        whenever(mockSettingsManager.currencyFlow).thenReturn(flowOf("EGP"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_initialState_isLoading() = runTest {
        whenever(mockUseCase.invoke()).thenReturn(flowOf(dummyDashboardData))
        val viewModel = DashboardViewModel(mockUseCase, mockSettingsManager)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial is UiState.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_whenUseCaseSucceeds_emitsSuccessWithData() = runTest {
        whenever(mockUseCase.invoke()).thenReturn(flowOf(dummyDashboardData))
        val viewModel = DashboardViewModel(mockUseCase, mockSettingsManager)

        viewModel.uiState.test {
            assertEquals(UiState.Loading, awaitItem())
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertTrue(state is UiState.Success)
            assertEquals(dummyDashboardData, (state as UiState.Success).data)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_whenUseCaseFails_emitsErrorWithLocalizedMessage() = runTest {
        whenever(mockUseCase.invoke()).thenReturn(flow {
            throw RuntimeException("Database error")
        })
        val viewModel = DashboardViewModel(mockUseCase, mockSettingsManager)

        viewModel.uiState.test {
            assertEquals(UiState.Loading, awaitItem())
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertTrue(state is UiState.Error)
            val error = state as UiState.Error
            assertTrue(error.message is UiText.StringResource)
            assertEquals(R.string.error_loading_data, (error.message as UiText.StringResource).resId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun retry_afterFailure_reloadsDataAndEmitsSuccess() = runTest {
        var shouldFail = true
        whenever(mockUseCase.invoke()).thenReturn(flow {
            if (shouldFail) {
                throw RuntimeException("Temporary failure")
            } else {
                emit(dummyDashboardData)
            }
        })
        val viewModel = DashboardViewModel(mockUseCase, mockSettingsManager)

        viewModel.uiState.test {
            assertEquals(UiState.Loading, awaitItem())
            testDispatcher.scheduler.advanceUntilIdle()

            // First attempt fails
            val errorState = awaitItem()
            assertTrue(errorState is UiState.Error)

            // Retry should trigger reload and succeed
            shouldFail = false
            viewModel.retry()
            testDispatcher.scheduler.advanceUntilIdle()

            val nextState = awaitItem()
            val finalState = if (nextState is UiState.Loading) awaitItem() else nextState
            assertTrue(finalState is UiState.Success)
            assertEquals(dummyDashboardData, (finalState as UiState.Success).data)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
