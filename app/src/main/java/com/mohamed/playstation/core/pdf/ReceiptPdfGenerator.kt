package com.mohamed.playstation.core.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import androidx.core.content.FileProvider
import androidx.core.graphics.withTranslation
import com.mohamed.playstation.R
import com.mohamed.playstation.core.pdf.model.ReceiptPdfModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates an 80mm thermal-style PDF receipt (576px width).
 * Redesigned for professional POS layout, supporting true RTL/LTR alignment.
 */
@Singleton
class ReceiptPdfGenerator @Inject constructor(@ApplicationContext private val context: Context) {

    private val pageWidth = 576
    private val margin = 32f
    private val contentWidth = pageWidth - (margin * 2)

    private val logoSize = 110

    private val titlePaint = TextPaint().apply {
        color = Color.BLACK
        textSize = 32f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val sectionHeaderPaint = TextPaint().apply {
        color = Color.BLACK
        textSize = 20f
        isFakeBoldText = true
    }

    private val bodyPaint = TextPaint().apply {
        color = Color.BLACK
        textSize = 18f
    }

    private val bodySecondaryPaint = TextPaint().apply {
        color = Color.rgb(80, 80, 80)
        textSize = 16f
    }

    private val boldPaint = TextPaint().apply {
        color = Color.BLACK
        textSize = 18f
        isFakeBoldText = true
    }

    private val totalLabelPaint = TextPaint().apply {
        color = Color.BLACK
        textSize = 24f
        isFakeBoldText = true
    }

    private val totalValuePaint = TextPaint().apply {
        color = Color.BLACK
        textSize = 28f
        isFakeBoldText = true
    }

    private val footerPaint = TextPaint().apply {
        color = Color.BLACK
        textSize = 16f
        textAlign = Paint.Align.CENTER
    }

    private val linePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val dashedLinePaint = Paint().apply {
        color = Color.rgb(100, 100, 100)
        style = Paint.Style.STROKE
        strokeWidth = 1f
        pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
    }

    fun generate(model: ReceiptPdfModel): Uri? {
        val pdfDir = File(context.cacheDir, "pdf")
        if (!pdfDir.exists()) pdfDir.mkdirs()

        cleanupOldPdfs(pdfDir)

        val file = File(pdfDir, "receipt_${model.receiptNumber}.pdf")

        val document = PdfDocument()

        // Calculate dynamic height with a dry-run
        val calculatedHeight = drawContent(null, model).toInt()
        val pageHeight = maxOf(calculatedHeight + margin.toInt(), 600)

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Real draw
        drawContent(canvas, model)

        document.finishPage(page)

        return try {
            FileOutputStream(file).use { outputStream ->
                document.writeTo(outputStream)
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to write receipt PDF")
            null
        } finally {
            document.close()
        }
    }

    private fun cleanupOldPdfs(pdfDir: File) {
        val files = pdfDir.listFiles() ?: return
        if (files.size > 50) {
            files.filter { it.isFile }
                .sortedByDescending { it.lastModified() }
                .drop(50)
                .forEach { it.delete() }
        }
    }

    private fun drawContent(canvas: Canvas?, model: ReceiptPdfModel): Float {
        var y = margin

        // 1. Logo
        y = drawLogo(canvas, y)
        y += 16f

        // 2. Store Name
        if (canvas != null) {
            canvas.drawText(model.appName, pageWidth / 2f, y + 24f, titlePaint)
        }
        y += 36f

        // 3. Receipt Number & Date
        y = drawCenterText(canvas, model.receiptNumber, y, bodyPaint)
        y += 4f
        y = drawCenterText(canvas, model.date, y, bodySecondaryPaint)
        y += 16f

        y = drawSolidLine(canvas, y)
        y += 16f

        // 4. Details
        y = drawRow(canvas, context.getString(R.string.device), model.deviceName, y, bodySecondaryPaint, boldPaint)
        y += 8f
        y = drawRow(canvas, context.getString(R.string.session_type), model.sessionType, y, bodySecondaryPaint, boldPaint)
        y += 8f
        y = drawRow(canvas, context.getString(R.string.duration), model.duration, y, bodySecondaryPaint, boldPaint)
        y += 16f

        // 5. Products
        if (model.products.isNotEmpty()) {
            y = drawDashedLine(canvas, y)
            y += 16f

            y = drawStartText(canvas, context.getString(R.string.products), y, sectionHeaderPaint)
            y += 12f

            model.products.forEach { product ->
                // Row 1: Product Name
                y = drawStartText(canvas, product.name, y, boldPaint)
                y += 4f

                // Row 2: Qty x Price and Total
                val qtyStr = "${product.quantity} × ${product.unitPrice}"
                y = drawRow(canvas, qtyStr, product.totalPrice, y, bodySecondaryPaint, boldPaint)
                y += 12f
            }
        }

        // 6. Summary
        y = drawDashedLine(canvas, y)
        y += 16f

        y = drawRow(canvas, context.getString(R.string.play_cost), model.playCost, y, bodySecondaryPaint, bodyPaint)
        y += 8f
        y = drawRow(canvas, context.getString(R.string.products_cost), model.productsCost, y, bodySecondaryPaint, bodyPaint)
        y += 16f

        y = drawSolidLine(canvas, y)
        y += 16f

        // 7. TOTAL
        y = drawRow(canvas, context.getString(R.string.total), model.totalAmount, y, totalLabelPaint, totalValuePaint)
        y += 24f

        // 8. Payment Method
        y = drawRow(canvas, context.getString(R.string.receipt_payment_method_label), model.paymentMethod, y, bodySecondaryPaint, boldPaint)
        y += 32f

        // 9. Footer
        if (canvas != null) {
            canvas.drawText("شكراً لزيارتكم", pageWidth / 2f, y + 16f, footerPaint)
        }
        y += 24f
        if (canvas != null) {
            canvas.drawText("Thank You For Visiting", pageWidth / 2f, y + 16f, footerPaint)
        }
        y += 24f

        return y
    }

    private fun drawLogo(canvas: Canvas?, startY: Float): Float {
        if (canvas != null) {
            val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_app_mark)
            if (drawable != null) {
                val logoLeft = ((pageWidth - logoSize) / 2f).toInt()
                val logoTop = startY.toInt()
                drawable.setBounds(logoLeft, logoTop, logoLeft + logoSize, logoTop + logoSize)
                drawable.draw(canvas)
            }
        }
        return startY + logoSize
    }

    private fun drawSolidLine(canvas: Canvas?, y: Float): Float {
        if (canvas != null) canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        return y + 4f
    }

    private fun drawDashedLine(canvas: Canvas?, y: Float): Float {
        if (canvas != null) canvas.drawLine(margin, y, pageWidth - margin, y, dashedLinePaint)
        return y + 4f
    }

    private fun drawCenterText(canvas: Canvas?, text: String, y: Float, paint: TextPaint): Float {
        val layout = createStaticLayout(text, paint, contentWidth.toInt(), Layout.Alignment.ALIGN_CENTER)
        if (canvas != null) {
            canvas.withTranslation(margin, y) { layout.draw(this) }
        }
        return y + layout.height
    }

    private fun drawStartText(canvas: Canvas?, text: String, y: Float, paint: TextPaint): Float {
        val layout = createStaticLayout(text, paint, contentWidth.toInt(), Layout.Alignment.ALIGN_NORMAL)
        if (canvas != null) {
            canvas.withTranslation(margin, y) { layout.draw(this) }
        }
        return y + layout.height
    }

    private fun drawRow(canvas: Canvas?, key: String, value: String, y: Float, keyPaint: TextPaint, valuePaint: TextPaint): Float {
        val isRtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

        val halfWidth = (contentWidth * 0.5f).toInt()

        val keyAlign = Layout.Alignment.ALIGN_NORMAL // Start
        val valueAlign = Layout.Alignment.ALIGN_OPPOSITE // End

        val keyLayout = createStaticLayout(key, keyPaint, halfWidth, keyAlign)
        val valueLayout = createStaticLayout(value, valuePaint, halfWidth, valueAlign)

        val keyX = if (isRtl) margin + halfWidth else margin
        val valueX = if (isRtl) margin else margin + halfWidth

        if (canvas != null) {
            canvas.withTranslation(keyX, y) { keyLayout.draw(this) }
            canvas.withTranslation(valueX, y) { valueLayout.draw(this) }
        }

        return y + maxOf(keyLayout.height, valueLayout.height)
    }

    private fun createStaticLayout(text: String, paint: TextPaint, width: Int, alignment: Layout.Alignment): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(alignment)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
    }
}
