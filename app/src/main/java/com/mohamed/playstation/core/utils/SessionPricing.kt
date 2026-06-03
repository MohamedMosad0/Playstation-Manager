package com.mohamed.playstation.core.utils

import com.mohamed.playstation.core.constants.AppConstants

/**
 * Price calculations using PS4/PS5 settings from DataStore.
 */
object SessionPricing {

    data class PricingSettings(
        val ps4HourPrice: Double,
        val ps4HalfHourPrice: Double,
        val ps4MultiExtra: Double,
        val ps5HourPrice: Double,
        val ps5HalfHourPrice: Double,
        val ps5MultiExtra: Double,
        val legacySinglePrice: Double,
        val legacyMultiPrice: Double
    )

    fun isPs5Device(deviceType: String): Boolean =
        deviceType.contains("PS5", ignoreCase = true)

    fun isPs4Device(deviceType: String): Boolean =
        deviceType.contains("PS4", ignoreCase = true)

    fun usesLegacyPricing(deviceType: String): Boolean =
        !isPs4Device(deviceType) && !isPs5Device(deviceType)

    fun hourPrice(settings: PricingSettings, isPs5: Boolean): Double =
        if (isPs5) settings.ps5HourPrice else settings.ps4HourPrice

    fun halfHourPrice(settings: PricingSettings, isPs5: Boolean): Double =
        if (isPs5) settings.ps5HalfHourPrice else settings.ps4HalfHourPrice

    fun multiExtra(settings: PricingSettings, isPs5: Boolean): Double =
        if (isPs5) settings.ps5MultiExtra else settings.ps4MultiExtra

    /**
     * Effective hourly rate for open sessions and receipt hourly math.
     */
    fun pricePerHour(
        settings: PricingSettings,
        deviceType: String,
        isMultiPlayer: Boolean
    ): Double {
        if (usesLegacyPricing(deviceType)) {
            return if (isMultiPlayer) settings.legacyMultiPrice else settings.legacySinglePrice
        }
        val isPs5 = isPs5Device(deviceType)
        val base = hourPrice(settings, isPs5)
        val extra = if (isMultiPlayer) multiExtra(settings, isPs5) else 0.0
        return base + extra
    }

    /**
     * Cost for a fixed-duration session based on actual time.
     */
    fun fixedPackagePrice(
        settings: PricingSettings,
        deviceType: String,
        isMultiPlayer: Boolean,
        durationMinutes: Int
    ): Double {
        val hourly = pricePerHour(settings, deviceType, isMultiPlayer)
        return hourly * (durationMinutes / 60.0)
    }

    /**
     * Live play cost for open sessions based on elapsed minutes.
     */
    fun openSessionCost(
        settings: PricingSettings,
        deviceType: String,
        isMultiPlayer: Boolean,
        elapsedMinutes: Long
    ): Double {
        val hourly = pricePerHour(settings, deviceType, isMultiPlayer)
        return hourly * (elapsedMinutes / 60.0)
    }
    fun previewAmount(
        sessionMode: String,
        settings: PricingSettings,
        deviceType: String,
        isMultiPlayer: Boolean,
        fixedDurationMinutes: Int?
    ): Double {
        return if (sessionMode == AppConstants.SESSION_MODE_FIXED) {
            val minutes = fixedDurationMinutes ?: AppConstants.DEFAULT_FIXED_MINUTES
            fixedPackagePrice(settings, deviceType, isMultiPlayer, minutes)
        } else {
            pricePerHour(settings, deviceType, isMultiPlayer)
        }
    }
}
