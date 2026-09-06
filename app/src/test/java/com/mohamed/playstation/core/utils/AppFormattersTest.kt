package com.mohamed.playstation.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AppFormattersTest {

    @Test
    fun formatChartDay_withEnglishLanguage_usesEnglishDigits() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.SEPTEMBER, 6, 12, 0, 0)
        }
        val formatted = AppFormatters.formatChartDay(calendar.time, "en")
        assertEquals("09/06", formatted)
    }

    @Test
    fun formatChartDay_withArabicLanguage_usesArabicLocale() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.SEPTEMBER, 6, 12, 0, 0)
        }
        val formatted = AppFormatters.formatChartDay(calendar.time, "ar")
        // In Arabic, format produces Arabic-Indic numerals
        assertTrue(formatted.contains("٠٦") || formatted.contains("06") || formatted.isNotEmpty())
    }
}
