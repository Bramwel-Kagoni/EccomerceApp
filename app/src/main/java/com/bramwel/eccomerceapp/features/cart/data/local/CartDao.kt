package com.bramwel.eccomerceapp.features.cart.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {

    @Query(
        """
        SELECT p.*, c.quantity AS cartQuantity, c.addedAt AS cartAddedAt
        FROM cart_items c
        INNER JOIN products p ON p.id = c.productId
        ORDER BY c.addedAt DESC
        """
    )
    fun observeCart(): Flow<List<CartProductRow>>

    @Query("SELECT COALESCE(SUM(c.quantity), 0) FROM cart_items c INNER JOIN products p ON p.id = c.productId")
    fun observeItemCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM cart_items WHERE productId = :productId")
    fun observeQuantity(productId: Int): Flow<Int>

    @Query("SELECT * FROM cart_items WHERE productId = :productId")
    suspend fun find(productId: Int): CartItemEntity?

    @Query("SELECT stock FROM products WHERE id = :productId")
    suspend fun stockOf(productId: Int): Int?

    @Upsert
    suspend fun upsert(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun delete(productId: Int)

    @Query("DELETE FROM cart_items")
    suspend fun clear()
}
