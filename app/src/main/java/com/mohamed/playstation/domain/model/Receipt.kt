package com.mohamed.playstation.domain.model

import java.text.SimpleDateFormat
import java.util.*

/**
 * Domain Model للفاتورة
 */
data class Receipt(
    val id: Long = 0,
    val sessionId: Long,
    val receiptNumber: String,
    val deviceType: String,
    val deviceNumber: Int,
    val sessionType: String,
    val startTime: Date,
    val endTime: Date,
    val durationMinutes: Long,
    val pricePerHour: Double,
    val totalAmount: Double,
    val currencyCode: String,
    val paymentMethod: String? = null,
    val notes: String? = null,
    val createdAt: Date = Date()
) {

    /**
     * تنسيق وقت البداية
     */
    fun getFormattedStartTime(): String {
        val format = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        return format.format(startTime)
    }

    /**
     * تنسيق وقت النهاية
     */
    fun getFormattedEndTime(): String {
        val format = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        return format.format(endTime)
    }

    /**
     * تنسيق التاريخ
     */
    fun getFormattedDate(): String {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return format.format(createdAt)
    }

    /**
     * تنسيق المدة
     */
    fun getFormattedDuration(): String {
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    /**
     * اسم الجهاز الكامل
     */
    fun getFullDeviceName(): String {
        return "$deviceType #$deviceNumber"
    }

    /**
     * نوع الجلسة بالعربي
     */
    fun getSessionTypeArabic(): String {
        return when (sessionType) {
            "single" -> "لاعب واحد"
            "multi" -> "متعدد اللاعبين"
            else -> sessionType
        }
    }
}
