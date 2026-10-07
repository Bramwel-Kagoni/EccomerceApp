package com.bramwel.eccomerceapp.features.cart.di

import com.bramwel.eccomerceapp.features.cart.data.repository.CartRepositoryImpl
import com.bramwel.eccomerceapp.features.cart.domain.repository.CartRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CartModule {
    @Binds
    @Singleton
    abstract fun bindCartRepository(impl: CartRepositoryImpl): CartRepository
}
