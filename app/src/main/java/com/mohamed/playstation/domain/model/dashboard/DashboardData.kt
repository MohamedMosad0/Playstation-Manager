package com.mohamed.playstation.domain.model.dashboard

import com.mohamed.playstation.domain.model.Expense
import com.mohamed.playstation.domain.model.Session

data class DashboardData(
    val todayRevenue: Double,
    val todayExpenses: Double,
    val sessionsToday: Int,
    val netProfit: Double,
    val totalProducts: Int,
    val lowStockProducts: Int,
    val revenueChartData: List<ChartPoint>,
    val expenseChartData: List<ChartPoint>,
    val recentSessions: List<Session>,
    val recentExpenses: List<Expense>
)
