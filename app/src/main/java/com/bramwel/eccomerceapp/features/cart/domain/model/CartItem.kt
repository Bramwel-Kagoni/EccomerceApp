package com.bramwel.eccomerceapp.features.cart.domain.model

import com.bramwel.eccomerceapp.core.common.Constants
import com.bramwel.eccomerceapp.features.products.domain.model.Product

data class CartItem(
    val product: Product,
    val quantity: Int
) {
    val lineTotal: Long get() = product.price * quantity
    val lineSavings: Long get() = (product.originalPrice - product.price).coerceAtLeast(0) * quantity
    val maxQuantity: Int get() = minOf(product.stock, Constants.MAX_QUANTITY_PER_ITEM)
}

data class CartSummary(
    val items: List<CartItem> = emptyList(),
    val itemCount: Int = 0,
    val subtotal: Long = 0,
    val savings: Long = 0,
    val amountToFreeDelivery: Long = 0,
    /** 0f..1f progress towards free standard delivery. */
    val freeDeliveryProgress: Float = 0f
) {
    val isEmpty: Boolean get() = items.isEmpty()
}
