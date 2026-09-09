package org.hoods.pocket.domain.util

import kotlin.time.Clock
import kotlin.time.Instant

object DateUtils {

    private val MONTH_SHORT_NAMES = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    fun formatDate(instant: Instant, now: Instant = Clock.System.now()): String {
        val dateString = instant.toString().substringBefore('T')
        val parts = dateString.split('-')
        if (parts.size < 3) return dateString
        val year = parts[0].toIntOrNull() ?: return dateString
        val month = parts[1].toIntOrNull() ?: return dateString
        val day = parts[2].toIntOrNull() ?: return dateString

        val nowDateString = now.toString().substringBefore('T')
        val monthLabel = MONTH_SHORT_NAMES.getOrNull(month - 1) ?: return dateString

        return if (dateString == nowDateString) {
            "Today, $monthLabel $day, $year"
        } else {
            "$monthLabel $day, $year"
        }
    }

    fun formatShortDate(instant: Instant): String {
        val dateString = instant.toString().substringBefore('T')
        val parts = dateString.split('-')
        if (parts.size < 3) return dateString
        val year = parts[0].toIntOrNull() ?: return dateString
        val month = parts[1].toIntOrNull() ?: return dateString
        val day = parts[2].toIntOrNull() ?: return dateString

        val monthLabel = MONTH_SHORT_NAMES.getOrNull(month - 1) ?: return dateString
        return "$monthLabel $day, $year"
    }
}
