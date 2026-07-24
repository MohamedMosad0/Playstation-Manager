package com.mohamed.playstation.core.localization

import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale
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
        val resolvedTag = if (languageTag == "ar") "ar-EG" else languageTag

        val targetLocaleList = if (resolvedTag == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(resolvedTag)
        }

        val currentLocales = AppCompatDelegate.getApplicationLocales()
        if (currentLocales.toLanguageTags() == targetLocaleList.toLanguageTags()) {
            return
        }

        Locale.setDefault(
            if (resolvedTag == "system") {
                Resources.getSystem().configuration.locales[0]
            } else {
                Locale.forLanguageTag(resolvedTag)
            }
        )
        AppCompatDelegate.setApplicationLocales(targetLocaleList)
    }
}
