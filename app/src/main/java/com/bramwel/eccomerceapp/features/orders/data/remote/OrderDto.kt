package com.bramwel.eccomerceapp.features.orders.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CartLineDto(
    @SerialName("product_id") val productId: Int,
    val quantity: Int
)

@Serializable
data class QuoteRequestDto(
    val items: List<CartLineDto>,
    @SerialName("delivery_method") val deliveryMethod: String,
    @SerialName("promo_code") val promoCode: String = ""
)

@Serializable
data class CreateOrderRequestDto(
    val items: List<CartLineDto>,
    @SerialName("delivery_method") val deliveryMethod: String,
    @SerialName("promo_code") val promoCode: String = "",
    @SerialName("customer_name") val customerName: String,
    val phone: String,
    val email: String = "",
    val address: String,
    val city: String,
    val notes: String = ""
)

@Serializable
data class QuoteDto(
    val items: List<OrderItemDto> = emptyList(),
    val subtotal: Long,
    @SerialName("delivery_fee") val deliveryFee: Long,
    val discount: Long = 0,
    val total: Long,
    @SerialName("promo_code") val promoCode: String = "",
    @SerialName("promo_valid") val promoValid: Boolean = false,
    @SerialName("promo_message") val promoMessage: String = "",
    @SerialName("free_delivery_threshold") val freeDeliveryThreshold: Long = 0
)

@Serializable
data class OrderDto(
    val id: String,
    @SerialName("order_number") val orderNumber: String,
    val status: String,
    @SerialName("customer_name") val customerName: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val notes: String = "",
    @SerialName("delivery_method") val deliveryMethod: String = "standard",
    val subtotal: Long = 0,
    @SerialName("delivery_fee") val deliveryFee: Long = 0,
    val discount: Long = 0,
    val total: Long = 0,
    @SerialName("promo_code") val promoCode: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("paid_at") val paidAt: String? = null,
    val items: List<OrderItemDto> = emptyList(),
    val payment: PaymentDto? = null
)

@Serializable
data class OrderItemDto(
    @SerialName("product_id") val productId: Int,
    val title: String = "",
    val thumbnail: String = "",
    @SerialName("unit_price") val unitPrice: Long = 0,
    val quantity: Int = 1,
    @SerialName("line_total") val lineTotal: Long = 0
)

@Serializable
data class PaymentDto(
    @SerialName("checkout_request_id") val checkoutRequestId: String,
    val status: String,
    val phone: String = "",
    val amount: Long = 0,
    @SerialName("result_code") val resultCode: String = "",
    @SerialName("result_desc") val resultDesc: String = "",
    @SerialName("mpesa_receipt_number") val mpesaReceiptNumber: String = "",
    @SerialName("transaction_date") val transactionDate: String = "",
    @SerialName("created_at") val createdAt: String? = null
)
