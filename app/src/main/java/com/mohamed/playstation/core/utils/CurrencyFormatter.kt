package com.mohamed.playstation.core.utils

import java.util.Locale

object CurrencyFormatter {
    /**
     * Formats an amount using the system locale for digit/separator handling,
     * but strictly appends the user-selected currency code.
     * Example: formatCurrency(50.0, "EGP") -> "50.00 EGP"
     */
    fun formatCurrency(amount: Double, currencyCode: String): String {
        val amountFormatted = String.format(Locale.getDefault(), "%.2f", amount)
        return "$amountFormatted $currencyCode"
    }
}
