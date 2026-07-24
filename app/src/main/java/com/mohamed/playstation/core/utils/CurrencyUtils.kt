package com.mohamed.playstation.core.utils

import android.content.Context
import com.mohamed.playstation.domain.model.CurrencyList

/**
 * Helper functions للعملات
 */
object CurrencyUtils {

    /**
     * تنسيق المبلغ مع رمز العملة
     *
     * @param context السياق لحل الرمز
     * @param amount المبلغ
     * @param currencyCode كود العملة (EGP, SAR, etc.)
     * @return المبلغ منسق مع الرمز مثل: "30.00 ج.م"
     */
    fun formatAmount(context: Context, amount: Double, currencyCode: String): String {
        val symbol = getCurrencySymbol(context, currencyCode)
        val formatted = AppFormatters.formatAmount(context, amount)
        return "$formatted $symbol"
    }

    /**
     * تنسيق المبلغ بدون رمز العملة
     *
     * @param amount المبلغ
     * @return المبلغ منسق مثل: "30.00"
     */
    fun formatAmountOnly(context: Context, amount: Double): String {
        return AppFormatters.formatAmount(context, amount)
    }

    /**
     * الحصول على رمز العملة فقط
     *
     * @param context السياق لحل الرمز
     * @param currencyCode كود العملة
     * @return الرمز مثل: "ج.م"
     */
    fun getCurrencySymbol(context: Context, currencyCode: String): String {
        val currency = CurrencyList.getCurrencyByCode(currencyCode)
        return context.getString(currency.symbolRes)
    }

    /**
     * حساب الإجمالي لجلسة
     *
     * @param durationMinutes مدة الجلسة بالدقائق
     * @param pricePerHour السعر بالساعة
     * @return الإجمالي
     */
    fun calculateTotal(durationMinutes: Long, pricePerHour: Double): Double {
        return com.mohamed.playstation.core.utils.SessionPricing.calculatePlayAmount(durationMinutes, pricePerHour)
    }

    /**
     * تنسيق الإجمالي مع العملة
     *
     * @param context السياق لحل الرمز
     * @param durationMinutes مدة الجلسة بالدقائق
     * @param pricePerHour السعر بالساعة
     * @param currencyCode كود العملة
     * @return الإجمالي منسق مثل: "45.50 ج.م"
     */
    fun formatTotal(
        context: Context,
        durationMinutes: Long,
        pricePerHour: Double,
        currencyCode: String
    ): String {
        val total = calculateTotal(durationMinutes, pricePerHour)
        return formatAmount(context, total, currencyCode)
    }
}
