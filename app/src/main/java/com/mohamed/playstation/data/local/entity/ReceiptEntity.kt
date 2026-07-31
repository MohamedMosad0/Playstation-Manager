package com.mohamed.playstation.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * جدول الفواتير في قاعدة البيانات
 */
@Entity(
    tableName = "receipts",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("createdAt")]
)
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // ربط الفاتورة بالجلسة
    val sessionId: Long,

    // رقم الفاتورة
    val receiptNumber: String,      // مثال: "0001234"

    // تفاصيل الجلسة
    val deviceType: String,
    val deviceNumber: Int,
    val sessionType: String,

    // الأوقات
    val startTime: Date,
    val endTime: Date,
    val durationMinutes: Long,      // المدة الإجمالية بالدقائق

    // الأسعار
    val pricePerHour: Double,       // السعر بالساعة
    val playAmount: Double,
    val productsAmount: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val totalAmount: Double,        // الإجمالي النهائي
    val currencyCode: String,       // كود العملة (EGP, SAR, etc.)

    // طريقة الدفع
    val paymentMethod: String? = null, // cash, card, etc.

    // ملاحظات
    val notes: String? = null,

    // تاريخ الإنشاء
    val createdAt: Date = Date()
)
