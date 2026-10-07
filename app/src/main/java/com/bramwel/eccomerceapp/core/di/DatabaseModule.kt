package com.bramwel.eccomerceapp.core.di

import android.content.Context
import androidx.room.Room
import com.bramwel.eccomerceapp.core.database.AppDatabase
import com.bramwel.eccomerceapp.features.cart.data.local.CartDao
import com.bramwel.eccomerceapp.features.orders.data.local.OrderDao
import com.bramwel.eccomerceapp.features.products.data.local.ProductDao
import com.bramwel.eccomerceapp.features.wishlist.data.local.WishlistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            // Catalog is re-downloadable; add real migrations before storing irreplaceable data.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()

    @Provides
    fun provideCartDao(db: AppDatabase): CartDao = db.cartDao()

    @Provides
    fun provideWishlistDao(db: AppDatabase): WishlistDao = db.wishlistDao()

    @Provides
    fun provideOrderDao(db: AppDatabase): OrderDao = db.orderDao()
}
