package com.winnertecnologia.fxrates.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate

interface RatesRepository {
    suspend fun latest(base: String, quotes: List<String>): List<Quote>
    suspend fun history(base: String, quote: String, days: Int): List<RatePoint>
}

/** Client for the free, key-less Frankfurter API (https://frankfurter.dev). */
class FrankfurterApi(
    private val endpoint: String = "https://api.frankfurter.dev/v2/rates",
) : RatesRepository {

    override suspend fun latest(base: String, quotes: List<String>): List<Quote> =
        parseRows(get(mapOf("base" to base, "quotes" to quotes.joinToString(","))))
            .filter { it.quote != base }
            .map { Quote(it.quote, it.rate) }
            .sortedBy { it.code }

    override suspend fun history(base: String, quote: String, days: Int): List<RatePoint> {
        val from = LocalDate.now().minusDays(days.toLong())
        return parseRows(get(mapOf("base" to base, "quotes" to quote, "from" to from.toString())))
            .filter { it.quote == quote }
            .map { RatePoint(LocalDate.parse(it.date), it.rate) }
            .sortedBy { it.date }
    }

    private suspend fun get(params: Map<String, String>): String = withContext(Dispatchers.IO) {
        val query = params.entries.joinToString("&") { (key, value) ->
            "$key=${URLEncoder.encode(value, "UTF-8")}"
        }
        val connection = URL("$endpoint?$query").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            val status = connection.responseCode
            if (status !in 200..299) throw IOException("The rates server answered with HTTP $status.")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parseRows(body: String): List<RateRow> = json.decodeFromString(body)
    }
}
