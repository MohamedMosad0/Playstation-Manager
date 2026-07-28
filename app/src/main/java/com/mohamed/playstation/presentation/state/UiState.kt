package com.mohamed.playstation.presentation.state

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: com.mohamed.playstation.core.utils.UiText) : UiState<Nothing>
}
