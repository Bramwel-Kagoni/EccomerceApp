package com.bramwel.eccomerceapp.features.checkout.di

import com.bramwel.eccomerceapp.core.network.BackendRetrofit
import com.bramwel.eccomerceapp.features.checkout.data.remote.PaymentApi
import com.bramwel.eccomerceapp.features.checkout.data.repository.PaymentRepositoryImpl
import com.bramwel.eccomerceapp.features.checkout.domain.repository.PaymentRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CheckoutModule {

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(impl: PaymentRepositoryImpl): PaymentRepository

    companion object {
        @Provides
        @Singleton
        fun providePaymentApi(@BackendRetrofit retrofit: Retrofit): PaymentApi =
            retrofit.create(PaymentApi::class.java)
    }
}
