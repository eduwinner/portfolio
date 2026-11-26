package com.winnertecnologia.fxrates.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.Currency
import java.util.Locale

/** One row returned by the Frankfurter v2 API (`GET /v2/rates`). */
@Serializable
data class RateRow(
    val date: String,
    val base: String,
    val quote: String,
    val rate: Double,
)

/** Latest rate of one currency against the selected base. */
data class Quote(val code: String, val rate: Double)

/** One point of a historical series, used by the chart. */
data class RatePoint(val date: LocalDate, val rate: Double)

object Currencies {
    /** Currencies shown in the list (the selected base is filtered out). */
    val tracked = listOf("USD", "BRL", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY", "MXN")

    /** Currencies the user can pick as base. */
    val bases = listOf("USD", "EUR", "BRL")

    fun name(code: String): String =
        runCatching { Currency.getInstance(code).getDisplayName(Locale.getDefault()) }
            .getOrDefault(code)

    /** Builds a flag emoji from the first two letters of the ISO code (EUR -> EU flag). */
    fun flag(code: String): String {
        if (code.length < 2) return ""
        val first = Character.codePointAt(code.uppercase(), 0) - 0x41 + 0x1F1E6
        val second = Character.codePointAt(code.uppercase(), 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(first)) + String(Character.toChars(second))
    }
}
