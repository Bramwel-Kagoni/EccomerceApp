package com.bramwel.eccomerceapp.features.checkout

import com.bramwel.eccomerceapp.core.common.AppError
import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.features.cart.domain.model.CartItem
import com.bramwel.eccomerceapp.features.cart.domain.repository.CartRepository
import com.bramwel.eccomerceapp.features.checkout.domain.model.PaymentProgress
import com.bramwel.eccomerceapp.features.checkout.domain.model.PaymentStatusUpdate
import com.bramwel.eccomerceapp.features.checkout.domain.model.StkPushRequest
import com.bramwel.eccomerceapp.features.checkout.domain.repository.PaymentRepository
import com.bramwel.eccomerceapp.features.checkout.domain.usecase.PayWithMpesaUseCase
import com.bramwel.eccomerceapp.features.orders.domain.model.CartLine
import com.bramwel.eccomerceapp.features.orders.domain.model.DeliveryDetails
import com.bramwel.eccomerceapp.features.orders.domain.model.DeliveryMethod
import com.bramwel.eccomerceapp.features.orders.domain.model.Order
import com.bramwel.eccomerceapp.features.orders.domain.model.OrderQuote
import com.bramwel.eccomerceapp.features.orders.domain.model.PaymentStatus
import com.bramwel.eccomerceapp.features.orders.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PayWithMpesaUseCaseTest {

    private class FakePayments(
        private val push: AppResult<StkPushRequest>,
        statuses: List<PaymentStatus>
    ) : PaymentRepository {
        private val queue = ArrayDeque(statuses)
        var sentPhone: String? = null

        override suspend fun startStkPush(orderId: String, phone: String): AppResult<StkPushRequest> {
            sentPhone = phone
            return push
        }

        override suspend fun getStatus(checkoutRequestId: String): AppResult<PaymentStatusUpdate> {
            val status = queue.removeFirstOrNull() ?: PaymentStatus.PENDING
            return AppResult.Success(PaymentStatusUpdate(checkoutRequestId, status, "done", "SIK7RT61SV"))
        }
    }

    private class FakeCart : CartRepository {
        var cleared = false
        override fun observeCart(): Flow<List<CartItem>> = flowOf(emptyList())
        override fun observeItemCount(): Flow<Int> = flowOf(0)
        override fun observeQuantity(productId: Int): Flow<Int> = flowOf(0)
        override suspend fun add(productId: Int, quantity: Int): AppResult<Int> = AppResult.Success(quantity)
        override suspend fun setQuantity(productId: Int, quantity: Int): AppResult<Int> = AppResult.Success(quantity)
        override suspend fun remove(productId: Int) = Unit
        override suspend fun clear() { cleared = true }
    }

    private class FakeOrders(private val paidOrder: Order? = null) : OrderRepository {
        override suspend fun getQuote(lines: List<CartLine>, deliveryMethod: DeliveryMethod, promoCode: String):
            AppResult<OrderQuote> = AppResult.Error(AppError.Unknown())
        override suspend fun placeOrder(
            lines: List<CartLine>, details: DeliveryDetails, deliveryMethod: DeliveryMethod, promoCode: String
        ): AppResult<Order> = AppResult.Error(AppError.Unknown())
        override fun observeOrders(): Flow<List<Order>> = emptyFlow()
        override fun observeOrder(orderId: String): Flow<Order?> = emptyFlow()
        override suspend fun refreshOrders(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun refreshOrder(orderId: String): AppResult<Order> =
            paidOrder?.let { AppResult.Success(it) } ?: AppResult.Error(AppError.Unknown())
    }

    private val push = AppResult.Success(StkPushRequest("ws_CO_1", "Enter PIN", 1_500, "254712345678"))

    @Test
    fun `successful payment clears the cart`() = runTest {
        val cart = FakeCart()
        val payments = FakePayments(push, listOf(PaymentStatus.PENDING, PaymentStatus.SUCCESS))
        val steps = PayWithMpesaUseCase(payments, FakeOrders(), cart)("order-1", "0712 345 678", clearCart = true).toList()

        assertEquals("254712345678", payments.sentPhone)
        assertTrue(steps.first() is PaymentProgress.SendingPrompt)
        assertTrue(steps[1] is PaymentProgress.AwaitingPin)
        assertEquals(PaymentProgress.Paid("order-1", "SIK7RT61SV"), steps.last())
        assertTrue(cart.cleared)
    }

    @Test
    fun `paying an old order keeps the current cart`() = runTest {
        val cart = FakeCart()
        val payments = FakePayments(push, listOf(PaymentStatus.SUCCESS))
        PayWithMpesaUseCase(payments, FakeOrders(), cart)("order-1", "0712345678", clearCart = false).toList()
        assertFalse(cart.cleared)
    }

    @Test
    fun `cancelled prompt reports failure`() = runTest {
        val payments = FakePayments(push, listOf(PaymentStatus.CANCELLED))
        val last = PayWithMpesaUseCase(payments, FakeOrders(), FakeCart())("order-1", "0712345678", true).toList().last()
        assertTrue(last is PaymentProgress.Failed)
    }

    @Test
    fun `no answer before timeout is unconfirmed, not failed`() = runTest {
        val payments = FakePayments(push, emptyList())
        val last = PayWithMpesaUseCase(payments, FakeOrders(), FakeCart())("order-1", "0712345678", true).toList().last()
        assertEquals(PaymentProgress.Unconfirmed("ws_CO_1"), last)
    }

    @Test
    fun `invalid phone never calls the backend`() = runTest {
        val payments = FakePayments(push, emptyList())
        val steps = PayWithMpesaUseCase(payments, FakeOrders(), FakeCart())("order-1", "123", true).toList()
        assertEquals(1, steps.size)
        assertTrue(steps.single() is PaymentProgress.Failed)
        assertEquals(null, payments.sentPhone)
    }

    @Test
    fun `resend on an already paid order shows success instead of an error`() = runTest {
        val paid = com.bramwel.eccomerceapp.features.orders.presentation.OrderPreviewData.paidOrder
        val cart = FakeCart()
        val payments = FakePayments(AppResult.Error(AppError.Server(409, "This order has already been paid.")), emptyList())
        val steps = PayWithMpesaUseCase(payments, FakeOrders(paid), cart)(paid.id, "0712345678", clearCart = true).toList()
        assertEquals(PaymentProgress.Paid(paid.id, "SIK7RT61SV"), steps.last())
        assertTrue(cart.cleared)
    }
}
