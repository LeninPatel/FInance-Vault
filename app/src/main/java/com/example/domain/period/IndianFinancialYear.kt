package com.example.domain.period

import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Indian Financial Year (1 April to 31 March) and accounting period utilities.
 */
object IndianFinancialYear {
    private val displayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val shortFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    private fun safeLocalDate(epochDay: Long): LocalDate {
        return if (epochDay <= -365_000L || epochDay >= 365_000L) {
            LocalDate.now()
        } else {
            runCatching { LocalDate.ofEpochDay(epochDay) }.getOrElse { LocalDate.now() }
        }
    }

    fun formatEpochDay(epochDay: Long): String {
        return safeLocalDate(epochDay).format(displayFormatter)
    }

    fun formatShortEpochDay(epochDay: Long): String {
        return safeLocalDate(epochDay).format(shortFormatter)
    }

    /**
     * Returns the Indian Financial Year label for a given epoch day, e.g. "FY 2026-27".
     */
    fun getFinancialYearLabel(epochDay: Long): String {
        val date = safeLocalDate(epochDay)
        val startYear = if (date.monthValue >= 4) date.year else date.year - 1
        val endYearShort = ((startYear + 1) % 100).toString().padStart(2, '0')
        return "FY $startYear-$endYearShort"
    }

    /**
     * Returns the Assessment Year corresponding to a Financial Year label.
     * E.g. "FY 2026-27" -> "AY 2027-28"
     */
    fun getAssessmentYearForFy(fyLabel: String): String {
        val cleaned = fyLabel.removePrefix("FY ").trim()
        val startYear = cleaned.substringBefore("-").toIntOrNull() ?: LocalDate.now().year
        val ayStart = startYear + 1
        val ayEndShort = ((ayStart + 1) % 100).toString().padStart(2, '0')
        return "AY $ayStart-$ayEndShort"
    }

    /**
     * Returns start and end epoch days (inclusive) for a Financial Year starting in `startYear`.
     * E.g. startYear = 2026 -> 1 April 2026 to 31 March 2027.
     */
    fun getFinancialYearRange(startYear: Int): Pair<Long, Long> {
        val start = LocalDate.of(startYear, Month.APRIL, 1).toEpochDay()
        val end = LocalDate.of(startYear + 1, Month.MARCH, 31).toEpochDay()
        return start to end
    }

    fun getRangeForFyLabel(fyLabel: String): Pair<Long, Long> {
        val cleaned = fyLabel.removePrefix("FY ").trim()
        val startYear = cleaned.substringBefore("-").toIntOrNull() ?: currentFyStartYear()
        return getFinancialYearRange(startYear)
    }

    fun currentFyStartYear(): Int {
        val now = LocalDate.now()
        return if (now.monthValue >= 4) now.year else now.year - 1
    }

    fun currentFyLabel(): String = getFinancialYearLabel(todayEpochDay())

    fun availableFinancialYears(): List<String> {
        val currentStart = currentFyStartYear()
        return (currentStart + 1 downTo currentStart - 4).map { start ->
            val endShort = ((start + 1) % 100).toString().padStart(2, '0')
            "FY $start-$endShort"
        }
    }

    fun currentMonthRange(referenceEpochDay: Long = todayEpochDay()): Pair<Long, Long> {
        val date = LocalDate.ofEpochDay(referenceEpochDay)
        val start = date.withDayOfMonth(1).toEpochDay()
        val end = date.withDayOfMonth(date.lengthOfMonth()).toEpochDay()
        return start to end
    }

    fun parseDateInputOrToday(input: String): Long {
        return parseFlexibleDateInput(input) ?: todayEpochDay()
    }

    fun parseFlexibleDateInput(input: String): Long? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val patterns = listOf(
            shortFormatter,
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            displayFormatter,
            DateTimeFormatter.ISO_LOCAL_DATE
        )
        for (fmt in patterns) {
            runCatching {
                return LocalDate.parse(trimmed, fmt).toEpochDay()
            }
        }
        return null
    }

    fun addMonthsToEpochDay(epochDay: Long, months: Long): Long {
        return safeLocalDate(epochDay).plusMonths(months).toEpochDay()
    }

    fun addDaysToEpochDay(epochDay: Long, days: Long): Long {
        return safeLocalDate(epochDay).plusDays(days).toEpochDay()
    }

    fun advanceSubscriptionRenewalEpochDay(
        fromEpochDay: Long,
        frequency: com.example.data.local.BillingFrequency,
        customIntervalCount: Int = 1,
        customIntervalUnit: String = "MONTHS"
    ): Long {
        val base = safeLocalDate(fromEpochDay)
        return when (frequency) {
            com.example.data.local.BillingFrequency.MONTHLY -> base.plusMonths(1).toEpochDay()
            com.example.data.local.BillingFrequency.BIMONTHLY -> base.plusMonths(2).toEpochDay()
            com.example.data.local.BillingFrequency.QUARTERLY -> base.plusMonths(3).toEpochDay()
            com.example.data.local.BillingFrequency.FOUR_MONTHLY -> base.plusMonths(4).toEpochDay()
            com.example.data.local.BillingFrequency.HALF_YEARLY -> base.plusMonths(6).toEpochDay()
            com.example.data.local.BillingFrequency.ANNUALLY -> base.plusMonths(12).toEpochDay()
            com.example.data.local.BillingFrequency.CUSTOM -> {
                val count = customIntervalCount.coerceAtLeast(1).toLong()
                if (customIntervalUnit.equals("DAYS", ignoreCase = true)) {
                    base.plusDays(count).toEpochDay()
                } else {
                    base.plusMonths(count).toEpochDay()
                }
            }
        }
    }
}
