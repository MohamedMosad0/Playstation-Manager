package com.mohamed.playstation.core.constants

object AppConstants {

    // Database
    const val DATABASE_NAME = "playstation_database"
    const val DATABASE_VERSION = 1

    // DataStore
    const val PREFERENCES_NAME = "playstation_preferences"

    // Preferences Keys — existing (kept for backward compatibility)
    const val KEY_DARK_MODE = "dark_mode"
    const val KEY_LANGUAGE = "language"
    const val KEY_CURRENCY = "currency"
    const val KEY_SINGLE_PRICE = "single_player_price"
    const val KEY_MULTI_PRICE = "multiplayer_price"
    const val KEY_ALERT_TIME = "alert_time_minutes"
    const val KEY_ALERT_SOUND = "alert_sound"

    // Preferences Keys — PS4 Pricing
    const val KEY_PS4_HOUR_PRICE = "ps4_hour_price"
    const val KEY_PS4_HALF_HOUR_PRICE = "ps4_half_hour_price"
    const val KEY_PS4_MULTI_EXTRA = "ps4_multiplayer_extra"

    // Preferences Keys — PS5 Pricing
    const val KEY_PS5_HOUR_PRICE = "ps5_hour_price"
    const val KEY_PS5_HALF_HOUR_PRICE = "ps5_half_hour_price"
    const val KEY_PS5_MULTI_EXTRA = "ps5_multiplayer_extra"

    // Preferences Keys — Session Defaults
    const val KEY_SESSION_MODE = "session_mode"
    const val KEY_DEFAULT_FIXED_MINUTES = "default_fixed_minutes"

    // Preferences Keys — Warning Settings
    const val KEY_WARNINGS_ENABLED = "warnings_enabled"
    const val KEY_WARNING_SOUND_ENABLED = "warning_sound_enabled"
    const val KEY_WARNING_NOTIFICATION_ENABLED = "warning_notification_enabled"
    const val KEY_WARNING_MINUTES = "warning_minutes"

    // Default Values — existing (kept for backward compatibility)
    const val DEFAULT_SINGLE_PRICE = 30.0  // EGP
    const val DEFAULT_MULTI_PRICE = 20.0   // EGP
    const val DEFAULT_ALERT_TIME = 5 // minutes
    const val DEFAULT_DARK_MODE = true
    const val DEFAULT_LANGUAGE = "ar"
    const val DEFAULT_CURRENCY = "EGP"

    // Default Values — PS4 Pricing
    const val DEFAULT_PS4_HOUR_PRICE = 30.0
    const val DEFAULT_PS4_HALF_HOUR_PRICE = 20.0
    const val DEFAULT_PS4_MULTI_EXTRA = 10.0

    // Default Values — PS5 Pricing
    const val DEFAULT_PS5_HOUR_PRICE = 50.0
    const val DEFAULT_PS5_HALF_HOUR_PRICE = 30.0
    const val DEFAULT_PS5_MULTI_EXTRA = 15.0

    // Default Values — Session Defaults
    const val DEFAULT_SESSION_MODE = "open"
    const val DEFAULT_FIXED_MINUTES = 60

    // Default Values — Warning Settings
    const val DEFAULT_WARNINGS_ENABLED = true
    const val DEFAULT_WARNING_SOUND_ENABLED = true
    const val DEFAULT_WARNING_NOTIFICATION_ENABLED = true
    const val DEFAULT_WARNING_MINUTES = 5

    // Session Modes
    const val SESSION_MODE_OPEN = "open"
    const val SESSION_MODE_FIXED = "fixed"

    // Currencies
    const val CURRENCY_EGP = "EGP"  // Egyptian Pound
    const val CURRENCY_SAR = "SAR"  // Saudi Riyal
    const val CURRENCY_AED = "AED"  // UAE Dirham
    const val CURRENCY_USD = "USD"  // US Dollar
    const val CURRENCY_EUR = "EUR"  // Euro
    const val CURRENCY_KWD = "KWD"  // Kuwaiti Dinar
    const val CURRENCY_BHD = "BHD"  // Bahraini Dinar
    const val CURRENCY_OMR = "OMR"  // Omani Rial
    const val CURRENCY_QAR = "QAR"  // Qatari Riyal
    const val CURRENCY_JOD = "JOD"  // Jordanian Dinar
    const val CURRENCY_IQD = "IQD"  // Iraqi Dinar
    const val CURRENCY_LBP = "LBP"  // Lebanese Pound
    const val CURRENCY_MAD = "MAD"  // Moroccan Dirham
    const val CURRENCY_TND = "TND"  // Tunisian Dinar
    const val CURRENCY_DZD = "DZD"  // Algerian Dinar
    const val CURRENCY_LYD = "LYD"  // Libyan Dinar

    // Notification
    const val NOTIFICATION_CHANNEL_ID = "playstation_notifications"
    const val NOTIFICATION_CHANNEL_NAME = "PlayStation Notifications"
    const val ONGOING_NOTIFICATION_CHANNEL_ID = "playstation_sessions_ongoing"
    const val ONGOING_NOTIFICATION_CHANNEL_NAME = "Active Sessions"
    const val SESSION_NOTIFICATION_ID = 1001
    const val FOREGROUND_SESSION_NOTIFICATION_ID = 2001
    const val NOTIFICATION_REQUEST_CODE_OPEN_APP = 1001
    const val NOTIFICATION_REQUEST_CODE_FOREGROUND = 2001

    // WorkManager
    const val WORK_TAG_SESSION_ALERT = "session_alert"

    // Date Format
    const val DATE_FORMAT = "dd/MM/yyyy"
    const val TIME_FORMAT = "HH:mm"
    const val DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm"

    // Backup
    const val BACKUP_FOLDER = "PlayStation/Backups"
    const val BACKUP_FILE_PREFIX = "playstation_backup_"
    const val BACKUP_FILE_EXTENSION = ".db"

    // Session Status
    const val SESSION_STATUS_ACTIVE = "active"
    const val SESSION_STATUS_PAUSED = "paused"
    const val SESSION_STATUS_ENDED = "ended"

    // Device Types
    const val DEVICE_PS4 = "PS4"
    const val DEVICE_PS5 = "PS5"
    const val DEVICE_PS4_STANDARD = "PS4 Standard"
    const val DEVICE_PS4_PRO = "PS4 Pro"
    const val DEVICE_PS5_STANDARD = "PS5 Standard"
    const val DEVICE_PS5_DIGITAL = "PS5 Digital"

    // Session Types
    const val SESSION_TYPE_SINGLE = "single"
    const val SESSION_TYPE_MULTI = "multi"
}