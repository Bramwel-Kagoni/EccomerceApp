package com.bramwel.eccomerceapp.features.orders.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Offline copy of an order (full backend JSON in [payload]) so receipts open without internet. */
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val orderNumber: String,
    val status: String,
    val createdAt: String,
    val total: Long,
    val payload: String
)
