package com.bramwel.eccomerceapp.features.checkout.domain.usecase

import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.core.common.Constants
import com.bramwel.eccomerceapp.core.common.MpesaPhone
import com.bramwel.eccomerceapp.core.common.toUserMessage
import com.bramwel.eccomerceapp.features.cart.domain.repository.CartRepository
import com.bramwel.eccomerceapp.features.checkout.domain.model.PaymentProgress
import com.bramwel.eccomerceapp.features.checkout.domain.repository.PaymentRepository
import com.bramwel.eccomerceapp.features.orders.domain.model.OrderStatus
import com.bramwel.eccomerceapp.features.orders.domain.model.PaymentStatus
import com.bramwel.eccomerceapp.features.orders.domain.repository.OrderRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Sends an M-Pesa STK push for an order and polls the backend until the payment is
 * confirmed, rejected or we give up waiting. On success the cart is emptied.
 */
class PayWithMpesaUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val cartRepository: CartRepository
) {

    /** @param clearCart true when paying for the order just created from the cart. */
    operator fun invoke(orderId: String, phone: String, clearCart: Boolean): Flow<PaymentProgress> = flow {
        val normalized = MpesaPhone.normalize(phone)
        if (normalized == null) {
            emit(PaymentProgress.Failed("Enter a valid Safaricom number, e.g. 0712 345 678."))
            return@flow
        }

        emit(PaymentProgress.SendingPrompt)
        when (val result = paymentRepository.startStkPush(orderId, normalized)) {
            is AppResult.Error -> {
                // The push is refused when the order is already paid (e.g. the customer tapped
                // "Resend" right after entering the PIN). Check the order before showing an error.
                val paidReceipt = paidReceiptOrNull(orderId)
                if (paidReceipt != null) {
                    onPaid(orderId, clearCart)
                    emit(PaymentProgress.Paid(orderId, paidReceipt))
                } else {
                    emit(PaymentProgress.Failed(result.error.toUserMessage()))
                }
            }
            is AppResult.Success -> {
                val request = result.data
                emit(
                    PaymentProgress.AwaitingPin(
                        message = request.customerMessage.ifBlank { "Check your phone and enter your M-Pesa PIN." },
                        amount = request.amount,
                        phone = request.phone.ifBlank { normalized }
                    )
                )
                pollUntilFinal(orderId, request.checkoutRequestId, Constants.PAYMENT_TIMEOUT_MS, clearCart)
            }
        }
    }

    /** Re-checks a payment that timed out on our side (the customer may still have paid). */
    fun checkAgain(orderId: String, checkoutRequestId: String, clearCart: Boolean): Flow<PaymentProgress> = flow {
        emit(PaymentProgress.AwaitingPin("Checking your payment…", amount = 0, phone = ""))
        pollUntilFinal(orderId, checkoutRequestId, timeoutMs = 30_000L, clearCart = clearCart, initialDelay = false)
    }

    private suspend fun FlowCollector<PaymentProgress>.pollUntilFinal(
        orderId: String,
        checkoutRequestId: String,
        timeoutMs: Long,
        clearCart: Boolean,
        initialDelay: Boolean = true
    ) {
        val interval = Constants.PAYMENT_POLL_INTERVAL_MS
        val attempts = (timeoutMs / interval).toInt().coerceAtLeast(1)
        var consecutiveErrors = 0

        repeat(attempts) { attempt ->
            if (initialDelay || attempt > 0) delay(interval)
            when (val result = paymentRepository.getStatus(checkoutRequestId)) {
                is AppResult.Error -> {
                    consecutiveErrors++
                    if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                        emit(PaymentProgress.Unconfirmed(checkoutRequestId))
                        return
                    }
                }
                is AppResult.Success -> {
                    consecutiveErrors = 0
                    val update = result.data
                    when (update.status) {
                        PaymentStatus.SUCCESS -> {
                            onPaid(orderId, clearCart)
                            emit(PaymentProgress.Paid(orderId, update.mpesaReceiptNumber))
                            return
                        }
                        PaymentStatus.FAILED, PaymentStatus.CANCELLED, PaymentStatus.TIMEOUT -> {
                            emit(PaymentProgress.Failed(update.message.ifBlank { "The payment was not completed." }))
                            return
                        }
                        PaymentStatus.PENDING, PaymentStatus.UNKNOWN -> Unit
                    }
                }
            }
        }
        emit(PaymentProgress.Unconfirmed(checkoutRequestId))
    }

    /** Receipt number ("" if unknown) when the backend says the order is paid, else null. */
    private suspend fun paidReceiptOrNull(orderId: String): String? {
        val order = (orderRepository.refreshOrder(orderId) as? AppResult.Success)?.data ?: return null
        return if (order.status == OrderStatus.PAID) order.payment?.mpesaReceiptNumber.orEmpty() else null
    }

    private suspend fun onPaid(orderId: String, clearCart: Boolean) {
        if (clearCart) cartRepository.clear()
        orderRepository.refreshOrder(orderId) // best effort: receipt screen also refreshes
    }

    private companion object {
        const val MAX_CONSECUTIVE_ERRORS = 5
    }
}
