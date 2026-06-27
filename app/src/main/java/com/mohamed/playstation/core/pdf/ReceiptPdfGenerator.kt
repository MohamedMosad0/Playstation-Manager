package com.mohamed.playstation.core.pdf

import android.content.Context
import android.graphics.*
import android.graphics.drawable.VectorDrawable
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.FileProvider
import com.mohamed.playstation.R
import com.mohamed.playstation.core.pdf.model.ReceiptPdfModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.graphics.withTranslation

/**
 * Generates an 80mm thermal-style PDF receipt (576px width).
 */
@Singleton
class ReceiptPdfGenerator @Inject constructor(@ApplicationContext private val context: Context) {

    private val pageWidth = 576
    private val margin = 32f
    private val contentWidth = pageWidth - (margin * 2)

    private val logoSize = 110

    private val titlePaint = TextPaint().apply {
        color = Color.BLACK
        textSize = 24f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val bodyPaint = TextPaint().apply {
        color = Color.BLACK
        textSize = 16f
    }

    private val boldPaint = TextPaint().apply {
        color = Color.BLACK
        textSize = 20f
        isFakeBoldText = true
    }

    private val footerPaint = TextPaint().apply {
        color = Color.BLACK
        textSize = 18f
        textAlign = Paint.Align.CENTER
    }

    private val dashedLinePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
    }

    fun generate(model: ReceiptPdfModel): Uri? {
        val pdfDir = File(context.cacheDir, "pdf")
        if (!pdfDir.exists()) pdfDir.mkdirs()

        cleanupOldPdfs(pdfDir)

        val file = File(pdfDir, "receipt_${model.receiptNumber}.pdf")

        val document = PdfDocument()

        // Calculate dynamic height
        val pageHeight = calculatePageHeight(model)

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

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
            e.printStackTrace()
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

    private fun calculatePageHeight(model: ReceiptPdfModel): Int {
        var height = margin.toInt()

        // Header: Logo + spacing + App Name + spacing
        height += logoSize       // Logo
        height += 12             // Logo → App Name spacing
        height += 36             // App Name text
        height += 16             // Spacing after App Name
        height += 4              // Dashed divider

        // Receipt info
        height += 30             // Receipt Number
        height += 26             // Date
        height += 16             // Spacing

        // Details (3 key-value rows)
        height += 26 * 3         // Device, Type, Duration
        height += 16             // Spacing after details
        height += 4              // Dashed divider

        // Products
        if (model.products.isNotEmpty()) {
            height += 28         // Products header
            height += 8          // Spacing
            model.products.forEach {
                height += calculateTextHeight(it.name, bodyPaint, contentWidth.toInt())
                val qtyStr = "${it.quantity} × ${it.unitPrice}"
                height += calculateTextHeight(qtyStr, bodyPaint, contentWidth.toInt())
                height += calculateTextHeight(it.totalPrice, boldPaint, contentWidth.toInt())
                height += 16     // Spacing between products
            }
            height += 4          // Dashed divider
            height += 12         // Spacing after products
        }

        // Costs section
        height += 26             // Play Cost
        height += 26             // Products Cost
        height += 16             // Spacing before total
        height += 4              // Dashed divider
        height += 30             // Total (bold)
        height += 26             // Payment Method
        height += 20             // Spacing

        // Footer
        height += 4              // Dashed divider
        height += 16             // Spacing
        height += 28             // Arabic footer line
        height += 28             // English footer line

        return height + margin.toInt()
    }

    private fun drawContent(canvas: Canvas, model: ReceiptPdfModel) {
        var y = margin

        // 1. Logo
        y = drawLogo(canvas, y)
        y += 12f

        // 2. App Name
        canvas.drawText(model.appName, pageWidth / 2f, y + 28f, titlePaint)
        y += 36f + 16f

        // 3. Header divider
        drawDashedLine(canvas, margin, pageWidth - margin, y)
        y += 16f

        // 4. Receipt Info
        drawRtlText(canvas, model.receiptNumber, y, boldPaint)
        y += 30f
        drawRtlText(canvas, model.date, y, bodyPaint)
        y += 26f + 12f

        // 5. Details
        drawKeyValue(canvas, context.getString(R.string.device), model.deviceName, y)
        y += 26f
        drawKeyValue(canvas, context.getString(R.string.receipt_type_label), model.sessionType, y)
        y += 26f
        drawKeyValue(canvas, context.getString(R.string.duration), model.duration, y)
        y += 26f + 8f

        // 6. Products
        if (model.products.isNotEmpty()) {
            drawDashedLine(canvas, margin, pageWidth - margin, y)
            y += 12f

            drawRtlText(canvas, context.getString(R.string.products), y, boldPaint)
            y += 28f

            model.products.forEach { product ->
                val nameLayout = createStaticLayout(product.name, bodyPaint, contentWidth.toInt(), Layout.Alignment.ALIGN_OPPOSITE)
                canvas.withTranslation(margin, y) {
                    nameLayout.draw(this)
                }
                y += nameLayout.height

                val qtyAndPrice = "${product.quantity} × ${product.unitPrice}"
                val qtyLayout = createStaticLayout(qtyAndPrice, bodyPaint, contentWidth.toInt(), Layout.Alignment.ALIGN_OPPOSITE)
                canvas.withTranslation(margin, y) {
                    qtyLayout.draw(this)
                }
                y += qtyLayout.height

                val totalLayout = createStaticLayout(product.totalPrice, boldPaint, contentWidth.toInt(), Layout.Alignment.ALIGN_OPPOSITE)
                canvas.withTranslation(margin, y) {
                    totalLayout.draw(this)
                }
                y += totalLayout.height + 16f
            }

            drawDashedLine(canvas, margin, pageWidth - margin, y)
            y += 16f
        } else {
            drawDashedLine(canvas, margin, pageWidth - margin, y)
            y += 16f
        }

        // 7. Costs
        drawKeyValue(canvas, context.getString(R.string.play_cost), model.playCost, y)
        y += 26f
        drawKeyValue(canvas, context.getString(R.string.products_cost), model.productsCost, y)
        y += 26f + 8f

        // 8. Total divider
        drawDashedLine(canvas, margin, pageWidth - margin, y)
        y += 12f

        drawKeyValue(canvas, context.getString(R.string.total), model.totalAmount, y, boldPaint)
        y += 30f
        drawKeyValue(canvas, context.getString(R.string.receipt_payment_method_label), model.paymentMethod, y)
        y += 26f + 16f

        // 9. Footer divider
        drawDashedLine(canvas, margin, pageWidth - margin, y)
        y += 16f

        // 10. Bilingual footer
        canvas.drawText("شكراً لزيارتكم", pageWidth / 2f, y + 22f, footerPaint)
        y += 28f
        canvas.drawText("Thank You For Visiting", pageWidth / 2f, y + 22f, footerPaint)
    }

    private fun drawLogo(canvas: Canvas, startY: Float): Float {
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_ps_logo) ?: return startY
        val logoLeft = ((pageWidth - logoSize) / 2f).toInt()
        val logoTop = startY.toInt()
        drawable.setBounds(logoLeft, logoTop, logoLeft + logoSize, logoTop + logoSize)
        drawable.draw(canvas)
        return startY + logoSize
    }

