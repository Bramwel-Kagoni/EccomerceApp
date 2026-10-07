package com.bramwel.eccomerceapp.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

/** These values must match backend/shop/tests.py so app and server agree on prices. */
class MoneyTest {

    @Test
    fun `converts usd to whole shillings rounding half up`() {
        assertEquals(1289L, Money.usdToKes(9.99))
        assertEquals(65L, Money.usdToKes(0.5))
    }

    @Test
    fun `original price is never below the selling price`() {
        val price = Money.usdToKes(9.99)
        val original = Money.originalKes(9.99, 10.0)
        assert(original >= price)
        assertEquals(price, Money.originalKes(9.99, 0.0))
    }

    @Test
    fun `formats kes`() {
        assertEquals("KSh 12,450", 12_450L.formatKes())
    }
}
