package com.mohamed.playstation.presentation.ui.receipts.mapper

import android.content.Context
import com.mohamed.playstation.R
import com.mohamed.playstation.core.utils.CurrencyUtils
import com.mohamed.playstation.core.utils.SessionPricing
import com.mohamed.playstation.domain.model.Receipt
import com.mohamed.playstation.domain.model.Session
import com.mohamed.playstation.domain.model.SessionProduct
import com.mohamed.playstation.domain.model.SessionProductSummary
import com.mohamed.playstation.presentation.ui.receipts.model.ProductUiModel
import com.mohamed.playstation.presentation.ui.receipts.model.ReceiptListItemUiModel
import com.mohamed.playstation.presentation.ui.receipts.model.ReceiptUiModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single source of truth for mapping domain Receipt data into display-ready UI models.
 * Consolidates all formatting, calculations, and string construction.
 */
object ReceiptDisplayMapper {

    /**
     * Maps a receipt to a full detail UI model.
     */
    fun map(
        context: Context,
        receipt: Receipt,
        session: Session?,
        products: List<SessionProduct>
    ): ReceiptUiModel {
        val currencySymbol = CurrencyUtils.getCurrencySymbol(context, receipt.currencyCode)

        // --- Snapshot Values ---
        val playAmount = receipt.playAmount
        val productsAmount = receipt.productsAmount

        // --- Formatting ---
        val receiptNumber = context.getString(R.string.receipt_number, receipt.receiptNumber)
        val deviceName = formatFullDeviceName(receipt.deviceType, receipt.deviceNumber)
        val sessionType = formatSessionType(context, receipt.sessionType)
        val startTime = formatTime(receipt.startTime)
        val endTime = formatTime(receipt.endTime)
        val date = formatDate(receipt.createdAt)
        val duration = formatDuration(receipt.durationMinutes)

        val pausedMinutes = session?.totalPausedMinutes ?: 0L
        val hasPausedDuration = pausedMinutes > 0
        val pausedDuration = if (hasPausedDuration) formatDuration(pausedMinutes) else ""

        val ratePerHour = context.getString(
            R.string.receipt_rate_per_hour,
            String.format(Locale.getDefault(), "%.2f", receipt.pricePerHour),
            currencySymbol
        )
        val playCost = CurrencyUtils.formatAmount(context, playAmount, receipt.currencyCode)
        val productsCost = CurrencyUtils.formatAmount(context, productsAmount, receipt.currencyCode)
        val totalAmount = "${String.format(Locale.getDefault(), "%.2f", receipt.totalAmount)} $currencySymbol"

        // --- Product display lines ---
        val productUiModels = products.map { product ->
            val unitPrice = CurrencyUtils.formatAmount(context, product.sellPriceSnapshot, receipt.currencyCode)
            val lineTotal = CurrencyUtils.formatAmount(context, product.getLineTotal(), receipt.currencyCode)
            val displayLine = context.getString(
                R.string.receipt_item_format,
                product.nameSnapshot,
                unitPrice,
                product.quantitySold,
                lineTotal
            )
            ProductUiModel(
                name = product.nameSnapshot,
                quantity = product.quantitySold.toString(),
                unitPrice = unitPrice,
                totalLinePrice = lineTotal,
                displayLine = displayLine
            )
        }

        val productsListDisplay = productUiModels.joinToString("\n") { it.displayLine }

        // --- Payment ---
        val paymentMethod = receipt.paymentMethod

        // --- Share text ---
        val plainTextShareString = buildShareText(
            context = context,
            receipt = receipt,
            session = session,
            products = products,
            currencySymbol = currencySymbol,
            playAmount = playAmount,
            productsAmount = productsAmount
        )

        return ReceiptUiModel(
            receiptId = receipt.id,
            receiptNumber = receiptNumber,
            deviceName = deviceName,
            sessionType = sessionType,
            startTime = startTime,
            endTime = endTime,
            date = date,
            duration = duration,
            hasPausedDuration = hasPausedDuration,
            pausedDuration = pausedDuration,
            ratePerHour = ratePerHour,
            playCost = playCost,
            productsCost = productsCost,
            totalAmount = totalAmount,
            paymentMethod = paymentMethod,
            products = productUiModels,
            hasProducts = products.isNotEmpty(),
            productsListDisplay = productsListDisplay,
            notes = receipt.notes,
            plainTextShareString = plainTextShareString
        )
    }

