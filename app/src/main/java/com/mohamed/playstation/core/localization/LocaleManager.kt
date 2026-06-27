package com.mohamed.playstation.core.localization

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مسؤول عن تطبيق اللغة على مستوى التطبيق باستخدام AppCompatDelegate.
 * يضمن تطبيق اللغة فعلياً على الـ UI وليس فقط حفظها في DataStore.
 */
@Singleton
class LocaleManager @Inject constructor() {

    /**
     * يطبّق اللغة المحددة على التطبيق.
     * @param languageTag رمز اللغة (مثل "ar", "en") أو "system" لاستخدام لغة النظام.
     */
    fun applyLanguage(languageTag: String) {
        val localeList = if (languageTag == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
