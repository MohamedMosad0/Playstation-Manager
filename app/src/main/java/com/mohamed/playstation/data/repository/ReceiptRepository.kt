package com.mohamed.playstation.data.repository

import com.mohamed.playstation.data.local.dao.ReceiptDao
import com.mohamed.playstation.data.mapper.ReceiptMapper
import com.mohamed.playstation.domain.model.Receipt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository للتعامل مع بيانات الفواتير
 */
@Singleton
class ReceiptRepository @Inject constructor(
    private val receiptDao: ReceiptDao
) {

    /**
     * إضافة فاتورة جديدة
     * @return ID الفاتورة المضافة
     */
    suspend fun insertReceipt(receipt: Receipt): Long {
        val entity = ReceiptMapper.toEntity(receipt)
        return receiptDao.insert(entity)
    }

    /**
     * تحديث فاتورة موجودة
     */
    suspend fun updateReceipt(receipt: Receipt) {
        val entity = ReceiptMapper.toEntity(receipt)
        receiptDao.update(entity)
    }

    /**
     * حذف فاتورة
     */
    suspend fun deleteReceipt(receipt: Receipt) {
        val entity = ReceiptMapper.toEntity(receipt)
        receiptDao.delete(entity)
    }

    /**
     * الحصول على فاتورة بالـ ID
     */
    suspend fun getReceiptById(receiptId: Long): Receipt? {
        val entity = receiptDao.getReceiptById(receiptId)
        return entity?.let { ReceiptMapper.toModel(it) }
    }

    /**
     * الحصول على فاتورة بالـ ID كـ Flow
     */
    fun getReceiptByIdFlow(receiptId: Long): Flow<Receipt?> {
        return receiptDao.getReceiptByIdFlow(receiptId).map { entity ->
            entity?.let { ReceiptMapper.toModel(it) }
        }
    }

    /**
     * الحصول على فاتورة جلسة محددة
     */
    suspend fun getReceiptBySessionId(sessionId: Long): Receipt? {
        val entity = receiptDao.getReceiptBySessionId(sessionId)
        return entity?.let { ReceiptMapper.toModel(it) }
    }

    /**
     * الحصول على كل الفواتير
     */
    fun getAllReceipts(): Flow<List<Receipt>> {
        return receiptDao.getAllReceipts().map { entities ->
            ReceiptMapper.toModelList(entities)
        }
    }

    /**
     * الحصول على فواتير اليوم
     */
    fun getTodayReceipts(): Flow<List<Receipt>> {
        return receiptDao.getTodayReceipts().map { entities ->
            ReceiptMapper.toModelList(entities)
        }
    }

    /**
     * الحصول على فواتير في فترة زمنية محددة
     */
    fun getReceiptsInRange(startTime: Long, endTime: Long): Flow<List<Receipt>> {
        return receiptDao.getReceiptsInRange(startTime, endTime).map { entities ->
            ReceiptMapper.toModelList(entities)
        }
    }

    /**
     * الحصول على إجمالي الإيرادات اليوم
     */
    fun getTodayTotalRevenue(): Flow<Double> {
        return receiptDao.getTodayTotalRevenue().map { it ?: 0.0 }
    }

    /**
     * توليد رقم فاتورة جديد
     */
    suspend fun generateReceiptNumber(): String {
        val lastNumber = receiptDao.getLastReceiptNumber()

        return if (lastNumber != null) {
            val number = lastNumber.toIntOrNull() ?: 0
            String.format("%07d", number + 1)
        } else {
            "0000001"
        }
    }

    /**
     * حذف كل الفواتير
     */
    suspend fun deleteAllReceipts() {
        receiptDao.deleteAll()
    }
}