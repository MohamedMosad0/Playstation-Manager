package com.mohamed.playstation.data.repository.dashboard

import app.cash.turbine.test
import com.mohamed.playstation.data.repository.ExpenseRepository
import com.mohamed.playstation.data.repository.InventoryRepository
import com.mohamed.playstation.data.repository.ReceiptRepository
import com.mohamed.playstation.data.repository.SessionRepository
import com.mohamed.playstation.data.repository.settings.SettingsRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DashboardRepositoryImplTest {

    private lateinit var mockReceiptRepository: ReceiptRepository
    private lateinit var mockExpenseRepository: ExpenseRepository
    private lateinit var mockSessionRepository: SessionRepository
    private lateinit var mockInventoryRepository: InventoryRepository
    private lateinit var mockSettingsRepository: SettingsRepository

    private lateinit var repository: DashboardRepositoryImpl

    @Before
    fun setUp() {
        mockReceiptRepository = mock()
        whenever(mockReceiptRepository.getTodayTotalRevenue()).thenReturn(flowOf(100.0))
        whenever(mockReceiptRepository.getReceiptsInRange(any(), any())).thenReturn(flowOf(emptyList()))

        mockExpenseRepository = mock()
        whenever(mockExpenseRepository.getTotalExpensesInRange(any(), any())).thenReturn(flowOf(30.0))
        whenever(mockExpenseRepository.getExpensesInRange(any(), any())).thenReturn(flowOf(emptyList()))

        mockSessionRepository = mock()
        whenever(mockSessionRepository.getTodaySessions()).thenReturn(flowOf(emptyList()))

        mockInventoryRepository = mock()
        whenever(mockInventoryRepository.getActiveInventoryItemsCount()).thenReturn(flowOf(10))
        whenever(mockInventoryRepository.getLowStockInventoryItemsCount()).thenReturn(flowOf(2))

        mockSettingsRepository = mock()
        whenever(mockSettingsRepository.languageFlow).thenReturn(flowOf("en"))

        repository = DashboardRepositoryImpl(
            receiptRepository = mockReceiptRepository,
            expenseRepository = mockExpenseRepository,
            sessionRepository = mockSessionRepository,
            inventoryRepository = mockInventoryRepository,
            settingsRepository = mockSettingsRepository
        )
    }

    @Test
    fun getDashboardData_withEnglishLanguage_formatsRevenueChartLabelsWithEnglishDigits() = runTest {
        repository.getDashboardData().test {
            val data = awaitItem()
            assertEquals(7, data.revenueChartData.size)
            // In English, label is dd/MM with ASCII digits
            val label = data.revenueChartData.first().label
            assertTrue(label.matches(Regex("\\d{2}/\\d{2}")))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getDashboardData_withArabicLanguage_formatsRevenueChartLabelsWithArabicLocale() = runTest {
        whenever(mockSettingsRepository.languageFlow).thenReturn(flowOf("ar"))

        repository.getDashboardData().test {
            val data = awaitItem()
            assertEquals(7, data.revenueChartData.size)
            val label = data.revenueChartData.first().label
            assertTrue(label.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
