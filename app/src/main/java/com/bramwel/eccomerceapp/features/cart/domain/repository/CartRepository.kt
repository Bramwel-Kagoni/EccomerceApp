package com.bramwel.eccomerceapp.features.cart.domain.repository

import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.features.cart.domain.model.CartItem
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun observeCart(): Flow<List<CartItem>>
    fun observeItemCount(): Flow<Int>
    fun observeQuantity(productId: Int): Flow<Int>

    /** Adds [quantity] more of a product, capped by stock. Returns the new quantity. */
    suspend fun add(productId: Int, quantity: Int = 1): AppResult<Int>

    /** Sets an exact quantity (0 removes the item). */
    suspend fun setQuantity(productId: Int, quantity: Int): AppResult<Int>
    suspend fun remove(productId: Int)
    suspend fun clear()
}
