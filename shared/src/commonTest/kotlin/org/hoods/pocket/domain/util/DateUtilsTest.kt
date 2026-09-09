package org.hoods.pocket.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DateUtilsTest {

    @Test
    fun formatDate_returnsTodayPrefixWhenDateMatchesToday() {
        val now = Instant.parse("2026-09-09T14:30:00Z")
        val instant = Instant.parse("2026-09-09T08:00:00Z")

        val result = DateUtils.formatDate(instant, now)
        assertEquals("Today, Sep 9, 2026", result)
    }

    @Test
    fun formatDate_returnsFormattedDateWithoutTodayWhenDifferentDay() {
        val now = Instant.parse("2026-09-09T14:30:00Z")
        val instant = Instant.parse("2026-08-22T10:00:00Z")

        val result = DateUtils.formatDate(instant, now)
        assertEquals("Aug 22, 2026", result)
    }

    @Test
    fun formatShortDate_returnsMonthShortNameDayAndYear() {
        val instant = Instant.parse("2026-08-15T18:20:00Z")
        assertEquals("Aug 15, 2026", DateUtils.formatShortDate(instant))

        val janInstant = Instant.parse("2025-01-01T00:00:00Z")
        assertEquals("Jan 1, 2025", DateUtils.formatShortDate(janInstant))
    }
}
