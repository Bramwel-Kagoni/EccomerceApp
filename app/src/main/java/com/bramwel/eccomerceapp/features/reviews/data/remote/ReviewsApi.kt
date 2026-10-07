package com.bramwel.eccomerceapp.features.reviews.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Django backend: backend/shop/urls.py (reviews) */
interface ReviewsApi {

    @GET("api/products/{id}/reviews/")
    suspend fun getProductReviews(@Path("id") productId: Int): ProductReviewsDto

    @GET("api/reviews/")
    suspend fun getMyReviews(): List<MyReviewDto>

    @POST("api/reviews/")
    suspend fun submitReview(@Body body: ReviewRequestDto): MyReviewDto
}
