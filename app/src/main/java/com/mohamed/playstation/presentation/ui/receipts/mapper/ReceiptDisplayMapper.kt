package com.mohamed.playstation.presentation.ui.receipts.mapper

import android.content.Context
import com.mohamed.playstation.R
import com.mohamed.playstation.core.utils.AppFormatters
import com.mohamed.playstation.core.utils.CurrencyUtils
import com.mohamed.playstation.core.utils.SessionPricing
import com.mohamed.playstation.domain.model.Receipt
import com.mohamed.playstation.domain.model.Session
import com.mohamed.playstation.domain.model.SessionProduct
import com.mohamed.playstation.domain.model.SessionProductSummary
import com.mohamed.playstation.domain.model.aggregate
import com.mohamed.playstation.presentation.ui.receipts.model.ProductUiModel
import com.mohamed.playstation.presentation.ui.receipts.model.ReceiptListItemUiModel
import com.mohamed.playstation.presentation.ui.receipts.model.ReceiptUiModel
import java.util.Date
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
        val deviceName = formatFullDeviceName(context, receipt.deviceType, receipt.deviceNumber)
        val sessionType = formatSessionType(context, receipt.sessionType)
        val startTime = formatTime(context, receipt.startTime)
        val endTime = formatTime(context, receipt.endTime)
        val date = formatDate(context, receipt.createdAt)
        val duration = formatDuration(context, receipt.durationMinutes)

        val pausedMinutes = session?.totalPausedMinutes ?: 0L
        val hasPausedDuration = pausedMinutes > 0
        val pausedDuration = if (hasPausedDuration) formatDuration(context, pausedMinutes) else ""

        val ratePerHour = context.getString(
            R.string.receipt_rate_per_hour,
            AppFormatters.formatAmount(context, receipt.pricePerHour),
            currencySymbol
        )
        val playCost = CurrencyUtils.formatAmount(context, playAmount, receipt.currencyCode)
        val productsCost = CurrencyUtils.formatAmount(context, productsAmount, receipt.currencyCode)
        val totalAmount = CurrencyUtils.formatAmount(context, receipt.totalAmount, receipt.currencyCode)

        // --- Product display lines ---
        val aggregatedProducts = products.aggregate()
        val productUiModels = aggregatedProducts.map { product ->
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
                name = android.text.BidiFormatter.getInstance().unicodeWrap(product.nameSnapshot),
                quantity = AppFormatters.formatInteger(context, product.quantitySold),
                unitPrice = unitPrice,
                totalLinePrice = lineTotal,
                displayLine = displayLine
            )
        }

        val productsListDisplay = productUiModels.joinToString("\n") { it.displayLine }

        // --- Payment ---
        val paymentMethod = when (receipt.paymentMethod) {
            "cash" -> context.getString(R.string.payment_cash)
            "card" -> context.getString(R.string.payment_card)
            else -> context.getString(R.string.payment_cash) // Default to Cash
        }

        // --- Share text ---
        val plainTextShareString = buildShareText(
            context = context,
            receipt = receipt,
            session = session,
            products = aggregatedProducts,
            currencySymbol = currencySymbol,
            playAmount = playAmount,
            productsAmount = productsAmount,
            paymentMethodLabel = paymentMethod
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
            hasProducts = aggregatedProducts.isNotEmpty(),
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
        val deviceInfo = "${formatFullDeviceName(context, receipt.deviceType, receipt.deviceNumber)} • $sessionTypeText"
        
        val timeRange = "${formatTime(context, receipt.startTime)} - ${formatTime(context, receipt.endTime)}"
        val durationAndRange = "${formatDuration(context, receipt.durationMinutes)} • $timeRange"
        
        val totalAmount = CurrencyUtils.formatAmount(context, receipt.totalAmount, receipt.currencyCode)
        
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

    private fun formatTime(context: Context, date: Date): String =
        AppFormatters.formatTime(context, date)

    private fun formatDate(context: Context, date: Date): String =
        AppFormatters.formatDate(context, date)

    private fun formatDuration(context: Context, durationMinutes: Long): String =
        AppFormatters.formatDuration(context, durationMinutes)

    private fun formatFullDeviceName(context: Context, deviceType: String, deviceNumber: Int): String {
        val raw = "$deviceType #${AppFormatters.formatInteger(context, deviceNumber)}"
        return android.text.BidiFormatter.getInstance().unicodeWrap(raw)
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
        productsAmount: Double,
        paymentMethodLabel: String
    ): String {
        return buildString {
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("🎮 ${context.getString(R.string.app_name)}")
            appendLine(context.getString(R.string.receipt_number, receipt.receiptNumber))
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            appendLine("${context.getString(R.string.device)}: ${formatFullDeviceName(context, receipt.deviceType, receipt.deviceNumber)}")
            appendLine("${context.getString(R.string.receipt_type_label)} ${formatSessionType(context, receipt.sessionType)}")
            appendLine("${context.getString(R.string.started)}: ${formatTime(context, receipt.startTime)}")
            appendLine("${context.getString(R.string.ended)}: ${formatTime(context, receipt.endTime)}")
            appendLine("${context.getString(R.string.duration)}: ${formatDuration(context, receipt.durationMinutes)}")
            
            if ((session?.totalPausedMinutes ?: 0L) > 0) {
                appendLine("${context.getString(R.string.pause_duration)}: ${formatDuration(context, session?.totalPausedMinutes ?: 0L)}")
            }
            
            appendLine()
            appendLine(
                "${context.getString(R.string.rate)}: ${
                    context.getString(
                        R.string.receipt_rate_per_hour,
                        AppFormatters.formatAmount(context, receipt.pricePerHour),
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
            
            appendLine("${context.getString(R.string.total)}: ${CurrencyUtils.formatAmount(context, receipt.totalAmount, receipt.currencyCode)}")
            appendLine("${context.getString(R.string.receipt_payment_method_label)}: $paymentMethodLabel")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
        }
    }
}
