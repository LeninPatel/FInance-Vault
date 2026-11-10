package com.example.domain.money

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/**
 * Exclusive Indian Rupee (INR ₹) monetary precision utility.
 * All monetary amounts in Finance Vault are stored and processed as integer paise (Long).
 * Example: ₹1,250.50 = 125050L paise.
 */
object InrMoney {
    const val CURRENCY_CODE = "INR"
    const val CURRENCY_SYMBOL = "₹"
    const val PAISE_PER_RUPEE = 100L

    fun rupeesToPaise(rupees: Long): Long = Math.multiplyExact(rupees, PAISE_PER_RUPEE)

    fun rupeesToPaise(rupees: Double): Long {
        return BigDecimal.valueOf(rupees)
            .multiply(BigDecimal.valueOf(PAISE_PER_RUPEE))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    /**
     * Parses a user-entered INR string (e.g., "1,250.50" or "50000") into paise.
     * Returns null if blank or invalid.
     */
    fun parseInrToPaise(input: String): Long? {
        val cleaned = input.replace(CURRENCY_SYMBOL, "")
            .replace(",", "")
            .replace(" ", "")
            .trim()
        if (cleaned.isEmpty()) return null
        return try {
            val bd = BigDecimal(cleaned)
            bd.multiply(BigDecimal.valueOf(PAISE_PER_RUPEE))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        } catch (e: Exception) {
            null
        }
    }

    fun add(aPaise: Long, bPaise: Long): Long = Math.addExact(aPaise, bPaise)

    fun subtract(aPaise: Long, bPaise: Long): Long = Math.subtractExact(aPaise, bPaise)

    fun multiply(paise: Long, factor: Double): Long {
        return BigDecimal.valueOf(paise)
            .multiply(BigDecimal.valueOf(factor))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    fun divide(paise: Long, divisor: Double): Long {
        if (divisor == 0.0) return 0L
        return BigDecimal.valueOf(paise)
            .divide(BigDecimal.valueOf(divisor), 0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    /**
     * Calculates percentage of an amount in paise.
     * E.g., percentage(100000L, 18.0) -> 18000L paise
     */
    fun percentageOf(paise: Long, percent: Double): Long {
        return BigDecimal.valueOf(paise)
            .multiply(BigDecimal.valueOf(percent))
            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    /**
     * Calculates ratio (numerator / denominator) * 100 rounded to 2 decimal places.
     * Safely returns 0.0 when denominator is 0.
     */
    fun percentageRatio(numeratorPaise: Long, denominatorPaise: Long): Double {
        if (denominatorPaise == 0L) return 0.0
        return BigDecimal.valueOf(numeratorPaise)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(denominatorPaise), 2, RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Formats integer paise using the Indian numbering system:
     * ₹1,000 | ₹10,000 | ₹1,00,000 | ₹10,00,000 | ₹1,00,00,000
     * Shows decimals (.50) only if non-zero paise exist or showZeroPaise is true.
     */
    fun formatPaise(
        paise: Long,
        showZeroPaise: Boolean = false,
        includeSymbol: Boolean = true,
        isHidden: Boolean = false
    ): String {
        if (isHidden) {
            return if (includeSymbol) "₹ ••••••" else "••••••"
        }
        val isNegative = paise < 0
        val absPaise = abs(paise)
        val wholeRupees = absPaise / PAISE_PER_RUPEE
        val remainderPaise = absPaise % PAISE_PER_RUPEE

        val formattedWhole = formatIndianInteger(wholeRupees)
        val decimalPart = if (remainderPaise > 0L || showZeroPaise) {
            "." + remainderPaise.toString().padStart(2, '0')
        } else {
            ""
        }

        val prefix = buildString {
            if (isNegative) append("-")
            if (includeSymbol) append(CURRENCY_SYMBOL)
        }
        return "$prefix$formattedWhole$decimalPart"
    }

    /**
     * Formats optional/nullable monetary values strictly according to the missing information policy:
     * Never display ₹0 when the actual amount is unknown / not recorded.
     */
    fun formatOptionalPaise(
        paise: Long?,
        isRecorded: Boolean = paise != null,
        missingLabel: String = "Not recorded",
        isHidden: Boolean = false
    ): String {
        if (!isRecorded || paise == null) return missingLabel
        return formatPaise(paise, isHidden = isHidden)
    }

    /**
     * Formats large amounts with Lakh / Crore suffix alongside or for compact badges.
     */
    fun formatWithLakhCroreHint(paise: Long, isHidden: Boolean = false): String {
        if (isHidden) return "₹ ••••••"
        val exact = formatPaise(paise)
        val absRupees = abs(paise) / 100.0
        val sign = if (paise < 0) "-" else ""
        return when {
            absRupees >= 1_00_00_000 -> {
                val cr = BigDecimal.valueOf(absRupees / 1_00_00_000.0)
                    .setScale(2, RoundingMode.HALF_UP)
                "$exact ($sign₹$cr Cr)"
            }
            absRupees >= 1_00_000 -> {
                val lakh = BigDecimal.valueOf(absRupees / 1_00_000.0)
                    .setScale(2, RoundingMode.HALF_UP)
                "$exact ($sign₹$lakh Lakh)"
            }
            else -> exact
        }
    }

    fun formatIndianInteger(number: Long): String {
        return formatIndianIntegerString(abs(number).toString())
    }

    fun formatIndianIntegerString(digitsOnly: String): String {
        val str = digitsOnly.filter { it.isDigit() }
        if (str.length <= 3) return str
        val lastThree = str.substring(str.length - 3)
        val remaining = str.substring(0, str.length - 3)
        val groupedRemaining = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            groupedRemaining.append(remaining[i])
            count++
            if (count == 2 && i != 0) {
                groupedRemaining.append(',')
                count = 0
            }
        }
        return groupedRemaining.reverse().toString() + "," + lastThree
    }

    /**
     * Sanitizes raw user input to digits and at most one decimal point with up to 2 decimal places.
     */
    fun sanitizeIndianNumericInput(raw: String): String {
        val filtered = StringBuilder()
        var hasDot = false
        var decimalDigits = 0
        for (ch in raw) {
            if (ch.isDigit()) {
                if (hasDot) {
                    if (decimalDigits < 2) {
                        filtered.append(ch)
                        decimalDigits++
                    }
                } else {
                    filtered.append(ch)
                }
            } else if (ch == '.' && !hasDot) {
                hasDot = true
                filtered.append(ch)
            }
        }
        val clean = filtered.toString()
        if (clean.isEmpty()) return ""
        val parts = clean.split('.', limit = 2)
        val rawInt = parts[0].trimStart('0')
        val intPart = when {
            parts[0].isEmpty() -> if (hasDot) "0" else ""
            rawInt.isEmpty() -> "0"
            else -> rawInt.take(13)
        }
        return if (hasDot) {
            "$intPart.${parts.getOrElse(1) { "" }}"
        } else {
            intPart
        }
    }

    /**
     * Formats a user-typed INR string with Indian numbering system commas:
     * first 3 digits from right, then every 2 digits (e.g., 1,23,456.78).
     */
    fun formatIndianInput(raw: String): String {
        val clean = sanitizeIndianNumericInput(raw)
        if (clean.isEmpty()) return ""
        val hasDot = clean.contains('.')
        val parts = clean.split('.', limit = 2)
        val formattedInt = if (parts[0].isEmpty()) "" else formatIndianIntegerString(parts[0])
        return if (hasDot) {
            "$formattedInt.${parts.getOrElse(1) { "" }}"
        } else {
            formattedInt
        }
    }

    fun paiseToPlainRupeeInput(paise: Long): String {
        val whole = paise / 100
        val rem = abs(paise % 100)
        return if (rem == 0L) whole.toString() else "$whole.${rem.toString().padStart(2, '0')}"
    }
}
