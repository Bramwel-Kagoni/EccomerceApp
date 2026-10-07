package com.bramwel.eccomerceapp.features.orders.di

import com.bramwel.eccomerceapp.core.network.BackendRetrofit
import com.bramwel.eccomerceapp.features.orders.data.receipt.ReceiptPdfGenerator
import com.bramwel.eccomerceapp.features.orders.data.remote.OrdersApi
import com.bramwel.eccomerceapp.features.orders.data.repository.OrderRepositoryImpl
import com.bramwel.eccomerceapp.features.orders.domain.repository.OrderRepository
import com.bramwel.eccomerceapp.features.orders.domain.repository.ReceiptExporter
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OrdersModule {

    @Binds
    @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository

    @Binds
    @Singleton
    abstract fun bindReceiptExporter(impl: ReceiptPdfGenerator): ReceiptExporter

    companion object {
        @Provides
        @Singleton
        fun provideOrdersApi(@BackendRetrofit retrofit: Retrofit): OrdersApi =
            retrofit.create(OrdersApi::class.java)
    }
}
