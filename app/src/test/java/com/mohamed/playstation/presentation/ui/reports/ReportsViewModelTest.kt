package com.mohamed.playstation.presentation.ui.reports

import app.cash.turbine.test
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.data.local.SettingsManager
import com.mohamed.playstation.data.repository.ExpenseRepository
import com.mohamed.playstation.data.repository.ReceiptRepository
import com.mohamed.playstation.data.repository.SessionProductRepository
import com.mohamed.playstation.domain.model.Expense
import com.mohamed.playstation.domain.model.ExpenseCategory
import com.mohamed.playstation.domain.model.Receipt
import com.mohamed.playstation.domain.model.SessionProduct
import com.mohamed.playstation.domain.model.filter.DateRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockReceiptRepository: ReceiptRepository
    private lateinit var mockExpenseRepository: ExpenseRepository
    private lateinit var mockSessionProductRepository: SessionProductRepository
    private lateinit var mockSettingsManager: SettingsManager

    private lateinit var viewModel: ReportsViewModel

    private val sampleReceipts = listOf(
        Receipt(
            id = 1L,
            sessionId = 100L,
            receiptNumber = "0001",
            deviceType = AppConstants.DEVICE_PS4,
            deviceNumber = 1,
            sessionType = AppConstants.SESSION_TYPE_SINGLE,
            startTime = Date(),
            endTime = Date(),
            durationMinutes = 60,
            pricePerHour = 30.0,
            playAmount = 30.0,
            productsAmount = 15.0,
            discountAmount = 0.0,
            taxAmount = 0.0,
            totalAmount = 45.0,
            currencyCode = "EGP",
            createdAt = Date()
        ),
        Receipt(
            id = 2L,
            sessionId = 101L,
            receiptNumber = "0002",
            deviceType = AppConstants.DEVICE_PS5,
            deviceNumber = 2,
            sessionType = AppConstants.SESSION_TYPE_MULTI,
            startTime = Date(),
            endTime = Date(),
            durationMinutes = 120,
            pricePerHour = 50.0,
            playAmount = 100.0,
            productsAmount = 20.0,
            discountAmount = 5.0,
            taxAmount = 0.0,
            totalAmount = 115.0,
            currencyCode = "EGP",
            createdAt = Date()
        )
    )

    private val sampleExpenses = listOf(
        Expense(
            id = 1L,
            amount = 20.0,
            category = ExpenseCategory.ELECTRICITY,
            description = "Light bill",
            expenseDate = Date()
        )
    )

    private val sampleProducts = listOf(
        SessionProduct(
            id = 1L,
            sessionId = 100L,
            inventoryItemId = 10L,
            nameSnapshot = "Pepsi Can",
            sellPriceSnapshot = 15.0,
            costSnapshot = 10.0,
            unitLabelSnapshot = "Can",
            isPreparedSnapshot = false,
            quantitySold = 1
        ),
        SessionProduct(
            id = 2L,
            sessionId = 101L,
            inventoryItemId = 11L,
            nameSnapshot = "Coffee",
            sellPriceSnapshot = 20.0,
            costSnapshot = 5.0,
            unitLabelSnapshot = "Cup",
            isPreparedSnapshot = true,
            quantitySold = 1
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockReceiptRepository = mock()
        whenever(mockReceiptRepository.getAllReceipts()).thenReturn(flowOf(sampleReceipts))
        whenever(mockReceiptRepository.getReceiptsInRange(any(), any())).thenReturn(flowOf(sampleReceipts))

        mockExpenseRepository = mock()
        whenever(mockExpenseRepository.getAllExpenses()).thenReturn(flowOf(sampleExpenses))
        whenever(mockExpenseRepository.getExpensesInRange(any(), any())).thenReturn(flowOf(sampleExpenses))

        mockSessionProductRepository = mock()
        whenever(mockSessionProductRepository.getAllSessionProducts()).thenReturn(flowOf(sampleProducts))
        whenever(mockSessionProductRepository.getProductsByReceiptDateRange(any(), any())).thenReturn(flowOf(sampleProducts))

        mockSettingsManager = mock()
        whenever(mockSettingsManager.currencyFlow).thenReturn(flowOf("EGP"))

        viewModel = ReportsViewModel(
            receiptRepository = mockReceiptRepository,
            expenseRepository = mockExpenseRepository,
            sessionProductRepository = mockSessionProductRepository,
            settingsManager = mockSettingsManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_calculatesTotalsAndProfitsCorrectly() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertFalse(state.isLoading)

            assertEquals(160.0, state.totalRevenue, 0.01)
            assertEquals(130.0, state.sessionRevenue, 0.01)
            assertEquals(35.0, state.productRevenue, 0.01)
            assertEquals(15.0, state.productCost, 0.01)
            assertEquals(20.0, state.productProfit, 0.01)
            assertEquals(20.0, state.totalExpenses, 0.01)
            assertEquals(130.0, state.netProfit, 0.01)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_whenEmptyData_handlesZeroesGracefully() = runTest {
        val emptyReceiptsRepo: ReceiptRepository = mock()
        whenever(emptyReceiptsRepo.getAllReceipts()).thenReturn(flowOf(emptyList()))
        whenever(emptyReceiptsRepo.getReceiptsInRange(any(), any())).thenReturn(flowOf(emptyList()))

        val emptyExpensesRepo: ExpenseRepository = mock()
        whenever(emptyExpensesRepo.getAllExpenses()).thenReturn(flowOf(emptyList()))
        whenever(emptyExpensesRepo.getExpensesInRange(any(), any())).thenReturn(flowOf(emptyList()))

        val emptyProductsRepo: SessionProductRepository = mock()
        whenever(emptyProductsRepo.getAllSessionProducts()).thenReturn(flowOf(emptyList()))
        whenever(emptyProductsRepo.getProductsByReceiptDateRange(any(), any())).thenReturn(flowOf(emptyList()))

        val emptyVm = ReportsViewModel(
            receiptRepository = emptyReceiptsRepo,
            expenseRepository = emptyExpensesRepo,
            sessionProductRepository = emptyProductsRepo,
            settingsManager = mockSettingsManager
        )

        emptyVm.uiState.test {
            val initial = awaitItem()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(0.0, state.totalRevenue, 0.001)
            assertEquals(0.0, state.netProfit, 0.001)
            assertEquals(0, state.totalSessions)
            assertTrue(state.topProducts.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_whenExpensesExceedRevenue_handlesNegativeNetProfit() = runTest {
        val heavyExpenses = listOf(
            Expense(id = 1L, amount = 500.0, category = ExpenseCategory.MAINTENANCE, expenseDate = Date())
        )
        val heavyExpensesRepo: ExpenseRepository = mock()
        whenever(heavyExpensesRepo.getAllExpenses()).thenReturn(flowOf(heavyExpenses))
        whenever(heavyExpensesRepo.getExpensesInRange(any(), any())).thenReturn(flowOf(heavyExpenses))

        val lossVm = ReportsViewModel(
            receiptRepository = mockReceiptRepository,
            expenseRepository = heavyExpensesRepo,
            sessionProductRepository = mockSessionProductRepository,
            settingsManager = mockSettingsManager
        )

        lossVm.uiState.test {
            val initial = awaitItem()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(-350.0, state.netProfit, 0.01)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setDateFilter_updatesDateRangeFilterState() = runTest {
        viewModel.setDateFilter(DateRangeFilter.THIS_MONTH)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("EGP", state.currency)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
