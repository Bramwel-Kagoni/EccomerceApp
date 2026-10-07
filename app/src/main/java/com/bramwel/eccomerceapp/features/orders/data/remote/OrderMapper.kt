package com.bramwel.eccomerceapp.features.orders.data.remote

import com.bramwel.eccomerceapp.features.orders.domain.model.CartLine
import com.bramwel.eccomerceapp.features.orders.domain.model.DeliveryMethod
import com.bramwel.eccomerceapp.features.orders.domain.model.Order
import com.bramwel.eccomerceapp.features.orders.domain.model.OrderItem
import com.bramwel.eccomerceapp.features.orders.domain.model.OrderQuote
import com.bramwel.eccomerceapp.features.orders.domain.model.OrderStatus
import com.bramwel.eccomerceapp.features.orders.domain.model.PaymentInfo
import com.bramwel.eccomerceapp.features.orders.domain.model.PaymentStatus

fun CartLine.toDto() = CartLineDto(productId = productId, quantity = quantity)

fun OrderItemDto.toDomain() = OrderItem(
    productId = productId,
    title = title,
    thumbnail = thumbnail,
    unitPrice = unitPrice,
    quantity = quantity,
    lineTotal = if (lineTotal > 0) lineTotal else unitPrice * quantity
)

fun PaymentDto.toDomain() = PaymentInfo(
    checkoutRequestId = checkoutRequestId,
    status = PaymentStatus.fromApi(status),
    phone = phone,
    amount = amount,
    resultDescription = resultDesc,
    mpesaReceiptNumber = mpesaReceiptNumber,
    transactionDate = transactionDate
)

fun OrderDto.toDomain() = Order(
    id = id,
    orderNumber = orderNumber,
    status = OrderStatus.fromApi(status),
    customerName = customerName,
    phone = phone,
    email = email,
    address = address,
    city = city,
    notes = notes,
    deliveryMethod = DeliveryMethod.fromApi(deliveryMethod),
    subtotal = subtotal,
    deliveryFee = deliveryFee,
    discount = discount,
    total = total,
    promoCode = promoCode,
    createdAt = createdAt,
    paidAt = paidAt,
    items = items.map { it.toDomain() },
    payment = payment?.toDomain()
)

fun QuoteDto.toDomain() = OrderQuote(
    items = items.map { it.toDomain() },
    subtotal = subtotal,
    deliveryFee = deliveryFee,
    discount = discount,
    total = total,
    promoCode = promoCode,
    promoValid = promoValid,
    promoMessage = promoMessage,
    freeDeliveryThreshold = freeDeliveryThreshold
)
