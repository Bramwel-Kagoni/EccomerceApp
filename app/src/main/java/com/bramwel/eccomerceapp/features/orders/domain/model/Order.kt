package com.bramwel.eccomerceapp.features.orders.domain.model

data class Order(
    val id: String,
    val orderNumber: String,
    val status: OrderStatus,
    val customerName: String,
    val phone: String,
    val email: String,
    val address: String,
    val city: String,
    val notes: String,
    val deliveryMethod: DeliveryMethod,
    val subtotal: Long,
    val deliveryFee: Long,
    val discount: Long,
    val total: Long,
    val promoCode: String,
    val createdAt: String,
    val paidAt: String?,
    val items: List<OrderItem>,
    val payment: PaymentInfo?
) {
    val itemCount: Int get() = items.sumOf { it.quantity }
    val canPay: Boolean get() = status == OrderStatus.PENDING_PAYMENT || status == OrderStatus.PAYMENT_FAILED
}

data class OrderItem(
    val productId: Int,
    val title: String,
    val thumbnail: String,
    val unitPrice: Long,
    val quantity: Int,
    val lineTotal: Long
)

data class PaymentInfo(
    val checkoutRequestId: String,
    val status: PaymentStatus,
    val phone: String,
    val amount: Long,
    val resultDescription: String,
    val mpesaReceiptNumber: String,
    val transactionDate: String
)

enum class OrderStatus(val label: String) {
    PENDING_PAYMENT("Awaiting payment"),
    PAID("Paid"),
    PAYMENT_FAILED("Payment failed"),
    CANCELLED("Cancelled"),
    UNKNOWN("Unknown");

    companion object {
        fun fromApi(value: String): OrderStatus = when (value) {
            "pending_payment" -> PENDING_PAYMENT
            "paid" -> PAID
            "payment_failed" -> PAYMENT_FAILED
            "cancelled" -> CANCELLED
            else -> UNKNOWN
        }
    }
}

enum class PaymentStatus {
    PENDING, SUCCESS, FAILED, CANCELLED, TIMEOUT, UNKNOWN;

    val isFinal: Boolean get() = this != PENDING && this != UNKNOWN

    companion object {
        fun fromApi(value: String): PaymentStatus = when (value) {
            "pending" -> PENDING
            "success" -> SUCCESS
            "failed" -> FAILED
            "cancelled" -> CANCELLED
            "timeout" -> TIMEOUT
            else -> UNKNOWN
        }
    }
}

enum class DeliveryMethod(val apiValue: String, val label: String, val description: String) {
    STANDARD("standard", "Standard delivery", "2–4 business days"),
    EXPRESS("express", "Express delivery", "Same or next day in Nairobi");

    companion object {
        fun fromApi(value: String): DeliveryMethod = entries.firstOrNull { it.apiValue == value } ?: STANDARD
    }
}
