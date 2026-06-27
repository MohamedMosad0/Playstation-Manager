package com.mohamed.playstation.presentation.ui.receipts.state

import android.net.Uri
import com.mohamed.playstation.core.utils.UiText

sealed interface PdfUiState {
    data object Idle : PdfUiState
    data object Loading : PdfUiState
    data class Success(val uri: Uri) : PdfUiState
    data class Error(val message: UiText) : PdfUiState
}
