package com.mohamed.playstation.core.pdf

import com.mohamed.playstation.core.pdf.mapper.ReceiptPdfMapper
import com.mohamed.playstation.core.pdf.model.ReceiptPdfLabels
import com.mohamed.playstation.presentation.ui.receipts.model.ProductUiModel
import com.mohamed.playstation.presentation.ui.receipts.model.ReceiptUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptPdfMapperTest {

    private val sampleUiModel = ReceiptUiModel(
        receiptId = 1L,
        receiptNumber = "#0001",
        deviceName = "PS5 - 1",
        sessionType = "Single",
        startTime = "10:00 AM",
        endTime = "11:00 AM",
        date = "06/09/2026",
        duration = "1h 0m",
        hasPausedDuration = false,
        pausedDuration = "",
        ratePerHour = "50 EGP",
        playCost = "50.00 EGP",
        productsCost = "15.00 EGP",
        totalAmount = "65.00 EGP",
        paymentMethod = "Cash",
        products = listOf(
            ProductUiModel(
                name = "Pepsi",
                quantity = "1",
                unitPrice = "15.00 EGP",
                totalLinePrice = "15.00 EGP",
                displayLine = "Pepsi 1 x 15.00 EGP"
            )
        ),
        hasProducts = true,
        productsListDisplay = "Pepsi x 1",
        notes = null,
        plainTextShareString = "Receipt #0001"
    )

    private val arabicLabels = ReceiptPdfLabels(
        device = "الجهاز",
        sessionType = "نوع الجلسة",
        duration = "المدة",
        products = "المنتجات",
        playCost = "تكلفة اللعب",
        productsCost = "تكلفة المنتجات",
        total = "الإجمالي",
        paymentMethod = "طريقة الدفع"
    )

    private val englishLabels = ReceiptPdfLabels(
        device = "Device",
        sessionType = "Session Type",
        duration = "Duration",
        products = "Products",
        playCost = "Play Cost",
        productsCost = "Products Cost",
        total = "Total",
        paymentMethod = "Payment"
    )

    @Test
    fun mapToPdfModel_withArabicLabels_preservesArabicLabelsAndRtl() {
        val pdfModel = ReceiptPdfMapper.mapToPdfModel(
            uiModel = sampleUiModel,
            appName = "PS Manager",
            footerMessage = "قريباً",
            labels = arabicLabels,
            isRtl = true
        )

        assertTrue(pdfModel.isRtl)
        assertEquals("الجهاز", pdfModel.labels.device)
        assertEquals("نوع الجلسة", pdfModel.labels.sessionType)
        assertEquals("المدة", pdfModel.labels.duration)
        assertEquals("المنتجات", pdfModel.labels.products)
        assertEquals("تكلفة اللعب", pdfModel.labels.playCost)
        assertEquals("تكلفة المنتجات", pdfModel.labels.productsCost)
        assertEquals("الإجمالي", pdfModel.labels.total)
        assertEquals("طريقة الدفع", pdfModel.labels.paymentMethod)
    }

    @Test
    fun mapToPdfModel_withEnglishLabels_preservesEnglishLabelsAndLtr() {
        val pdfModel = ReceiptPdfMapper.mapToPdfModel(
            uiModel = sampleUiModel,
            appName = "PS Manager",
            footerMessage = "Coming Soon",
            labels = englishLabels,
            isRtl = false
        )

        assertFalse(pdfModel.isRtl)
        assertEquals("Device", pdfModel.labels.device)
        assertEquals("Session Type", pdfModel.labels.sessionType)
        assertEquals("Duration", pdfModel.labels.duration)
        assertEquals("Products", pdfModel.labels.products)
        assertEquals("Play Cost", pdfModel.labels.playCost)
        assertEquals("Products Cost", pdfModel.labels.productsCost)
        assertEquals("Total", pdfModel.labels.total)
        assertEquals("Payment", pdfModel.labels.paymentMethod)
    }
}
