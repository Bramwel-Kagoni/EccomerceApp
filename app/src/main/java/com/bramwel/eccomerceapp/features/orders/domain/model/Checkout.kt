package com.bramwel.eccomerceapp.features.orders.domain.model

data class CartLine(
    val productId: Int,
    val quantity: Int
)

data class DeliveryDetails(
    val name: String,
    val phone: String,
    val email: String,
    val address: String,
    val city: String,
    val notes: String
)

/** Server-computed totals. The app never decides the final amount on its own. */
data class OrderQuote(
    val items: List<OrderItem>,
    val subtotal: Long,
    val deliveryFee: Long,
    val discount: Long,
    val total: Long,
    val promoCode: String,
    val promoValid: Boolean,
    val promoMessage: String,
    val freeDeliveryThreshold: Long
)
