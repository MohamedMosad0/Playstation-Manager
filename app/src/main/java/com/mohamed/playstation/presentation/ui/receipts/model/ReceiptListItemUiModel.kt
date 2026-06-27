package com.mohamed.playstation.presentation.ui.receipts.model

/**
 * Presentation model for a receipt item in a list.
 * Contains only pre-formatted display strings.
 */
data class ReceiptListItemUiModel(
    val id: Long,
    val receiptNumber: String,
    val deviceInfo: String,
    val durationAndRange: String,
    val totalAmount: String,
    val paymentMethod: String,
    val productsSummary: String?,
    val hasProducts: Boolean
)
