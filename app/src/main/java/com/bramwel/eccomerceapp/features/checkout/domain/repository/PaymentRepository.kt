package com.bramwel.eccomerceapp.features.checkout.domain.repository

import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.features.checkout.domain.model.PaymentStatusUpdate
import com.bramwel.eccomerceapp.features.checkout.domain.model.StkPushRequest

interface PaymentRepository {
    suspend fun startStkPush(orderId: String, phone: String): AppResult<StkPushRequest>
    suspend fun getStatus(checkoutRequestId: String): AppResult<PaymentStatusUpdate>
}
