package com.winnertecnologia.fxrates

import com.winnertecnologia.fxrates.data.Currencies
import com.winnertecnologia.fxrates.data.FrankfurterApi
import org.junit.Assert.assertEquals
import org.junit.Test

class FrankfurterApiTest {

    @Test
    fun parsesV2RateRows() {
        val body = """
            [
              {"date":"2026-11-25","base":"USD","quote":"USD","rate":1},
              {"date":"2026-11-25","base":"USD","quote":"BRL","rate":5.4321},
              {"date":"2026-11-25","base":"USD","quote":"EUR","rate":0.9123}
            ]
        """.trimIndent()

        val rows = FrankfurterApi.parseRows(body)

        assertEquals(3, rows.size)
        assertEquals("BRL", rows[1].quote)
        assertEquals(5.4321, rows[1].rate, 0.00001)
    }

    @Test
    fun buildsFlagFromCurrencyCode() {
        assertEquals("🇧🇷", Currencies.flag("BRL"))
        assertEquals("🇪🇺", Currencies.flag("EUR"))
    }
}
