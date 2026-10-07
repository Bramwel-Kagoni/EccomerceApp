package com.bramwel.eccomerceapp.features.cart.data.repository

import com.bramwel.eccomerceapp.core.common.AppError
import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.core.common.Constants
import com.bramwel.eccomerceapp.features.cart.data.local.CartDao
import com.bramwel.eccomerceapp.features.cart.data.local.CartItemEntity
import com.bramwel.eccomerceapp.features.cart.domain.model.CartItem
import com.bramwel.eccomerceapp.features.cart.domain.repository.CartRepository
import com.bramwel.eccomerceapp.features.products.data.local.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartRepositoryImpl @Inject constructor(
    private val dao: CartDao
) : CartRepository {

    override fun observeCart(): Flow<List<CartItem>> = dao.observeCart().map { rows ->
        rows.map { CartItem(product = it.product.toDomain(), quantity = it.cartQuantity) }
    }

    override fun observeItemCount(): Flow<Int> = dao.observeItemCount()

    override fun observeQuantity(productId: Int): Flow<Int> = dao.observeQuantity(productId)

    override suspend fun add(productId: Int, quantity: Int): AppResult<Int> {
        val current = dao.find(productId)?.quantity ?: 0
        return setQuantity(productId, current + quantity, addedAt = dao.find(productId)?.addedAt)
    }

    override suspend fun setQuantity(productId: Int, quantity: Int): AppResult<Int> =
        setQuantity(productId, quantity, addedAt = dao.find(productId)?.addedAt)

    private suspend fun setQuantity(productId: Int, quantity: Int, addedAt: Long?): AppResult<Int> {
        if (quantity <= 0) {
            dao.delete(productId)
            return AppResult.Success(0)
        }
        val stock = dao.stockOf(productId)
            ?: return AppResult.Error(AppError.Validation("This product is no longer available."))
        if (stock <= 0) return AppResult.Error(AppError.Validation("Sorry, this item is out of stock."))

        val max = minOf(stock, Constants.MAX_QUANTITY_PER_ITEM)
        val current = dao.find(productId)?.quantity ?: 0
        if (quantity > max && current >= max) {
            return AppResult.Error(AppError.Validation("You can only buy $max of this item."))
        }
        val finalQuantity = quantity.coerceAtMost(max)
        dao.upsert(
            CartItemEntity(
                productId = productId,
                quantity = finalQuantity,
                addedAt = addedAt ?: System.currentTimeMillis()
            )
        )
        return AppResult.Success(finalQuantity)
    }

    override suspend fun remove(productId: Int) = dao.delete(productId)

    override suspend fun clear() = dao.clear()
}
