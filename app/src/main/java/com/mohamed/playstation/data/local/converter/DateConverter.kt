package com.mohamed.playstation.data.local.converter

import androidx.room.TypeConverter
import java.util.Date

/**
 * محول التواريخ لـ Room Database
 * يحول Date إلى Long والعكس
 */
class DateConverter {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}