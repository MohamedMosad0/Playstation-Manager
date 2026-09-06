package com.mohamed.playstation.core.pdf.model

import android.content.Context
import com.mohamed.playstation.R

/**
 * Pre-resolved localized labels for receipt PDF generation.
 * Decouples PDF drawing from direct resource/context resolution.
 */
data class ReceiptPdfLabels(
    val device: String,
    val sessionType: String,
    val duration: String,
    val products: String,
    val playCost: String,
    val productsCost: String,
    val total: String,
    val paymentMethod: String
) {
    companion object {
        fun fromContext(context: Context): ReceiptPdfLabels = ReceiptPdfLabels(
            device = context.getString(R.string.device),
            sessionType = context.getString(R.string.session_type),
            duration = context.getString(R.string.duration),
            products = context.getString(R.string.products),
            playCost = context.getString(R.string.play_cost),
            productsCost = context.getString(R.string.products_cost),
            total = context.getString(R.string.total),
            paymentMethod = context.getString(R.string.receipt_payment_method_label)
        )
    }
}
