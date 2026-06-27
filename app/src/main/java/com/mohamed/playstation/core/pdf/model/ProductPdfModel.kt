package com.mohamed.playstation.core.pdf.model

/**
 * Presentation model for a product line in the PDF receipt.
 */
data class ProductPdfModel(
    val name: String,
    val quantity: String,
    val unitPrice: String,
    val totalPrice: String
)
