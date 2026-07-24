package com.mohamed.playstation.core.utils

import android.content.Context
import com.mohamed.playstation.R
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Presentation formatting that follows the locale applied to this app's resources.
 *
 * Reading the locale from [Context] is deliberate: AppCompat can apply an app locale
 * that differs from the device's process-wide default locale.
 */
object AppFormatters {

    private data class LocaleDateFormatter(
        val locale: Locale,
        val formatter: SimpleDateFormat
    )

    private val chartDateFormatter = ThreadLocal<LocaleDateFormatter>()

    fun formatAmount(context: Context, amount: Double): String =
        decimalFormat(context, minimumFractionDigits = 2, maximumFractionDigits = 2).format(amount)

    fun formatEditableAmount(context: Context, amount: Double): String =
        decimalFormat(context, minimumFractionDigits = 0, maximumFractionDigits = 2).format(amount)

    fun formatInteger(context: Context, value: Number): String =
        decimalFormat(context, minimumFractionDigits = 0, maximumFractionDigits = 0).format(value)

    fun parseDecimal(value: CharSequence?): Double? =
        normalizeNumericInput(value).toDoubleOrNull()

    fun parseInteger(value: CharSequence?): Int? =
        normalizeNumericInput(value).toIntOrNull()

    fun formatTimerPart(context: Context, value: Long): String {
        val symbols = DecimalFormatSymbols(locale(context))
        return DecimalFormat("00", symbols).apply { isGroupingUsed = false }.format(value)
    }

    fun formatTimer(context: Context, hours: Long, minutes: Long, seconds: Long): String =
        "${formatTimerPart(context, hours)}:${formatTimerPart(context, minutes)}:${formatTimerPart(context, seconds)}"

    fun formatDate(context: Context, date: Date): String =
        formatDate(context, date, "dd/MM/yyyy")

    fun formatDateWithTime(context: Context, date: Date): String =
        formatDate(context, date, "dd/MM/yyyy hh:mm a")

    fun formatTime(context: Context, date: Date): String =
        formatDate(context, date, "hh:mm a")

    fun formatTwentyFourHourTime(context: Context, date: Date): String =
        formatDate(context, date, "HH:mm")

    fun formatLongDate(context: Context, date: Date): String =
        formatDate(context, date, "dd MMM yyyy")

    fun formatChartDay(date: Date): String {
        val locale = Locale.getDefault()
        val cached = chartDateFormatter.get()
        val formatter = if (cached?.locale == locale) {
            cached.formatter
        } else {
            SimpleDateFormat("MM/dd", locale).also {
                chartDateFormatter.set(LocaleDateFormatter(locale, it))
            }
        }
        return formatter.format(date)
    }

    fun formatDateTimeWithMonth(context: Context, date: Date): String =
        formatDate(context, date, "dd MMM yyyy \u2022 HH:mm")

    fun formatDuration(context: Context, totalMinutes: Long): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> {
                "${formatInteger(context, hours)} ${context.getString(R.string.hours)} " +
                    "${formatInteger(context, minutes)} ${context.getString(R.string.minutes)}"
            }
            hours > 0 -> "${formatInteger(context, hours)} ${context.getString(R.string.hours)}"
            else -> "${formatInteger(context, minutes)} ${context.getString(R.string.minutes)}"
        }
    }

    private fun formatDate(context: Context, date: Date, pattern: String): String =
        SimpleDateFormat(pattern, locale(context)).format(date)

    private fun decimalFormat(
        context: Context,
        minimumFractionDigits: Int,
        maximumFractionDigits: Int
    ): DecimalFormat = DecimalFormat("0", DecimalFormatSymbols(locale(context))).apply {
        this.minimumFractionDigits = minimumFractionDigits
        this.maximumFractionDigits = maximumFractionDigits
        isGroupingUsed = false
    }

    private fun locale(context: Context): Locale = context.resources.configuration.locales[0]

    private fun normalizeNumericInput(value: CharSequence?): String {
        val normalized = value?.toString().orEmpty().trim().map { character ->
            when (character) {
                in '\u0660'..'\u0669' -> '0' + (character - '\u0660')
                in '\u06F0'..'\u06F9' -> '0' + (character - '\u06F0')
                '\u066B', '\u060C' -> '.'
                '\u066C' -> '\u0000'
                else -> character
            }
        }.filter { it != '\u0000' }.joinToString("")

        return if (normalized.count { it == ',' } == 1 && '.' !in normalized) {
            normalized.replace(',', '.')
        } else {
            normalized
        }
    }
}
