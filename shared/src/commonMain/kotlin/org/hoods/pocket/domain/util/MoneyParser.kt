package org.hoods.pocket.domain.util

import kotlin.math.abs

object MoneyParser {

    /**
     * Parses a user-entered monetary amount string directly into Long integer cents.
     * Enforces integer math invariants; prohibits Double or Float conversion.
     * Rejects invalid characters, multiple dots, negative amounts, zero, and >2 decimal places.
     */
    fun parseAmountToCents(input: String): Long? {
        val trimmed = input.trim().removePrefix("$").trim()
        if (trimmed.isEmpty()) return null

        val parts = trimmed.split('.')
        if (parts.size > 2) return null

        val wholePart = parts[0]
        if (wholePart.isEmpty() && parts.size == 1) return null
        val whole = if (wholePart.isEmpty()) 0L else {
            if (!wholePart.all { it.isDigit() }) return null
            wholePart.toLongOrNull() ?: return null
        }

        val cents = if (parts.size == 2) {
            val fractional = parts[1]
            if (!fractional.all { it.isDigit() }) return null
            when (fractional.length) {
                0 -> 0L
                1 -> fractional.toLong() * 10L
                2 -> fractional.toLong()
                else -> return null
            }
        } else {
            0L
        }

        val totalCents = whole * 100L + cents
        return if (totalCents > 0L) totalCents else null
    }

    /**
     * Formats integer cents to a standard decimal string (e.g. 4550L -> "45.50").
     */
    fun formatCentsToAmount(cents: Long): String {
        val isNegative = cents < 0L
        val absCents = abs(cents)
        val whole = absCents / 100L
        val fraction = absCents % 100L
        val fractionStr = if (fraction < 10) "0$fraction" else "$fraction"
        val prefix = if (isNegative) "-" else ""
        return "$prefix$whole.$fractionStr"
    }
}
