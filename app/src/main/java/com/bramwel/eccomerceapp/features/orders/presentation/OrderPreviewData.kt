package com.bramwel.eccomerceapp.features.orders.presentation

import com.bramwel.eccomerceapp.features.orders.domain.model.DeliveryMethod
import com.bramwel.eccomerceapp.features.orders.domain.model.Order
import com.bramwel.eccomerceapp.features.orders.domain.model.OrderItem
import com.bramwel.eccomerceapp.features.orders.domain.model.OrderStatus
import com.bramwel.eccomerceapp.features.orders.domain.model.PaymentInfo
import com.bramwel.eccomerceapp.features.orders.domain.model.PaymentStatus

/** Sample orders for @Preview only. */
object OrderPreviewData {
    val paidOrder = Order(
        id = "5b1c",
        orderNumber = "ORD-7F3K9Q2A",
        status = OrderStatus.PAID,
        customerName = "Kagoni Livwege",
        phone = "254712345678",
        email = "kagoni@example.com",
        address = "Moi Avenue, Hse 4",
        city = "Nairobi",
        notes = "",
        deliveryMethod = DeliveryMethod.STANDARD,
        subtotal = 16_058,
        deliveryFee = 0,
        discount = 1_000,
        total = 15_058,
        promoCode = "WELCOME10",
        createdAt = "2026-09-26T18:30:12.123456+03:00",
        paidAt = "2026-09-26T18:31:02+03:00",
        items = listOf(
            OrderItem(1, "Essence Mascara Lash Princess", "", 1289, 2, 2578),
            OrderItem(6, "Apple Airpods", "", 13_480, 1, 13_480)
        ),
        payment = PaymentInfo("ws_CO_1", PaymentStatus.SUCCESS, "254712345678", 15_058, "Payment received", "SIK7RT61SV", "20260926183102")
    )

    val pendingOrder = paidOrder.copy(
        id = "9a2d",
        orderNumber = "ORD-P2Q8M1ZX",
        status = OrderStatus.PENDING_PAYMENT,
        paidAt = null,
        payment = null
    )

    val failedOrder = paidOrder.copy(
        id = "77ee",
        orderNumber = "ORD-F4ILED00",
        status = OrderStatus.PAYMENT_FAILED,
        paidAt = null,
        payment = paidOrder.payment?.copy(status = PaymentStatus.CANCELLED, mpesaReceiptNumber = "")
    )
}
