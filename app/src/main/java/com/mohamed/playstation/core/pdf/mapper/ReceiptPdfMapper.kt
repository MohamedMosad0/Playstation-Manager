package com.mohamed.playstation.core.pdf.mapper

import com.mohamed.playstation.core.pdf.model.ProductPdfModel
import com.mohamed.playstation.core.pdf.model.ReceiptPdfModel
import com.mohamed.playstation.presentation.ui.receipts.model.ReceiptUiModel

/**
 * Pure mapper that transforms ReceiptUiModel into ReceiptPdfModel.
 * No business logic, no context, no calculations.
 */
object ReceiptPdfMapper {

    fun mapToPdfModel(
        uiModel: ReceiptUiModel,
        appName: String,
        footerMessage: String
    ): ReceiptPdfModel {
        return ReceiptPdfModel(
            receiptNumber = uiModel.receiptNumber,
            date = uiModel.date,
            deviceName = uiModel.deviceName,
            sessionType = uiModel.sessionType,
            duration = uiModel.duration,
            products = uiModel.products.map { product ->
                ProductPdfModel(
                    name = product.name,
                    quantity = product.quantity,
                    unitPrice = product.unitPrice,
                    totalPrice = product.totalLinePrice
                )
            },
            playCost = uiModel.playCost,
            productsCost = uiModel.productsCost,
            totalAmount = uiModel.totalAmount,
            paymentMethod = uiModel.paymentMethod ?: "",
            currencyCode = "", // Already formatted in totalAmount/playCost strings
            appName = appName,
            footerMessage = footerMessage
        )
    }
}
