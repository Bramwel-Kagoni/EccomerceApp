package com.bramwel.eccomerceapp.features.checkout.data.repository

import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.core.network.safeApiCall
import com.bramwel.eccomerceapp.features.checkout.data.remote.PaymentApi
import com.bramwel.eccomerceapp.features.checkout.data.remote.StkPushRequestDto
import com.bramwel.eccomerceapp.features.checkout.domain.model.PaymentStatusUpdate
import com.bramwel.eccomerceapp.features.checkout.domain.model.StkPushRequest
import com.bramwel.eccomerceapp.features.checkout.domain.repository.PaymentRepository
import com.bramwel.eccomerceapp.features.orders.domain.model.PaymentStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepositoryImpl @Inject constructor(
    private val api: PaymentApi
) : PaymentRepository {

    override suspend fun startStkPush(orderId: String, phone: String): AppResult<StkPushRequest> =
        safeApiCall {
            val response = api.startStkPush(orderId, StkPushRequestDto(phone))
            StkPushRequest(
                checkoutRequestId = response.checkoutRequestId,
                customerMessage = response.customerMessage,
                amount = response.amount,
                phone = response.phone
            )
        }

    override suspend fun getStatus(checkoutRequestId: String): AppResult<PaymentStatusUpdate> =
        safeApiCall {
            val response = api.getPaymentStatus(checkoutRequestId)
            PaymentStatusUpdate(
                checkoutRequestId = response.checkoutRequestId,
                status = PaymentStatus.fromApi(response.status),
                message = response.resultDesc,
                mpesaReceiptNumber = response.mpesaReceiptNumber
            )
        }
}
