package com.mohamed.playstation.presentation.viewmodel.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.playstation.R
import com.mohamed.playstation.core.constants.AppConstants
import com.mohamed.playstation.core.utils.UiText
import com.mohamed.playstation.data.local.SettingsManager
import com.mohamed.playstation.domain.model.dashboard.DashboardData
import com.mohamed.playstation.domain.usecase.dashboard.GetDashboardDataUseCase
import com.mohamed.playstation.presentation.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardDataUseCase: GetDashboardDataUseCase,
    settingsManager: SettingsManager
) : ViewModel() {

    val currency: StateFlow<String> = settingsManager.currencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppConstants.DEFAULT_CURRENCY)

    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<UiState<DashboardData>> = retryTrigger.flatMapLatest {
        getDashboardDataUseCase()
            .map<DashboardData, UiState<DashboardData>> { UiState.Success(it) }
            .catch { e ->
                Timber.e(e, "Error loading dashboard data")
                emit(UiState.Error(UiText.StringResource(R.string.error_loading_data)))
            }
            .onStart { emit(UiState.Loading) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun retry() {
        retryTrigger.update { it + 1 }
    }
}
