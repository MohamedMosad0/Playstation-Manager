package com.mohamed.playstation.presentation.viewmodel

import app.cash.turbine.test
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.core.pdf.ReceiptPdfGenerator
import com.mohamed.playstation.core.utils.DateUtils
import com.mohamed.playstation.data.local.SettingsManager
import com.mohamed.playstation.domain.model.Receipt
import com.mohamed.playstation.domain.model.filter.DateRangeFilter
import com.mohamed.playstation.domain.usecase.ReceiptUseCases
import com.mohamed.playstation.domain.usecase.SessionProductUseCases
import com.mohamed.playstation.presentation.state.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockReceiptUseCases: ReceiptUseCases
    private lateinit var mockSessionProductUseCases: SessionProductUseCases
    private lateinit var mockSettingsManager: SettingsManager
    private lateinit var mockPdfGenerator: ReceiptPdfGenerator

    private val rolloverTrigger = MutableSharedFlow<Long>(replay = 1)

    private val day1Timestamp = calendarAt(2026, Calendar.SEPTEMBER, 8, 14, 0, 0).timeInMillis
    private val day2Timestamp = calendarAt(2026, Calendar.SEPTEMBER, 9, 0, 0, 50).timeInMillis

    private val receiptDay1 = Receipt(
        id = 1L,
        sessionId = 100L,
        receiptNumber = "0001",
        deviceType = AppConstants.DEVICE_PS4,
        deviceNumber = 1,
        sessionType = AppConstants.SESSION_TYPE_SINGLE,
        startTime = Date(day1Timestamp),
        endTime = Date(day1Timestamp + 3600000),
        durationMinutes = 60,
        pricePerHour = 30.0,
        playAmount = 30.0,
        productsAmount = 0.0,
        discountAmount = 0.0,
        taxAmount = 0.0,
        totalAmount = 30.0,
        currencyCode = "EGP",
        createdAt = Date(day1Timestamp)
    )

    private val receiptDay2 = Receipt(
        id = 2L,
        sessionId = 101L,
        receiptNumber = "0002",
        deviceType = AppConstants.DEVICE_PS5,
        deviceNumber = 2,
        sessionType = AppConstants.SESSION_TYPE_SINGLE,
        startTime = Date(day2Timestamp),
        endTime = Date(day2Timestamp + 3600000),
        durationMinutes = 60,
        pricePerHour = 50.0,
        playAmount = 50.0,
        productsAmount = 0.0,
        discountAmount = 0.0,
        taxAmount = 0.0,
        totalAmount = 50.0,
        currencyCode = "EGP",
        createdAt = Date(day2Timestamp)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        DateUtils.dayRolloverFlowOverride = rolloverTrigger
        rolloverTrigger.tryEmit(day1Timestamp)

        mockReceiptUseCases = mock()
        mockSessionProductUseCases = mock()
        mockSettingsManager = mock()
        mockPdfGenerator = mock()

        whenever(mockSettingsManager.currencyFlow).thenReturn(flowOf("EGP"))
        whenever(mockSessionProductUseCases.getAllSessionProductSummaries()).thenReturn(flowOf(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        DateUtils.dayRolloverFlowOverride = null
        DateUtils.currentTimeMillisProvider = null
    }

    @Test
    fun receipts_whenLocalMidnightRolloverOccurs_refreshesAndIncludesNewDayRecords() = runTest {
        val (start1, end1) = DateUtils.todayRange(day1Timestamp)
        val (start2, end2) = DateUtils.todayRange(day2Timestamp)

        whenever(mockReceiptUseCases.getReceiptsInRange(start1, end1)).thenReturn(flowOf(listOf(receiptDay1)))
        whenever(mockReceiptUseCases.getReceiptsInRange(start2, end2)).thenReturn(flowOf(listOf(receiptDay2)))
        whenever(mockReceiptUseCases.getTotalRevenueInRange(any(), any())).thenReturn(flowOf(0.0))

        val viewModel = ReceiptViewModel(
            receiptUseCases = mockReceiptUseCases,
            sessionProductUseCases = mockSessionProductUseCases,
            settingsManager = mockSettingsManager,
            pdfGenerator = mockPdfGenerator
        )

        viewModel.receipts.test {
            // Initial item before flow runs
            val initial = awaitItem()
            assertTrue(initial is UiState.Loading)
            testDispatcher.scheduler.advanceUntilIdle()

            // Day 1 emission
            val day1State = awaitItem()
            assertTrue(day1State is UiState.Success)
            val day1List = (day1State as UiState.Success).data
            assertEquals(1, day1List.size)
            assertEquals(receiptDay1.id, day1List.first().id)

            // Rollover to Day 2!
            rolloverTrigger.emit(day2Timestamp)
            testDispatcher.scheduler.advanceUntilIdle()

            // Day 2 emission
            val day2State = awaitItem()
            assertTrue(day2State is UiState.Success)
            val day2List = (day2State as UiState.Success).data
            assertEquals(1, day2List.size)
            assertEquals(receiptDay2.id, day2List.first().id)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun periodRevenue_whenLocalMidnightRolloverOccurs_refreshesWithNewDayRevenue() = runTest {
        val (start1, end1) = DateUtils.todayRange(day1Timestamp)
        val (start2, end2) = DateUtils.todayRange(day2Timestamp)

        whenever(mockReceiptUseCases.getReceiptsInRange(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(mockReceiptUseCases.getTotalRevenueInRange(start1, end1)).thenReturn(flowOf(30.0))
        whenever(mockReceiptUseCases.getTotalRevenueInRange(start2, end2)).thenReturn(flowOf(50.0))

        val viewModel = ReceiptViewModel(
            receiptUseCases = mockReceiptUseCases,
            sessionProductUseCases = mockSessionProductUseCases,
            settingsManager = mockSettingsManager,
            pdfGenerator = mockPdfGenerator
        )

        viewModel.periodRevenue.test {
            val initial = awaitItem()
            assertEquals(0.0, initial, 0.001)
            testDispatcher.scheduler.advanceUntilIdle()

            val day1Revenue = awaitItem()
            assertEquals(30.0, day1Revenue, 0.001)

            // Rollover to Day 2!
            rolloverTrigger.emit(day2Timestamp)
            testDispatcher.scheduler.advanceUntilIdle()

            val day2Revenue = awaitItem()
            assertEquals(50.0, day2Revenue, 0.001)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setDateFilter_preservesExistingFilterBehavior() = runTest {
        whenever(mockReceiptUseCases.getReceiptsInRange(any(), any())).thenReturn(flowOf(listOf(receiptDay1, receiptDay2)))
        whenever(mockReceiptUseCases.getTotalRevenueInRange(any(), any())).thenReturn(flowOf(80.0))

        val viewModel = ReceiptViewModel(
            receiptUseCases = mockReceiptUseCases,
            sessionProductUseCases = mockSessionProductUseCases,
            settingsManager = mockSettingsManager,
            pdfGenerator = mockPdfGenerator
        )

        viewModel.setDateFilter(DateRangeFilter.ALL_TIME)
        assertEquals(DateRangeFilter.ALL_TIME, viewModel.dateFilterFlow.value)

        viewModel.receipts.test {
            awaitItem() // Loading
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertTrue(state is UiState.Success)
            assertEquals(2, (state as UiState.Success).data.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun calendarAt(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Calendar {
        return Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute, second)
        }
    }
}
