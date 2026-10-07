package com.bramwel.eccomerceapp.features.orders.domain.repository

import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.features.orders.domain.model.CartLine
import com.bramwel.eccomerceapp.features.orders.domain.model.DeliveryDetails
import com.bramwel.eccomerceapp.features.orders.domain.model.DeliveryMethod
import com.bramwel.eccomerceapp.features.orders.domain.model.Order
import com.bramwel.eccomerceapp.features.orders.domain.model.OrderQuote
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    suspend fun getQuote(
        lines: List<CartLine>,
        deliveryMethod: DeliveryMethod,
        promoCode: String
    ): AppResult<OrderQuote>

    suspend fun placeOrder(
        lines: List<CartLine>,
        details: DeliveryDetails,
        deliveryMethod: DeliveryMethod,
        promoCode: String
    ): AppResult<Order>

    /** Cached orders (work offline); call [refreshOrders] to sync. */
    fun observeOrders(): Flow<List<Order>>
    fun observeOrder(orderId: String): Flow<Order?>
    suspend fun refreshOrders(): AppResult<Unit>
    suspend fun refreshOrder(orderId: String): AppResult<Order>
}
