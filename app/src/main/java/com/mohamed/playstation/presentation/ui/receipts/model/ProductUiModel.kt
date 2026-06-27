package com.mohamed.playstation.presentation.ui.receipts.model

/**
 * Presentation model for a product line on a receipt.
 * Contains only pre-formatted display strings. No calculations.
 */
data class ProductUiModel(
    val name: String,
    val quantity: String,
    val unitPrice: String,
    val totalLinePrice: String,
    val displayLine: String
)
