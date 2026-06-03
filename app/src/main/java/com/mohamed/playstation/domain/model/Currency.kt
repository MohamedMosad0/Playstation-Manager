package com.mohamed.playstation.domain.model

import com.mohamed.playstation.core.constants.AppConstants

/**
 * Data class للعملات المدعومة
 */
data class Currency(
    val code: String,
    val nameAr: String,
    val symbol: String
)

/**
 * قائمة العملات المتاحة
 */
object CurrencyList {

    val currencies = listOf(
        Currency(
            code = AppConstants.CURRENCY_EGP,
            nameAr = "جنيه مصري",
            symbol = "ج.م"
        ),
        Currency(
            code = AppConstants.CURRENCY_SAR,
            nameAr = "ريال سعودي",
            symbol = "ر.س"
        ),
        Currency(
            code = AppConstants.CURRENCY_AED,
            nameAr = "درهم إماراتي",
            symbol = "د.إ"
        ),
        Currency(
            code = AppConstants.CURRENCY_USD,
            nameAr = "دولار أمريكي",
            symbol = "$"
        ),
        Currency(
            code = AppConstants.CURRENCY_EUR,
            nameAr = "يورو",
            symbol = "€"
        ),
        Currency(
            code = AppConstants.CURRENCY_KWD,
            nameAr = "دينار كويتي",
            symbol = "د.ك"
        ),
        Currency(
            code = AppConstants.CURRENCY_BHD,
            nameAr = "دينار بحريني",
            symbol = "د.ب"
        ),
        Currency(
            code = AppConstants.CURRENCY_OMR,
            nameAr = "ريال عماني",
            symbol = "ر.ع"
        ),
        Currency(
            code = AppConstants.CURRENCY_QAR,
            nameAr = "ريال قطري",
            symbol = "ر.ق"
        ),
        Currency(
            code = AppConstants.CURRENCY_JOD,
            nameAr = "دينار أردني",
            symbol = "د.أ"
        ),
        Currency(
            code = AppConstants.CURRENCY_IQD,
            nameAr = "دينار عراقي",
            symbol = "د.ع"
        ),
        Currency(
            code = AppConstants.CURRENCY_LBP,
            nameAr = "ليرة لبنانية",
            symbol = "ل.ل"
        ),
        Currency(
            code = AppConstants.CURRENCY_MAD,
            nameAr = "درهم مغربي",
            symbol = "د.م"
        ),
        Currency(
            code = AppConstants.CURRENCY_TND,
            nameAr = "دينار تونسي",
            symbol = "د.ت"
        ),
        Currency(
            code = AppConstants.CURRENCY_DZD,
            nameAr = "دينار جزائري",
            symbol = "د.ج"
        ),
        Currency(
            code = AppConstants.CURRENCY_LYD,
            nameAr = "دينار ليبي",
            symbol = "د.ل"
        )
    )

    /**
     * الحصول على العملة من الكود
     */
    fun getCurrencyByCode(code: String): Currency {
        return currencies.find { it.code == code }
            ?: currencies.first() // Default to first currency (EGP)
    }

    /**
     * الحصول على رمز العملة من الكود
     */
    fun getSymbol(code: String): String {
        return getCurrencyByCode(code).symbol
    }
}