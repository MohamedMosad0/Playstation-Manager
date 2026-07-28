package com.mohamed.playstation.core.pdf.model

/**
 * Presentation model for the PDF receipt.
 * Contains only pre-formatted strings ready for drawing.
 */
data class ReceiptPdfModel(
    val receiptNumber: String,
    val date: String,
    val deviceName: String,
    val sessionType: String,
    val duration: String,
    val products: List<ProductPdfModel>,
    val playCost: String,
    val productsCost: String,
    val totalAmount: String,
    val paymentMethod: String,
    val currencyCode: String,
    val appName: String,
    val footerMessage: String
)
