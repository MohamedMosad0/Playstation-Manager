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
     * @param languageTag رمز اللغة المدعوم ("ar" أو "en").
     */
    fun applyLanguage(languageTag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }
}
