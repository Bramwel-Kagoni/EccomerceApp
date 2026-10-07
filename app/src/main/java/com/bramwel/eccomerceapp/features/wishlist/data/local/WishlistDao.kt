package com.bramwel.eccomerceapp.features.wishlist.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.bramwel.eccomerceapp.features.products.data.local.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WishlistDao {

    @Query("SELECT productId FROM wishlist")
    abstract fun observeIds(): Flow<List<Int>>

    @Query(
        """
        SELECT p.* FROM wishlist w
        INNER JOIN products p ON p.id = w.productId
        ORDER BY w.addedAt DESC
        """
    )
    abstract fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist WHERE productId = :productId)")
    abstract suspend fun contains(productId: Int): Boolean

    @Upsert
    abstract suspend fun insert(entity: WishlistEntity)

    @Query("DELETE FROM wishlist WHERE productId = :productId")
    abstract suspend fun delete(productId: Int)

    @Query("DELETE FROM wishlist")
    abstract suspend fun clear()

    /** Returns true when the product is now in the wishlist. */
    @Transaction
    open suspend fun toggle(productId: Int, now: Long): Boolean =
        if (contains(productId)) {
            delete(productId)
            false
        } else {
            insert(WishlistEntity(productId, now))
            true
        }
}
