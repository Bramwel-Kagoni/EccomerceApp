package com.bramwel.eccomerceapp.features.cart

import com.bramwel.eccomerceapp.features.cart.domain.model.CartItem
import com.bramwel.eccomerceapp.features.cart.domain.usecase.CalculateCartSummaryUseCase
import com.bramwel.eccomerceapp.features.testProduct
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateCartSummaryUseCaseTest {

    private val calculate = CalculateCartSummaryUseCase()

    @Test
    fun `sums totals, savings and free delivery progress`() {
        val summary = calculate(
            listOf(
                CartItem(testProduct(1, price = 1_000, original = 1_200), 2),
                CartItem(testProduct(2, price = 500), 1)
            )
        )
        assertEquals(3, summary.itemCount)
        assertEquals(2_500L, summary.subtotal)
        assertEquals(400L, summary.savings)
        assertEquals(2_500L, summary.amountToFreeDelivery)
        assertEquals(0.5f, summary.freeDeliveryProgress, 0.001f)
    }

    @Test
    fun `free delivery unlocked above threshold`() {
        val summary = calculate(listOf(CartItem(testProduct(1, price = 6_000), 1)))
        assertEquals(0L, summary.amountToFreeDelivery)
        assertEquals(1f, summary.freeDeliveryProgress, 0.001f)
    }
}
