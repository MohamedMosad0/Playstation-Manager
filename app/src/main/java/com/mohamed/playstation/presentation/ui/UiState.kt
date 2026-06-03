package com.mohamed.playstation.presentation.ui

/**
 * Sealed Class لإدارة حالات الـ UI
 */
sealed class UiState<out T> {

    /**
     * حالة التحميل
     */
    object Loading : UiState<Nothing>()

    /**
     * حالة النجاح
     */
    data class Success<T>(val data: T) : UiState<T>()

    /**
     * حالة الخطأ
     */
    data class Error(val message: String) : UiState<Nothing>()

    /**
     * حالة فارغة
     */
    object Empty : UiState<Nothing>()
}