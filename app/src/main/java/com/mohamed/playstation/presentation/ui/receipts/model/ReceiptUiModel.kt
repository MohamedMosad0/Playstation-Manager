package com.mohamed.playstation.presentation.ui.receipts.model

/**
 * Presentation model for the receipt detail screen.
 * Contains only pre-formatted display strings. No calculations. No business logic.
 * Single source of truth for UI rendering, share text, and future PDF generation.
 */
data class ReceiptUiModel(
    val receiptId: Long,
    val receiptNumber: String,
    val deviceName: String,
    val sessionType: String,
    val startTime: String,
    val endTime: String,
    val date: String,
    val duration: String,
    val hasPausedDuration: Boolean,
    val pausedDuration: String,
    val ratePerHour: String,
    val playCost: String,
    val productsCost: String,
    val totalAmount: String,
    val paymentMethod: String?,
    val products: List<ProductUiModel>,
    val hasProducts: Boolean,
    val productsListDisplay: String,
    val notes: String?,
    val plainTextShareString: String
)
