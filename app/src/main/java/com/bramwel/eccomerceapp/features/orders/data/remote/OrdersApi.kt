package com.bramwel.eccomerceapp.features.orders.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Django backend: backend/shop/urls.py */
interface OrdersApi {

    @POST("api/orders/quote/")
    suspend fun quote(@Body body: QuoteRequestDto): QuoteDto

    @POST("api/orders/")
    suspend fun createOrder(@Body body: CreateOrderRequestDto): OrderDto

    @GET("api/orders/")
    suspend fun getOrders(): List<OrderDto>

    @GET("api/orders/{id}/")
    suspend fun getOrder(@Path("id") orderId: String): OrderDto
}
