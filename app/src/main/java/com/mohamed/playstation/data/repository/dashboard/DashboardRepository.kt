package com.mohamed.playstation.data.repository.dashboard

import com.mohamed.playstation.domain.model.dashboard.DashboardData
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun getDashboardData(): Flow<DashboardData>
}
