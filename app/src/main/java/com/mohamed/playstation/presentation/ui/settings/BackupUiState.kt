package com.mohamed.playstation.presentation.ui.settings

import com.mohamed.playstation.core.utils.UiText

sealed interface BackupUiState {
    data object Idle : BackupUiState
    data object Loading : BackupUiState
    data object Success : BackupUiState
    data class RestoreSuccess(val language: String? = null) : BackupUiState
    data class Error(val message: UiText) : BackupUiState
}
