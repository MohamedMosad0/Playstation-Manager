package com.mohamed.playstation.domain.usecase

import com.mohamed.playstation.data.repository.ReceiptRepository
import com.mohamed.playstation.domain.model.Receipt
import com.mohamed.playstation.domain.model.Session
import com.mohamed.playstation.domain.model.SessionProduct
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject

/**
 * Use Cases للفواتير
 */
class ReceiptUseCases @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val sessionProductUseCases: SessionProductUseCases
) {

    /**
     * إنشاء فاتورة من جلسة منتهية
     * @param pricePerHour السعر الحالي من الإعدادات (ليس من الجلسة المحفوظة)
     */
    suspend fun createReceiptFromSession(
        session: Session,
        currencyCode: String,
        pricePerHour: Double,
        paymentMethod: String? = null
    ): Long {
        // توليد رقم فاتورة جديد
        val receiptNumber = receiptRepository.generateReceiptNumber()

        // حساب المدة والإجمالي بالسعر الحالي
        val durationMinutes = session.getDurationMinutes()
        val playAmount = com.mohamed.playstation.core.utils.SessionPricing.calculatePlayAmount(durationMinutes, pricePerHour)
        val productsAmount = SessionProduct.calculateTotalAmount(
            sessionProductUseCases.getProductsBySessionIdOnce(session.id)
        )
        val discountAmount = 0.0
        val taxAmount = 0.0
        val totalAmount = playAmount + productsAmount - discountAmount + taxAmount

        require(
            kotlin.math.abs(
                totalAmount -
                (
                    playAmount +
                    productsAmount -
                    discountAmount +
                    taxAmount
                )
            ) < 0.01
        )

        // إنشاء الفاتورة
        val receipt = Receipt(
            sessionId = session.id,
            receiptNumber = receiptNumber,
            deviceType = session.deviceType,
            deviceNumber = session.deviceNumber,
            sessionType = session.sessionType,
            startTime = session.startTime,
            endTime = session.endTime ?: Date(),
            durationMinutes = durationMinutes,
            pricePerHour = pricePerHour,
            playAmount = playAmount,
            productsAmount = productsAmount,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            totalAmount = totalAmount,
            currencyCode = currencyCode,
            paymentMethod = paymentMethod,
            notes = session.notes
        )

        return receiptRepository.insertReceipt(receipt)
    }

    /**
     * تحديث طريقة الدفع للفاتورة
     */
    suspend fun updatePaymentMethod(receipt: Receipt, paymentMethod: String) {
        val updatedReceipt = receipt.copy(paymentMethod = paymentMethod)
        receiptRepository.updateReceipt(updatedReceipt)
    }

    /**
     * تحديث ملاحظات الفاتورة
     */
    suspend fun updateReceiptNotes(receipt: Receipt, notes: String) {
        val updatedReceipt = receipt.copy(notes = notes)
        receiptRepository.updateReceipt(updatedReceipt)
    }

    /**
     * حذف فاتورة
     */
    suspend fun deleteReceipt(receipt: Receipt) {
        receiptRepository.deleteReceipt(receipt)
    }

    /**
     * الحصول على فاتورة بالـ ID
     */
    suspend fun getReceiptById(receiptId: Long): Receipt? {
        return receiptRepository.getReceiptById(receiptId)
    }

    /**
     * الحصول على فاتورة جلسة محددة
     */
    suspend fun getReceiptBySessionId(sessionId: Long): Receipt? {
        return receiptRepository.getReceiptBySessionId(sessionId)
    }

    /**
     * الحصول على كل الفواتير
     */
    fun getAllReceipts(): Flow<List<Receipt>> {
        return receiptRepository.getAllReceipts()
    }

    /**
     * الحصول على فواتير اليوم
     */
    fun getTodayReceipts(): Flow<List<Receipt>> {
        return receiptRepository.getTodayReceipts()
    }

    /**
     * الحصول على إجمالي الإيرادات اليوم
     */
    fun getTodayTotalRevenue(): Flow<Double> {
        return receiptRepository.getTodayTotalRevenue()
    }
}
