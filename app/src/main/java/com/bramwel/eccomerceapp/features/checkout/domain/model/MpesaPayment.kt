package com.bramwel.eccomerceapp.features.checkout.domain.model

import com.bramwel.eccomerceapp.features.orders.domain.model.PaymentStatus

data class StkPushRequest(
    val checkoutRequestId: String,
    val customerMessage: String,
    val amount: Long,
    val phone: String
)

data class PaymentStatusUpdate(
    val checkoutRequestId: String,
    val status: PaymentStatus,
    val message: String,
    val mpesaReceiptNumber: String
)

/** What the payment screen renders while an STK push is in flight. */
sealed interface PaymentProgress {
    data object SendingPrompt : PaymentProgress
    data class AwaitingPin(val message: String, val amount: Long, val phone: String) : PaymentProgress
    data class Paid(val orderId: String, val mpesaReceiptNumber: String) : PaymentProgress
    data class Failed(val reason: String, val canRetry: Boolean = true) : PaymentProgress

    /** No final answer within the timeout: the user may still have paid, so offer "check again". */
    data class Unconfirmed(val checkoutRequestId: String) : PaymentProgress
}
