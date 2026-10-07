package com.bramwel.eccomerceapp.features.wishlist.data.repository

import com.bramwel.eccomerceapp.features.products.data.local.toDomain
import com.bramwel.eccomerceapp.features.products.domain.model.Product
import com.bramwel.eccomerceapp.features.wishlist.data.local.WishlistDao
import com.bramwel.eccomerceapp.features.wishlist.domain.repository.WishlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WishlistRepositoryImpl @Inject constructor(
    private val dao: WishlistDao
) : WishlistRepository {

    override fun observeWishlistIds(): Flow<Set<Int>> = dao.observeIds().map { it.toSet() }

    override fun observeWishlist(): Flow<List<Product>> =
        dao.observeProducts().map { list -> list.map { it.toDomain() } }

    override suspend fun toggle(productId: Int): Boolean =
        dao.toggle(productId, System.currentTimeMillis())

    override suspend fun remove(productId: Int) = dao.delete(productId)

    override suspend fun clear() = dao.clear()
}
