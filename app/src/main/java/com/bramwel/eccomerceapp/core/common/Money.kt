package com.bramwel.eccomerceapp.core.common

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** All money in the app is whole Kenyan shillings stored as Long (M-Pesa only accepts integers). */
object Money {

    private val rate = BigDecimal(Constants.USD_TO_KES_RATE)

    /** Must stay identical to the backend's usd_to_kes (HALF_UP to whole shillings). */
    fun usdToKes(usd: Double): Long =
        BigDecimal(usd.toString()).multiply(rate).setScale(0, RoundingMode.HALF_UP).toLong()

    /** DummyJSON "price" is the discounted price; derive the original price for display. */
    fun originalKes(usdPrice: Double, discountPercent: Double): Long {
        if (discountPercent <= 0.0 || discountPercent >= 100.0) return usdToKes(usdPrice)
        return usdToKes(usdPrice / (1 - discountPercent / 100.0))
    }
}

private val kesFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.US)

fun Long.formatKes(): String = "KSh " + kesFormat.format(this)

fun Int.formatKes(): String = toLong().formatKes()
