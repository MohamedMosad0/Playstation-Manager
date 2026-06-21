package com.mohamed.playstation.data.repository.dashboard

import com.mohamed.playstation.core.utils.DateUtils
import com.mohamed.playstation.data.repository.ExpenseRepository
import com.mohamed.playstation.data.repository.InventoryRepository
import com.mohamed.playstation.data.repository.ReceiptRepository
import com.mohamed.playstation.data.repository.SessionRepository
import com.mohamed.playstation.domain.model.Expense
import com.mohamed.playstation.domain.model.Receipt
import com.mohamed.playstation.domain.model.dashboard.ChartPoint
import com.mohamed.playstation.domain.model.dashboard.DashboardData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val expenseRepository: ExpenseRepository,
    private val sessionRepository: SessionRepository,
    private val inventoryRepository: InventoryRepository
) : DashboardRepository {

    override fun getDashboardData(): Flow<DashboardData> {
        val (startOfDay, endOfDay) = DateUtils.todayRange()
        val (startOf7Days, _) = DateUtils.last7DaysRange()

        val dailyFlow = combine(
            receiptRepository.getTodayTotalRevenue(),
            expenseRepository.getTotalExpensesInRange(startOfDay, endOfDay),
            sessionRepository.getTodaySessions(),
            inventoryRepository.getAllActiveItems()
        ) { todayRevenue, todayExpenses, todaySessions, inventoryProducts ->
            DailyMetrics(todayRevenue, todayExpenses ?: 0.0, todaySessions, inventoryProducts)
        }

        val chartFlow = combine(
            receiptRepository.getReceiptsInRange(startOf7Days, endOfDay),
            expenseRepository.getExpensesInRange(startOf7Days, endOfDay)
        ) { recentReceipts, recentExpenses ->
            ChartMetrics(recentReceipts, recentExpenses)
        }

        return combine(dailyFlow, chartFlow) { daily, chart ->
            val todayExpensesList = chart.recentExpenses.filter { it.expenseDate.time >= startOfDay }

            DashboardData(
                todayRevenue = daily.todayRevenue,
                todayExpenses = daily.todayExpenses,
                sessionsToday = daily.todaySessions.size,
                netProfit = daily.todayRevenue - daily.todayExpenses,
                totalProducts = daily.inventoryProducts.size,
                lowStockProducts = daily.inventoryProducts.count { it.quantity <= 5 },
                revenueChartData = buildRevenueChartData(chart.recentReceipts),
                expenseChartData = buildExpenseChartData(chart.recentExpenses),
                recentSessions = daily.todaySessions.sortedByDescending { it.startTime }.take(5),
                recentExpenses = todayExpensesList.sortedByDescending { it.expenseDate }.take(5)
            )
        }
    }

    private fun buildRevenueChartData(receipts: List<Receipt>): List<ChartPoint> {
        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
        val last7DaysMap = linkedMapOf<String, Float>()

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0..6) {
            last7DaysMap[dateFormat.format(calendar.time)] = 0f
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        for (receipt in receipts) {
            val dateStr = dateFormat.format(receipt.createdAt)
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
        val todaySessions: List<com.mohamed.playstation.domain.model.Session>,
        val inventoryProducts: List<com.mohamed.playstation.domain.model.InventoryItem>
    )

    private data class ChartMetrics(
        val recentReceipts: List<Receipt>,
        val recentExpenses: List<Expense>
    )
}
