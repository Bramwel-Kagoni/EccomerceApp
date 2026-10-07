package com.bramwel.eccomerceapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bramwel.eccomerceapp.features.cart.data.local.CartDao
import com.bramwel.eccomerceapp.features.cart.data.local.CartItemEntity
import com.bramwel.eccomerceapp.features.orders.data.local.OrderDao
import com.bramwel.eccomerceapp.features.orders.data.local.OrderEntity
import com.bramwel.eccomerceapp.features.products.data.local.CategoryEntity
import com.bramwel.eccomerceapp.features.products.data.local.ProductDao
import com.bramwel.eccomerceapp.features.products.data.local.ProductEntity
import com.bramwel.eccomerceapp.features.wishlist.data.local.WishlistDao
import com.bramwel.eccomerceapp.features.wishlist.data.local.WishlistEntity

@Database(
    entities = [
        ProductEntity::class,
        CategoryEntity::class,
        CartItemEntity::class,
        WishlistEntity::class,
        OrderEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun orderDao(): OrderDao

    companion object {
        const val NAME = "shop.db"
    }
}
