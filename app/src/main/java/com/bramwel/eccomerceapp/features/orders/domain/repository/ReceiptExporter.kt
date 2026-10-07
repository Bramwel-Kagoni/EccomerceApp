package com.bramwel.eccomerceapp.features.orders.domain.repository

import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.features.orders.domain.model.Order

/** Produces a shareable receipt document for an order. */
interface ReceiptExporter {
    /** Returns the absolute path of the generated PDF. */
    suspend fun export(order: Order, storeName: String): AppResult<String>
}
