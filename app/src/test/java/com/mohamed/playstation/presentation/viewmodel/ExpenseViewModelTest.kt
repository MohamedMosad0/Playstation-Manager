package com.mohamed.playstation.presentation.viewmodel

import app.cash.turbine.test
import com.mohamed.playstation.core.utils.DateUtils
import com.mohamed.playstation.data.local.SettingsManager
import com.mohamed.playstation.domain.model.Expense
import com.mohamed.playstation.domain.model.ExpenseCategory
import com.mohamed.playstation.domain.model.filter.DateRangeFilter
import com.mohamed.playstation.domain.usecase.ExpenseUseCases
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
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockExpenseUseCases: ExpenseUseCases
    private lateinit var mockSettingsManager: SettingsManager

    private val rolloverTrigger = MutableSharedFlow<Long>(replay = 1)

    private val day1Timestamp = calendarAt(2026, Calendar.SEPTEMBER, 8, 11, 0, 0).timeInMillis
    private val day2Timestamp = calendarAt(2026, Calendar.SEPTEMBER, 9, 0, 0, 50).timeInMillis

    private val expenseDay1 = Expense(
        id = 1L,
        amount = 40.0,
        category = ExpenseCategory.PURCHASES,
        description = "Snacks",
        expenseDate = Date(day1Timestamp)
    )

    private val expenseDay2 = Expense(
        id = 2L,
        amount = 75.0,
        category = ExpenseCategory.MAINTENANCE,
        description = "Controller fix",
        expenseDate = Date(day2Timestamp)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        DateUtils.dayRolloverFlowOverride = rolloverTrigger
        rolloverTrigger.tryEmit(day1Timestamp)

        mockExpenseUseCases = mock()
        mockSettingsManager = mock()

        whenever(mockSettingsManager.currencyFlow).thenReturn(flowOf("EGP"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        DateUtils.dayRolloverFlowOverride = null
        DateUtils.currentTimeMillisProvider = null
    }

    @Test
    fun expenses_whenLocalMidnightRolloverOccurs_refreshesAndIncludesNewDayRecords() = runTest {
        val (start1, end1) = DateUtils.todayRange(day1Timestamp)
        val (start2, end2) = DateUtils.todayRange(day2Timestamp)

        whenever(mockExpenseUseCases.getExpensesInRange(start1, end1)).thenReturn(flowOf(listOf(expenseDay1)))
        whenever(mockExpenseUseCases.getExpensesInRange(start2, end2)).thenReturn(flowOf(listOf(expenseDay2)))

        val viewModel = ExpenseViewModel(
            expenseUseCases = mockExpenseUseCases,
            settingsManager = mockSettingsManager
        )

        viewModel.expenses.test {
            val initial = awaitItem()
            assertEquals(emptyList<Expense>(), initial)
            testDispatcher.scheduler.advanceUntilIdle()

            // Day 1
            val day1Expenses = awaitItem()
            assertEquals(1, day1Expenses.size)
            assertEquals(expenseDay1.id, day1Expenses.first().id)

            // Rollover to Day 2!
            rolloverTrigger.emit(day2Timestamp)
            testDispatcher.scheduler.advanceUntilIdle()

            // Day 2
            val day2Expenses = awaitItem()
            assertEquals(1, day2Expenses.size)
            assertEquals(expenseDay2.id, day2Expenses.first().id)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun totalAmount_whenLocalMidnightRolloverOccurs_updatesWithNewDayTotal() = runTest {
        val (start1, end1) = DateUtils.todayRange(day1Timestamp)
        val (start2, end2) = DateUtils.todayRange(day2Timestamp)

        whenever(mockExpenseUseCases.getExpensesInRange(start1, end1)).thenReturn(flowOf(listOf(expenseDay1)))
        whenever(mockExpenseUseCases.getExpensesInRange(start2, end2)).thenReturn(flowOf(listOf(expenseDay2)))

        val viewModel = ExpenseViewModel(
            expenseUseCases = mockExpenseUseCases,
            settingsManager = mockSettingsManager
        )

        viewModel.totalAmount.test {
            val initial = awaitItem()
            assertEquals(0.0, initial, 0.001)
            testDispatcher.scheduler.advanceUntilIdle()

            val day1Total = awaitItem()
            assertEquals(40.0, day1Total, 0.001)

            // Rollover to Day 2!
            rolloverTrigger.emit(day2Timestamp)
            testDispatcher.scheduler.advanceUntilIdle()

            val day2Total = awaitItem()
            assertEquals(75.0, day2Total, 0.001)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setDateFilter_preservesExistingFilterBehavior() = runTest {
        whenever(mockExpenseUseCases.getExpensesInRange(any(), any())).thenReturn(flowOf(listOf(expenseDay1, expenseDay2)))

        val viewModel = ExpenseViewModel(
            expenseUseCases = mockExpenseUseCases,
            settingsManager = mockSettingsManager
        )

        viewModel.setDateFilter(DateRangeFilter.ALL_TIME)
        assertEquals(DateRangeFilter.ALL_TIME, viewModel.dateFilterFlow.value)

        viewModel.expenses.test {
            awaitItem() // emptyList
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertEquals(2, state.size)
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
