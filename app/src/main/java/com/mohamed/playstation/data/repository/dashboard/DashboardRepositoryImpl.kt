package com.mohamed.playstation.data.repository.dashboard

import com.mohamed.playstation.core.utils.DateUtils
import com.mohamed.playstation.data.repository.ExpenseRepository
import com.mohamed.playstation.data.repository.InventoryRepository
import com.mohamed.playstation.data.repository.ReceiptRepository
import com.mohamed.playstation.data.repository.SessionRepository
import com.mohamed.playstation.data.repository.settings.SettingsRepository
import com.mohamed.playstation.domain.model.Expense
import com.mohamed.playstation.domain.model.Receipt
import com.mohamed.playstation.domain.model.Session
import com.mohamed.playstation.domain.model.dashboard.ChartPoint
import com.mohamed.playstation.domain.model.dashboard.DashboardData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val expenseRepository: ExpenseRepository,
    private val sessionRepository: SessionRepository,
    private val inventoryRepository: InventoryRepository,
    private val settingsRepository: SettingsRepository
) : DashboardRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getDashboardData(): Flow<DashboardData> {
        return DateUtils.dayRolloverFlow().flatMapLatest { now ->
            val (startOfDay, endOfDay) = DateUtils.todayRange(now)
            val (startOf7Days, _) = DateUtils.last7DaysRange(now)

            val dailyFlow = combine(
                receiptRepository.getTodayTotalRevenue().distinctUntilChanged(),
                expenseRepository.getTotalExpensesInRange(startOfDay, endOfDay).distinctUntilChanged(),
                sessionRepository.getTodaySessions().distinctUntilChanged(),
                inventoryRepository.getActiveInventoryItemsCount().distinctUntilChanged(),
                inventoryRepository.getLowStockInventoryItemsCount().distinctUntilChanged()
            ) { todayRevenue, todayExpenses, todaySessions, totalProducts, lowStockProducts ->
                DailyMetrics(todayRevenue, todayExpenses, todaySessions, totalProducts, lowStockProducts)
            }

            val chartFlow = combine(
                receiptRepository.getReceiptsInRange(startOf7Days, endOfDay).distinctUntilChanged(),
                expenseRepository.getExpensesInRange(startOf7Days, endOfDay).distinctUntilChanged()
            ) { recentReceipts, recentExpenses ->
                ChartMetrics(recentReceipts, recentExpenses)
            }

            val languageFlow = settingsRepository.languageFlow.distinctUntilChanged()

            combine(dailyFlow, chartFlow, languageFlow) { daily, chart, language ->
                val todayExpensesList = chart.recentExpenses.filter { it.expenseDate.time >= startOfDay }

                DashboardData(
                    todayRevenue = daily.todayRevenue,
                    todayExpenses = daily.todayExpenses,
                    sessionsToday = daily.todaySessions.size,
                    netProfit = daily.todayRevenue - daily.todayExpenses,
                    totalProducts = daily.totalProducts,
                    lowStockProducts = daily.lowStockProducts,
                    revenueChartData = buildRevenueChartData(chart.recentReceipts, language, now),
                    expenseChartData = buildExpenseChartData(chart.recentExpenses),
                    recentSessions = daily.todaySessions.sortedByDescending { it.startTime }.take(5),
                    recentExpenses = todayExpensesList.sortedByDescending { it.expenseDate }.take(5)
                )
            }
        }.flowOn(Dispatchers.Default)
    }

    private fun buildRevenueChartData(receipts: List<Receipt>, language: String, now: Long = DateUtils.currentTimeMillis()): List<ChartPoint> {
        val locale = if (language == "en") Locale.ENGLISH else Locale.forLanguageTag("ar")
        val formatter = SimpleDateFormat("dd/MM", locale)
        val last7DaysMap = linkedMapOf<String, Float>()

        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0..6) {
            last7DaysMap[formatter.format(calendar.time)] = 0f
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        for (receipt in receipts) {
            val dateStr = formatter.format(receipt.createdAt)
            if (last7DaysMap.containsKey(dateStr)) {
                last7DaysMap[dateStr] = last7DaysMap[dateStr]!! + receipt.totalAmount.toFloat()
            }
        }

        return last7DaysMap.map { ChartPoint(it.key, it.value) }
    }

    private fun buildExpenseChartData(expenses: List<Expense>): List<ChartPoint> {
        val map = mutableMapOf<String, Float>()
        for (expense in expenses) {
            val catName = expense.category.name 
            map[catName] = (map[catName] ?: 0f) + expense.amount.toFloat()
        }
        return map.map { ChartPoint(it.key, it.value) }.sortedByDescending { it.value }
    }

    private data class DailyMetrics(
        val todayRevenue: Double,
        val todayExpenses: Double,
        val todaySessions: List<Session>,
        val totalProducts: Int,
        val lowStockProducts: Int
    )

    private data class ChartMetrics(
        val recentReceipts: List<Receipt>,
        val recentExpenses: List<Expense>
    )
}
