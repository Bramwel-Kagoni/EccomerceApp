package com.bramwel.eccomerceapp.features.wishlist.di

import com.bramwel.eccomerceapp.features.wishlist.data.repository.WishlistRepositoryImpl
import com.bramwel.eccomerceapp.features.wishlist.domain.repository.WishlistRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WishlistModule {
    @Binds
    @Singleton
    abstract fun bindWishlistRepository(impl: WishlistRepositoryImpl): WishlistRepository
}
