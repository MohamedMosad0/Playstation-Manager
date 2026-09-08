package com.mohamed.playstation.core.utils

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class DateUtilsTest {

    @Before
    fun setUp() {
        DateUtils.dayRolloverFlowOverride = null
        DateUtils.currentTimeMillisProvider = null
    }

    @After
    fun tearDown() {
        DateUtils.dayRolloverFlowOverride = null
        DateUtils.currentTimeMillisProvider = null
    }

    @Test
    fun dayRolloverFlow_emitsInitialTimestampImmediately() = runTest {
        val fixedTime = calendarAt(2026, Calendar.SEPTEMBER, 8, 14, 30, 0).timeInMillis

        DateUtils.dayRolloverFlow(timeSource = { fixedTime }).test {
            val first = awaitItem()
            assertEquals(fixedTime, first)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dayRolloverFlow_calculatesNextMidnightDelayAndEmitsNextDay() = runTest {
        // 2026-09-08 23:59:50 -> next midnight is 2026-09-09 00:00:00
        val day1Time = calendarAt(2026, Calendar.SEPTEMBER, 8, 23, 59, 50).timeInMillis
        val day2Time = calendarAt(2026, Calendar.SEPTEMBER, 9, 0, 0, 1).timeInMillis

        var clockCalls = 0
        var recordedDelayMs = -1L

        val flow = DateUtils.dayRolloverFlow(
            timeSource = {
                clockCalls++
                if (clockCalls == 1) day1Time else day2Time
            },
            delayProvider = { delayMs ->
                recordedDelayMs = delayMs
            }
        )

        flow.take(2).test {
            val first = awaitItem()
            assertEquals(day1Time, first)

            val second = awaitItem()
            assertEquals(day2Time, second)

            // Expected delay: 10 seconds + 50ms = 10050ms
            assertEquals(10_050L, recordedDelayMs)

            awaitComplete()
        }
    }

    @Test
    fun todayRange_updatesBoundariesWhenDateChanges() {
        val day1 = calendarAt(2026, Calendar.SEPTEMBER, 8, 15, 0, 0).timeInMillis
        val day2 = calendarAt(2026, Calendar.SEPTEMBER, 9, 10, 0, 0).timeInMillis

        val (start1, end1) = DateUtils.todayRange(day1)
        val (start2, end2) = DateUtils.todayRange(day2)

        val expectedStart1 = calendarAt(2026, Calendar.SEPTEMBER, 8, 0, 0, 0).timeInMillis
        val expectedEnd1 = calendarAt(2026, Calendar.SEPTEMBER, 9, 0, 0, 0).timeInMillis

        val expectedStart2 = calendarAt(2026, Calendar.SEPTEMBER, 9, 0, 0, 0).timeInMillis
        val expectedEnd2 = calendarAt(2026, Calendar.SEPTEMBER, 10, 0, 0, 0).timeInMillis

        assertEquals(expectedStart1, start1)
        assertEquals(expectedEnd1, end1)
        assertEquals(expectedStart2, start2)
        assertEquals(expectedEnd2, end2)
        assertTrue(start2 > start1)
    }

    @Test
    fun last7DaysRange_updatesBoundariesWhenDateChanges() {
        val day1 = calendarAt(2026, Calendar.SEPTEMBER, 8, 15, 0, 0).timeInMillis
        val day2 = calendarAt(2026, Calendar.SEPTEMBER, 9, 10, 0, 0).timeInMillis

        val (start1, end1) = DateUtils.last7DaysRange(day1)
        val (start2, end2) = DateUtils.last7DaysRange(day2)

        val expectedStart1 = calendarAt(2026, Calendar.SEPTEMBER, 2, 0, 0, 0).timeInMillis
        val expectedEnd1 = calendarAt(2026, Calendar.SEPTEMBER, 9, 0, 0, 0).timeInMillis

        val expectedStart2 = calendarAt(2026, Calendar.SEPTEMBER, 3, 0, 0, 0).timeInMillis
        val expectedEnd2 = calendarAt(2026, Calendar.SEPTEMBER, 10, 0, 0, 0).timeInMillis

        assertEquals(expectedStart1, start1)
        assertEquals(expectedEnd1, end1)
        assertEquals(expectedStart2, start2)
        assertEquals(expectedEnd2, end2)
    }

    private fun calendarAt(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Calendar {
        return Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute, second)
        }
    }
}
