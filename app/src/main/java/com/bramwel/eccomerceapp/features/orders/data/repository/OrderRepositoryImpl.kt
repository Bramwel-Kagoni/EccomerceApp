package com.bramwel.eccomerceapp.features.orders.data.repository

import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.core.common.map
import com.bramwel.eccomerceapp.core.network.safeApiCall
import com.bramwel.eccomerceapp.features.orders.data.local.OrderDao
import com.bramwel.eccomerceapp.features.orders.data.local.OrderEntity
import com.bramwel.eccomerceapp.features.orders.data.remote.CreateOrderRequestDto
import com.bramwel.eccomerceapp.features.orders.data.remote.OrderDto
import com.bramwel.eccomerceapp.features.orders.data.remote.OrdersApi
import com.bramwel.eccomerceapp.features.orders.data.remote.QuoteRequestDto
import com.bramwel.eccomerceapp.features.orders.data.remote.toDomain
import com.bramwel.eccomerceapp.features.orders.data.remote.toDto
import com.bramwel.eccomerceapp.features.orders.domain.model.CartLine
import com.bramwel.eccomerceapp.features.orders.domain.model.DeliveryDetails
import com.bramwel.eccomerceapp.features.orders.domain.model.DeliveryMethod
import com.bramwel.eccomerceapp.features.orders.domain.model.Order
import com.bramwel.eccomerceapp.features.orders.domain.model.OrderQuote
import com.bramwel.eccomerceapp.features.orders.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val api: OrdersApi,
    private val dao: OrderDao,
    private val json: Json
) : OrderRepository {

    override suspend fun getQuote(
        lines: List<CartLine>,
        deliveryMethod: DeliveryMethod,
        promoCode: String
    ): AppResult<OrderQuote> = safeApiCall {
        api.quote(
            QuoteRequestDto(
                items = lines.map { it.toDto() },
                deliveryMethod = deliveryMethod.apiValue,
                promoCode = promoCode.trim()
            )
        ).toDomain()
    }

    override suspend fun placeOrder(
        lines: List<CartLine>,
        details: DeliveryDetails,
        deliveryMethod: DeliveryMethod,
        promoCode: String
    ): AppResult<Order> = safeApiCall {
        val dto = api.createOrder(
            CreateOrderRequestDto(
                items = lines.map { it.toDto() },
                deliveryMethod = deliveryMethod.apiValue,
                promoCode = promoCode.trim(),
                customerName = details.name.trim(),
                phone = details.phone.trim(),
                email = details.email.trim(),
                address = details.address.trim(),
                city = details.city.trim(),
                notes = details.notes.trim()
            )
        )
        dao.upsert(dto.toEntity())
        dto.toDomain()
    }

    override fun observeOrders(): Flow<List<Order>> =
        dao.observeAll().map { entities -> entities.mapNotNull { it.toDomainOrNull() } }

    override fun observeOrder(orderId: String): Flow<Order?> =
        dao.observeById(orderId).map { it?.toDomainOrNull() }

    override suspend fun refreshOrders(): AppResult<Unit> = safeApiCall {
        dao.upsertAll(api.getOrders().map { it.toEntity() })
    }

    override suspend fun refreshOrder(orderId: String): AppResult<Order> = safeApiCall {
        api.getOrder(orderId).also { dao.upsert(it.toEntity()) }
    }.map { it.toDomain() }

    private fun OrderDto.toEntity() = OrderEntity(
        id = id,
        orderNumber = orderNumber,
        status = status,
        createdAt = createdAt,
        total = total,
        payload = json.encodeToString(OrderDto.serializer(), this)
    )

    private fun OrderEntity.toDomainOrNull(): Order? = try {
        json.decodeFromString(OrderDto.serializer(), payload).toDomain()
    } catch (e: IllegalArgumentException) {
        null // corrupted/old cache entry; it will be replaced on the next sync
    }
}
