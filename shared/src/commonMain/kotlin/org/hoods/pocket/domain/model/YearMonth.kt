package org.hoods.pocket.domain.model

import kotlin.time.Clock
import kotlin.time.Instant

data class YearMonth(
    val year: Int,
    val month: Int
) : Comparable<YearMonth> {

    init {
        require(month in 1..12) { "Month must be between 1 and 12, got $month" }
    }

    fun previous(): YearMonth {
        return if (month == 1) {
            YearMonth(year - 1, 12)
        } else {
            YearMonth(year, month - 1)
        }
    }

    fun next(): YearMonth {
        return if (month == 12) {
            YearMonth(year + 1, 1)
        } else {
            YearMonth(year, month + 1)
        }
    }

    fun format(): String {
        return "${MONTH_NAMES[month - 1]} $year"
    }

    fun startInstant(): Instant {
        val y = year.toString().padStart(4, '0')
        val m = month.toString().padStart(2, '0')
        return Instant.parse("$y-$m-01T00:00:00Z")
    }

    fun endInstant(): Instant {
        return next().startInstant()
    }

    fun contains(instant: Instant): Boolean {
        return instant >= startInstant() && instant < endInstant()
    }

    override fun compareTo(other: YearMonth): Int {
        val yearDiff = this.year.compareTo(other.year)
        if (yearDiff != 0) return yearDiff
        return this.month.compareTo(other.month)
    }

    companion object {
        private val MONTH_NAMES = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        fun fromInstant(instant: Instant): YearMonth {
            val dateString = instant.toString().substringBefore('T')
            val parts = dateString.split('-')
            val y = parts[0].toInt()
            val m = parts[1].toInt()
            return YearMonth(y, m)
        }

        fun now(clock: Clock = Clock.System): YearMonth {
            return fromInstant(clock.now())
        }
    }
}
