package com.bramwel.eccomerceapp.features.cart.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bramwel.eccomerceapp.features.products.data.local.ProductEntity

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val productId: Int,
    val quantity: Int,
    val addedAt: Long
)

/** A cart row joined with its product so prices are always the latest cached ones. */
data class CartProductRow(
    @Embedded val product: ProductEntity,
    val cartQuantity: Int,
    val cartAddedAt: Long
)
