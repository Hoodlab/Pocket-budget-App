package org.hoods.pocket.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class YearMonthTest {

    @Test
    fun constructor_validatesMonthRange() {
        assertFailsWith<IllegalArgumentException> {
            YearMonth(2026, 0)
        }
        assertFailsWith<IllegalArgumentException> {
            YearMonth(2026, 13)
        }
        val valid = YearMonth(2026, 9)
        assertEquals(2026, valid.year)
        assertEquals(9, valid.month)
    }

    @Test
    fun previous_decrementsMonthOrRollsOverToPreviousYear() {
        val sep = YearMonth(2026, 9)
        assertEquals(YearMonth(2026, 8), sep.previous())

        val jan = YearMonth(2026, 1)
        assertEquals(YearMonth(2025, 12), jan.previous())
    }

    @Test
    fun next_incrementsMonthOrRollsOverToNextYear() {
        val sep = YearMonth(2026, 9)
        assertEquals(YearMonth(2026, 10), sep.next())

        val dec = YearMonth(2026, 12)
        assertEquals(YearMonth(2027, 1), dec.next())
    }

    @Test
    fun format_returnsFullMonthNameAndYear() {
        assertEquals("September 2026", YearMonth(2026, 9).format())
        assertEquals("January 2025", YearMonth(2025, 1).format())
        assertEquals("December 2027", YearMonth(2027, 12).format())
    }

    @Test
    fun contains_correctlyMatchesInstantsWithinMonth() {
        val sep2026 = YearMonth(2026, 9)
        // 2026-09-01T00:00:00Z
        val startOfSep = Instant.parse("2026-09-01T00:00:00Z")
        // 2026-09-15T12:30:00Z
        val midSep = Instant.parse("2026-09-15T12:30:00Z")
        // 2026-09-30T23:59:59Z
        val endOfSep = Instant.parse("2026-09-30T23:59:59Z")
        // 2026-08-31T23:59:59Z
        val endOfAug = Instant.parse("2026-08-31T23:59:59Z")
        // 2026-10-01T00:00:00Z
        val startOfOct = Instant.parse("2026-10-01T00:00:00Z")

        assertTrue(sep2026.contains(startOfSep))
        assertTrue(sep2026.contains(midSep))
        assertTrue(sep2026.contains(endOfSep))
        assertFalse(sep2026.contains(endOfAug))
        assertFalse(sep2026.contains(startOfOct))
    }

    @Test
    fun fromInstant_extractsYearAndMonth() {
        val instant = Instant.parse("2026-08-22T10:15:30Z")
        assertEquals(YearMonth(2026, 8), YearMonth.fromInstant(instant))
    }

    @Test
    fun compareTo_ordersByYearThenMonth() {
        val aug2026 = YearMonth(2026, 8)
        val sep2026 = YearMonth(2026, 9)
        val jan2027 = YearMonth(2027, 1)

        assertTrue(aug2026 < sep2026)
        assertTrue(sep2026 < jan2027)
        assertTrue(aug2026 < jan2027)
    }

    @Test
    fun startInstant_and_endInstant_returnExactUtcMonthBoundaries() {
        val sep2026 = YearMonth(2026, 9)
        assertEquals(Instant.parse("2026-09-01T00:00:00Z"), sep2026.startInstant())
        assertEquals(Instant.parse("2026-10-01T00:00:00Z"), sep2026.endInstant())

        val dec2026 = YearMonth(2026, 12)
        assertEquals(Instant.parse("2026-12-01T00:00:00Z"), dec2026.startInstant())
        assertEquals(Instant.parse("2027-01-01T00:00:00Z"), dec2026.endInstant())

        val feb2024 = YearMonth(2024, 2)
        assertEquals(Instant.parse("2024-02-01T00:00:00Z"), feb2024.startInstant())
        assertEquals(Instant.parse("2024-03-01T00:00:00Z"), feb2024.endInstant())
    }
}
