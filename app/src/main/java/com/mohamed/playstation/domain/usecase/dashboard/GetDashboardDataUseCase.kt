package com.mohamed.playstation.domain.usecase.dashboard

import com.mohamed.playstation.data.repository.dashboard.DashboardRepository
import com.mohamed.playstation.domain.model.dashboard.DashboardData
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetDashboardDataUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository
) {
    operator fun invoke(): Flow<DashboardData> {
        return dashboardRepository.getDashboardData()
    }
}