    private fun drawDashedLine(canvas: Canvas, startX: Float, endX: Float, y: Float) {
        canvas.drawLine(startX, y, endX, y, dashedLinePaint)
    }

    private fun drawKeyValue(canvas: Canvas, key: String, value: String, y: Float, paint: TextPaint = bodyPaint) {
        val keyLayout = createStaticLayout(key, paint, (contentWidth * 0.5f).toInt(), Layout.Alignment.ALIGN_OPPOSITE)
        canvas.withTranslation(pageWidth - margin - (contentWidth * 0.5f), y) {
            keyLayout.draw(this)
        }

        val valueLayout = createStaticLayout(value, paint, (contentWidth * 0.45f).toInt(), Layout.Alignment.ALIGN_NORMAL)
        canvas.withTranslation(margin, y) {
            valueLayout.draw(this)
        }
    }

    private fun drawRtlText(canvas: Canvas, text: String, y: Float, paint: TextPaint) {
        val layout = createStaticLayout(text, paint, contentWidth.toInt(), Layout.Alignment.ALIGN_OPPOSITE)
        canvas.withTranslation(margin, y) {
            layout.draw(this)
        }
    }

    private fun createStaticLayout(text: String, paint: TextPaint, width: Int, alignment: Layout.Alignment): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(alignment)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
    }

    private fun calculateTextHeight(text: String, paint: TextPaint, width: Int): Int {
        val layout = createStaticLayout(text, paint, width, Layout.Alignment.ALIGN_OPPOSITE)
        return layout.height
    }
}
