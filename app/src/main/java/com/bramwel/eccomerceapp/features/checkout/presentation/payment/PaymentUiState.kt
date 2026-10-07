package com.bramwel.eccomerceapp.features.checkout.presentation.payment

import com.bramwel.eccomerceapp.features.checkout.domain.model.PaymentProgress

data class PaymentUiState(
    val orderNumber: String = "",
    val total: Long = 0,
    val phone: String = "",
    val phoneError: String? = null,
    /** null = idle (waiting for the user to press Pay). */
    val progress: PaymentProgress? = null
) {
    val isBusy: Boolean
        get() = progress is PaymentProgress.SendingPrompt || progress is PaymentProgress.AwaitingPin
}

sealed interface PaymentEvent {
    data class PhoneChanged(val phone: String) : PaymentEvent
    data object Pay : PaymentEvent
    data object CheckAgain : PaymentEvent
}

sealed interface PaymentEffect {
    data class ShowReceipt(val orderId: String) : PaymentEffect
}
