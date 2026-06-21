package com.mohamed.playstation.presentation.ui.reports

import com.mohamed.playstation.domain.model.SessionProduct

data class ReportsUiState(
    val isLoading: Boolean = false,
    
    // Date Range (Start and End in milliseconds)
    val dateRangeStart: Long = 0L,
    val dateRangeEnd: Long = System.currentTimeMillis(),
    val dateRangeLabel: String = "الكل",
    
    // KPI Metrics
    val totalRevenue: Double = 0.0,
    val sessionRevenue: Double = 0.0,
    val productRevenue: Double = 0.0,
    val productCost: Double = 0.0,
    val productProfit: Double = 0.0,
    val netProfit: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalSessions: Int = 0,
    val avgSessionDurationMinutes: Long = 0L,
    
    // Charts Data
    val revenueLast7Days: List<Pair<String, Double>> = emptyList(),
    val revenueDistribution: Map<String, Double> = emptyMap(),
    val deviceDistribution: Map<String, Int> = emptyMap(),
    
    // Top Products
    val topProducts: List<TopProductItem> = emptyList(),
    
    // Profit availability
    val isProfitAvailable: Boolean = true,
    
    // Historical gap warning: some products have costPerUnit == 0
    val hasHistoricalCostGap: Boolean = false,
    
    // Currency
    val currency: String = "EGP"
)

data class TopProductItem(
    val name: String,
    val quantitySold: Int,
    val revenue: Double,
    val isPrepared: Boolean,
    val unitLabel: String
)
