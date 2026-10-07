package com.bramwel.eccomerceapp.features.reviews.di

import com.bramwel.eccomerceapp.core.network.BackendRetrofit
import com.bramwel.eccomerceapp.features.reviews.data.remote.ReviewsApi
import com.bramwel.eccomerceapp.features.reviews.data.repository.ReviewRepositoryImpl
import com.bramwel.eccomerceapp.features.reviews.domain.repository.ReviewRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReviewsModule {

    @Binds
    @Singleton
    abstract fun bindReviewRepository(impl: ReviewRepositoryImpl): ReviewRepository

    companion object {
        @Provides
        @Singleton
        fun provideReviewsApi(@BackendRetrofit retrofit: Retrofit): ReviewsApi =
            retrofit.create(ReviewsApi::class.java)
    }
}
