package com.mohamed.playstation.data.local.dao

import androidx.room.*
import com.mohamed.playstation.data.local.entity.ReceiptEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object للفواتير
 */
@Dao
interface ReceiptDao {

    /**
     * إدراج فاتورة جديدة
     * @return ID الفاتورة المضافة
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(receipt: ReceiptEntity): Long

    /**
     * تحديث فاتورة موجودة
     */
    @Update
    suspend fun update(receipt: ReceiptEntity)

    /**
     * حذف فاتورة
     */
    @Delete
    suspend fun delete(receipt: ReceiptEntity)

    /**
     * الحصول على فاتورة بالـ ID
     */
    @Query("SELECT * FROM receipts WHERE id = :receiptId")
    suspend fun getReceiptById(receiptId: Long): ReceiptEntity?

    /**
     * الحصول على فاتورة بالـ ID كـ Flow
     */
    @Query("SELECT * FROM receipts WHERE id = :receiptId")
    fun getReceiptByIdFlow(receiptId: Long): Flow<ReceiptEntity?>

    /**
     * الحصول على فاتورة جلسة محددة
     */
    @Query("SELECT * FROM receipts WHERE sessionId = :sessionId")
    suspend fun getReceiptBySessionId(sessionId: Long): ReceiptEntity?

    /**
     * الحصول على كل الفواتير
     */
    @Query("SELECT * FROM receipts ORDER BY createdAt DESC")
    fun getAllReceipts(): Flow<List<ReceiptEntity>>

    /**
     * الحصول على فواتير اليوم
     */
    @Query("""
        SELECT * FROM receipts 
        WHERE DATE(createdAt/1000, 'unixepoch') = DATE('now') 
        ORDER BY createdAt DESC
    """)
    fun getTodayReceipts(): Flow<List<ReceiptEntity>>

    /**
     * الحصول على إجمالي الإيرادات اليوم
     */
    @Query("""
        SELECT SUM(totalAmount) FROM receipts 
        WHERE DATE(createdAt/1000, 'unixepoch') = DATE('now')
    """)
    fun getTodayTotalRevenue(): Flow<Double?>

    /**
     * الحصول على آخر رقم فاتورة
     */
    @Query("SELECT receiptNumber FROM receipts ORDER BY id DESC LIMIT 1")
    suspend fun getLastReceiptNumber(): String?

    /**
     * حذف كل الفواتير
     */
    @Query("DELETE FROM receipts")
    suspend fun deleteAll()
}