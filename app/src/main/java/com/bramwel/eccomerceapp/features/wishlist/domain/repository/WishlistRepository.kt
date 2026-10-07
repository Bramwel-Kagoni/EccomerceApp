package com.bramwel.eccomerceapp.features.wishlist.domain.repository

import com.bramwel.eccomerceapp.features.products.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface WishlistRepository {
    fun observeWishlistIds(): Flow<Set<Int>>
    fun observeWishlist(): Flow<List<Product>>

    /** Returns true if the product was added, false if it was removed. */
    suspend fun toggle(productId: Int): Boolean
    suspend fun remove(productId: Int)
    suspend fun clear()
}
