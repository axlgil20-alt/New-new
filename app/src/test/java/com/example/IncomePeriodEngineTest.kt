package com.example

import com.example.ui.util.IncomePeriodEngine
import com.example.ui.util.IncomeScheduleConfig
import com.example.ui.util.IncomeScheduleMode
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomePeriodEngineTest {
    private fun millis(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        clear(); set(year, month, day, 12, 0, 0)
    }.timeInMillis

    private fun day(value: Long): Int = Calendar.getInstance().apply { timeInMillis = value }.get(Calendar.DAY_OF_MONTH)
    private fun month(value: Long): Int = Calendar.getInstance().apply { timeInMillis = value }.get(Calendar.MONTH)

    @Test fun standardQuincenaDay14IsFirstAndDay15IsSecond() {
        val first = IncomePeriodEngine.resolveExpensePeriod(millis(2026, 9, 14))
        val second = IncomePeriodEngine.resolveExpensePeriod(millis(2026, 9, 15))
        assertEquals(1, first.sequence); assertEquals(2, second.sequence)
    }

    @Test fun lastDayIncomeFundsNextFirstPeriod() {
        val period = IncomePeriodEngine.resolveIncomePeriod(millis(2026, 9, 31))
        assertEquals(10, period.monthIndex)
        assertEquals(1, period.sequence)
        assertEquals(2026, period.year)
    }

    @Test fun february28And29HaveNoInvalidEndDate() {
        val nonLeap = IncomePeriodEngine.periodRange(2025, 1, 2)
        val leap = IncomePeriodEngine.periodRange(2024, 1, 2)
        assertEquals(28, day(nonLeap.second)); assertEquals(29, day(leap.second))
    }

    @Test fun thirtyAndThirtyOneDayMonthsCloseOnActualLastDay() {
        assertEquals(30, day(IncomePeriodEngine.periodRange(2026, 3, 2).second))
        assertEquals(31, day(IncomePeriodEngine.periodRange(2026, 9, 2).second))
    }

    @Test fun standardPeriodsHaveNoGapOrOverlap() {
        val q1 = IncomePeriodEngine.periodRange(2026, 9, 1)
        val q2 = IncomePeriodEngine.periodRange(2026, 9, 2)
        assertEquals(14, day(q1.second)); assertEquals(15, day(q2.first))
        assertTrue(q1.second < q2.first)
    }

    @Test fun customDatesSevenAndTwentyTwoCoverConfiguredWindows() {
        val config = IncomeScheduleConfig(IncomeScheduleMode.CUSTOM_DATES, listOf(7, 22), "custom-7-22")
        val p1 = IncomePeriodEngine.resolveExpensePeriod(millis(2026, 9, 7), config)
        val p2 = IncomePeriodEngine.resolveExpensePeriod(millis(2026, 9, 22), config)
        assertEquals(1, p1.sequence); assertEquals(2, p2.sequence)
        assertEquals(7, day(p1.startMillis)); assertEquals(21, day(p1.endMillis))
        val p2Range = IncomePeriodEngine.periodRange(2026, 9, 2, config)
        assertEquals(22, day(p2Range.first)); assertEquals(6, day(p2Range.second)); assertEquals(10, month(p2Range.second))
    }

    @Test fun weeklyModeUsesSevenDayWindows() {
        val config = IncomeScheduleConfig(mode = IncomeScheduleMode.WEEKLY, version = "weekly-v1")
        val period = IncomePeriodEngine.resolveExpensePeriod(millis(2026, 9, 9), config)
        assertTrue(period.endMillis - period.startMillis >= 6L * 24L * 60L * 60L * 1000L)
    }

    @Test fun monthlyModeKeepsAllDaysTogether() {
        val config = IncomeScheduleConfig(mode = IncomeScheduleMode.MONTHLY, version = "monthly-v1")
        val period = IncomePeriodEngine.resolveExpensePeriod(millis(2026, 1, 31), config)
        assertEquals(1, period.sequence); assertEquals(1, month(period.startMillis)); assertEquals(31, day(period.endMillis))
    }

    @Test fun irregularModeDoesNotInventASecondPeriod() {
        val first = millis(2026, 9, 7)
        val second = millis(2026, 9, 22)
        val config = IncomeScheduleConfig(mode = IncomeScheduleMode.IRREGULAR, version = "irregular-v1", irregularPaymentDatesMillis = listOf(first, second))
        val period = IncomePeriodEngine.resolveIncomePeriod(second, config = config)
        assertEquals(2, period.sequence); assertEquals("irregular-v1:IRREGULAR:1", period.key)
        assertTrue(period.startMillis == second)
    }

    @Test fun weekendAndHolidayPaymentCanShiftBackWithoutChangingExpenseDates() {
        val config = IncomeScheduleConfig(
            adjustWeekendPayments = true,
            holidayDates = setOf("2026-10-15"),
            version = "shifted-v1"
        )
        val period = IncomePeriodEngine.resolveIncomePeriod(millis(2026, 9, 15), config = config)
        assertEquals(2, period.sequence)
        assertEquals(millis(2026, 9, 15), period.officialPaymentDateMillis)
        assertTrue(period.receivedDateMillis!! < millis(2026, 9, 15))
    }

    @Test fun scheduleVersionIsPartOfPeriodIdentity() {
        val a = IncomePeriodEngine.resolveExpensePeriod(millis(2026, 9, 15), IncomeScheduleConfig(version = "v1"))
        val b = IncomePeriodEngine.resolveExpensePeriod(millis(2026, 9, 15), IncomeScheduleConfig(version = "v2"))
        assertTrue(a.key != b.key)
    }
}
