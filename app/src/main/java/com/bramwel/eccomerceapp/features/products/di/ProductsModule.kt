package com.bramwel.eccomerceapp.features.products.di

import com.bramwel.eccomerceapp.core.network.CatalogRetrofit
import com.bramwel.eccomerceapp.features.products.data.remote.ProductApi
import com.bramwel.eccomerceapp.features.products.data.repository.ProductRepositoryImpl
import com.bramwel.eccomerceapp.features.products.domain.repository.ProductRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProductsModule {

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    companion object {
        @Provides
        @Singleton
        fun provideProductApi(@CatalogRetrofit retrofit: Retrofit): ProductApi =
            retrofit.create(ProductApi::class.java)
    }
}
