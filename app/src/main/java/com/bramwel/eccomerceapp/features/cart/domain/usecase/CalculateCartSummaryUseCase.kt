package com.bramwel.eccomerceapp.features.cart.domain.usecase

import com.bramwel.eccomerceapp.core.common.Constants
import com.bramwel.eccomerceapp.features.cart.domain.model.CartItem
import com.bramwel.eccomerceapp.features.cart.domain.model.CartSummary
import javax.inject.Inject

/**
 * Local cart estimate. Delivery fees, promo codes and the final total are always
 * confirmed by the backend quote at checkout.
 */
class CalculateCartSummaryUseCase @Inject constructor() {

    operator fun invoke(items: List<CartItem>): CartSummary {
        val subtotal = items.sumOf { it.lineTotal }
        val threshold = Constants.FREE_DELIVERY_THRESHOLD
        return CartSummary(
            items = items,
            itemCount = items.sumOf { it.quantity },
            subtotal = subtotal,
            savings = items.sumOf { it.lineSavings },
            amountToFreeDelivery = (threshold - subtotal).coerceAtLeast(0),
            freeDeliveryProgress = if (threshold <= 0) 1f else (subtotal.toFloat() / threshold).coerceIn(0f, 1f)
        )
    }
}
