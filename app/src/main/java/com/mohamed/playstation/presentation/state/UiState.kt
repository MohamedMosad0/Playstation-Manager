package com.mohamed.playstation.presentation.state

import com.mohamed.playstation.core.utils.UiText

/**
 * Unified sealed interface representing the UI state hierarchy.
 */
sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: UiText) : UiState<Nothing>
    data object Empty : UiState<Nothing>
}
