package com.mohamed.playstation.core.utils

import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

object DateUtils {

    @Volatile
    internal var dayRolloverFlowOverride: Flow<Long>? = null

    @Volatile
    internal var currentTimeMillisProvider: (() -> Long)? = null

    internal fun currentTimeMillis(): Long =
        currentTimeMillisProvider?.invoke() ?: System.currentTimeMillis()

    fun dayRolloverFlow(
        timeSource: () -> Long = { currentTimeMillis() },
        delayProvider: suspend (Long) -> Unit = { delay(it) }
    ): Flow<Long> = flow {
        val override = dayRolloverFlowOverride
        if (override != null) {
            emitAll(override)
            return@flow
        }
        while (true) {
            val now = timeSource()
            emit(now)
            val calendar = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val delayMs = maxOf(1000L, calendar.timeInMillis - now + 50L)
            delayProvider(delayMs)
        }
    }

    fun todayRange(now: Long = currentTimeMillis()): Pair<Long, Long> {
        return Pair(startOfDay(now), endExclusive(now))
    }

    fun last7DaysRange(now: Long = currentTimeMillis()): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return Pair(calendar.timeInMillis, endExclusive(now))
    }

    fun last30DaysRange(now: Long = currentTimeMillis()): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        calendar.add(Calendar.DAY_OF_YEAR, -29)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return Pair(calendar.timeInMillis, endExclusive(now))
    }

    fun thisMonthRange(now: Long = currentTimeMillis()): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return Pair(calendar.timeInMillis, endExclusive(now))
    }

    fun thisWeekRange(now: Long = currentTimeMillis()): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        calendar.firstDayOfWeek = Calendar.SUNDAY // Or use default depending on locale
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return Pair(calendar.timeInMillis, endExclusive(now))
    }

    fun lastMonthRange(now: Long = currentTimeMillis()): Pair<Long, Long> {
        val startCal = Calendar.getInstance().apply { timeInMillis = now }
        startCal.add(Calendar.MONTH, -1)
        startCal.set(Calendar.DAY_OF_MONTH, 1)
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)

        val endCal = Calendar.getInstance().apply { timeInMillis = now }
        endCal.set(Calendar.DAY_OF_MONTH, 1) // First day of this month
        endCal.set(Calendar.HOUR_OF_DAY, 0)
        endCal.set(Calendar.MINUTE, 0)
        endCal.set(Calendar.SECOND, 0)
        endCal.set(Calendar.MILLISECOND, 0)
        return Pair(startCal.timeInMillis, endCal.timeInMillis) // end exclusive
    }

    fun last3MonthsRange(now: Long = currentTimeMillis()): Pair<Long, Long> {
        val startCal = Calendar.getInstance().apply { timeInMillis = now }
        startCal.add(Calendar.MONTH, -3) // Exactly 3 months ago from today? Or from beginning of that month?
        // Let's do exactly 3 months ago (rolling)
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)
        return Pair(startCal.timeInMillis, endExclusive(now))
    }

    fun startOfDay(now: Long = currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun endExclusive(now: Long = currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
