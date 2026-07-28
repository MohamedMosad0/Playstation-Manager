package com.mohamed.playstation.domain.model

import androidx.annotation.StringRes
import com.mohamed.playstation.R
import com.mohamed.playstation.core.constants.AppConstants

/**
 * Enum للعملات المدعومة
 */
enum class Currency(
    val code: String,
    @param:StringRes val displayNameRes: Int,
    @param:StringRes val symbolRes: Int
) {
    EGP(AppConstants.CURRENCY_EGP, R.string.currency_egp, R.string.symbol_egp),
    SAR(AppConstants.CURRENCY_SAR, R.string.currency_sar, R.string.symbol_sar),
    AED(AppConstants.CURRENCY_AED, R.string.currency_aed, R.string.symbol_aed),
    USD(AppConstants.CURRENCY_USD, R.string.currency_usd, R.string.symbol_usd),
    EUR(AppConstants.CURRENCY_EUR, R.string.currency_eur, R.string.symbol_eur),
    KWD(AppConstants.CURRENCY_KWD, R.string.currency_kwd, R.string.symbol_kwd),
    BHD(AppConstants.CURRENCY_BHD, R.string.currency_bhd, R.string.symbol_bhd),
    OMR(AppConstants.CURRENCY_OMR, R.string.currency_omr, R.string.symbol_omr),
    QAR(AppConstants.CURRENCY_QAR, R.string.currency_qar, R.string.symbol_qar),
    JOD(AppConstants.CURRENCY_JOD, R.string.currency_jod, R.string.symbol_jod),
    IQD(AppConstants.CURRENCY_IQD, R.string.currency_iqd, R.string.symbol_iqd),
    LBP(AppConstants.CURRENCY_LBP, R.string.currency_lbp, R.string.symbol_lbp),
    MAD(AppConstants.CURRENCY_MAD, R.string.currency_mad, R.string.symbol_mad),
    TND(AppConstants.CURRENCY_TND, R.string.currency_tnd, R.string.symbol_tnd),
    DZD(AppConstants.CURRENCY_DZD, R.string.currency_dzd, R.string.symbol_dzd),
    LYD(AppConstants.CURRENCY_LYD, R.string.currency_lyd, R.string.symbol_lyd)
}

/**
 * قائمة العملات المتاحة
 */
object CurrencyList {
    val currencies = Currency.values().toList()

    /**
     * الحصول على العملة من الكود
     */
    fun getCurrencyByCode(code: String): Currency {
        return currencies.find { it.code == code } ?: Currency.EGP
    }
}