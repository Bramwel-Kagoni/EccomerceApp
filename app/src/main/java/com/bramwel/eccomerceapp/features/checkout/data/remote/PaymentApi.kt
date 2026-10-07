package com.bramwel.eccomerceapp.features.checkout.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** M-Pesa endpoints on our Django backend. Daraja credentials never touch the app. */
interface PaymentApi {

    @POST("api/orders/{id}/pay/")
    suspend fun startStkPush(
        @Path("id") orderId: String,
        @Body body: StkPushRequestDto
    ): StkPushResponseDto

    @GET("api/payments/{checkoutRequestId}/")
    suspend fun getPaymentStatus(@Path("checkoutRequestId") checkoutRequestId: String): PaymentStatusDto
}

@Serializable
data class StkPushRequestDto(val phone: String)

@Serializable
data class StkPushResponseDto(
    @SerialName("checkout_request_id") val checkoutRequestId: String,
    val status: String,
    @SerialName("customer_message") val customerMessage: String = "",
    val amount: Long = 0,
    val phone: String = ""
)

@Serializable
data class PaymentStatusDto(
    @SerialName("checkout_request_id") val checkoutRequestId: String,
    val status: String,
    @SerialName("result_desc") val resultDesc: String = "",
    @SerialName("mpesa_receipt_number") val mpesaReceiptNumber: String = ""
)