    /**
     * Maps a receipt to a list item UI model for the adapter.
     */
    fun mapToListItem(
        context: Context,
        receipt: Receipt,
        summary: SessionProductSummary?
    ): ReceiptListItemUiModel {
        val currencySymbol = CurrencyUtils.getCurrencySymbol(context, receipt.currencyCode)
        
        val sessionTypeText = formatSessionType(context, receipt.sessionType)
        val deviceInfo = "${formatFullDeviceName(receipt.deviceType, receipt.deviceNumber)} • $sessionTypeText"
        
        val timeRange = "${formatTime(receipt.startTime)} - ${formatTime(receipt.endTime)}"
        val durationAndRange = "${formatDuration(receipt.durationMinutes)} • $timeRange"
        
        val totalAmount = com.mohamed.playstation.core.utils.CurrencyFormatter.formatCurrency(receipt.totalAmount, currencySymbol)
        
        val paymentMethodLabel = when (receipt.paymentMethod) {
            "cash" -> context.getString(R.string.cash)
            "card" -> context.getString(R.string.card)
            else -> receipt.paymentMethod ?: ""
        }
        
        val productsSummary = if (summary != null && summary.totalQuantity > 0) {
            context.getString(
                R.string.receipt_products_summary,
                summary.totalQuantity,
                CurrencyUtils.formatAmount(context, summary.totalAmount, receipt.currencyCode)
            )
        } else null

        return ReceiptListItemUiModel(
            id = receipt.id,
            receiptNumber = context.getString(R.string.receipt_number, receipt.receiptNumber),
            deviceInfo = deviceInfo,
            durationAndRange = durationAndRange,
            totalAmount = totalAmount,
            paymentMethod = paymentMethodLabel,
            productsSummary = productsSummary,
            hasProducts = summary != null && summary.totalQuantity > 0
        )
    }

    // --- Internal Formatting Helpers (Moved from domain/Receipt.kt) ---

    private fun formatTime(date: Date): String {
        val format = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        return format.format(date)
    }

    private fun formatDate(date: Date): String {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return format.format(date)
    }

    private fun formatDuration(durationMinutes: Long): String {
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    private fun formatFullDeviceName(deviceType: String, deviceNumber: Int): String {
        return "$deviceType #$deviceNumber"
    }

    private fun formatSessionType(context: Context, sessionType: String): String {
        return when (sessionType) {
            "single" -> context.getString(R.string.single_player)
            "multi" -> context.getString(R.string.multiplayer)
            else -> sessionType
        }
    }

    private fun buildShareText(
        context: Context,
        receipt: Receipt,
        session: Session?,
        products: List<SessionProduct>,
        currencySymbol: String,
        playAmount: Double,
        productsAmount: Double
    ): String {
        return buildString {
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("🎮 ${context.getString(R.string.app_name)}")
            appendLine(context.getString(R.string.receipt_number, receipt.receiptNumber))
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            appendLine("${context.getString(R.string.device)}: ${formatFullDeviceName(receipt.deviceType, receipt.deviceNumber)}")
            appendLine("${context.getString(R.string.receipt_type_label)} ${formatSessionType(context, receipt.sessionType)}")
            appendLine("${context.getString(R.string.started)}: ${formatTime(receipt.startTime)}")
            appendLine("${context.getString(R.string.ended)}: ${formatTime(receipt.endTime)}")
            appendLine("${context.getString(R.string.duration)}: ${formatDuration(receipt.durationMinutes)}")
            
            if ((session?.totalPausedMinutes ?: 0L) > 0) {
                appendLine("${context.getString(R.string.pause_duration)}: ${formatDuration(session?.totalPausedMinutes ?: 0L)}")
            }
            
            appendLine()
            appendLine(
                "${context.getString(R.string.rate)}: ${
                    context.getString(
                        R.string.receipt_rate_per_hour,
                        String.format(Locale.getDefault(), "%.2f", receipt.pricePerHour),
                        currencySymbol
                    )
                }"
            )
            appendLine("${context.getString(R.string.play_cost)}: ${CurrencyUtils.formatAmount(context, playAmount, receipt.currencyCode)}")
            appendLine("${context.getString(R.string.products_cost)}: ${CurrencyUtils.formatAmount(context, productsAmount, receipt.currencyCode)}")
            
            if (products.isNotEmpty()) {
                appendLine(context.getString(R.string.products))
                products.forEach { product ->
                    val unitPrice = CurrencyUtils.formatAmount(context, product.sellPriceSnapshot, receipt.currencyCode)
                    val lineTotal = CurrencyUtils.formatAmount(context, product.getLineTotal(), receipt.currencyCode)
                    appendLine(context.getString(R.string.receipt_item_format_print, product.nameSnapshot, unitPrice, product.quantitySold, lineTotal))
                }
            }
            
            appendLine("${context.getString(R.string.total)}: ${String.format(Locale.getDefault(), "%.2f", receipt.totalAmount)} $currencySymbol")
            appendLine()
            appendLine(
                "${context.getString(R.string.receipt_payment_method_label)}: ${
                    if (receipt.paymentMethod == "cash") context.getString(R.string.payment_cash) else context.getString(R.string.payment_card)
                }"
            )
            appendLine("━━━━━━━━━━━━━━━━━━━━")
        }
    }
}
