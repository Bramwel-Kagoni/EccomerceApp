package com.bramwel.eccomerceapp.features.reviews.data.repository

import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.core.common.map
import com.bramwel.eccomerceapp.core.network.safeApiCall
import com.bramwel.eccomerceapp.features.reviews.data.remote.ReviewRequestDto
import com.bramwel.eccomerceapp.features.reviews.data.remote.ReviewsApi
import com.bramwel.eccomerceapp.features.reviews.data.remote.toDomain
import com.bramwel.eccomerceapp.features.reviews.domain.model.MyReview
import com.bramwel.eccomerceapp.features.reviews.domain.model.ProductReviews
import com.bramwel.eccomerceapp.features.reviews.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** Reviews live on the server (they need moderation); the customer's own list is cached in memory. */
@Singleton
class ReviewRepositoryImpl @Inject constructor(
    private val api: ReviewsApi
) : ReviewRepository {

    private val myReviews = MutableStateFlow<List<MyReview>>(emptyList())

    override suspend fun getProductReviews(productId: Int): AppResult<ProductReviews> =
        safeApiCall { api.getProductReviews(productId).toDomain() }

    override fun observeMyReviews(): Flow<List<MyReview>> = myReviews.asStateFlow()

    override suspend fun refreshMyReviews(): AppResult<Unit> =
        safeApiCall { api.getMyReviews().map { it.toDomain() } }.map { myReviews.value = it }

    override suspend fun submitReview(
        productId: Int,
        rating: Int,
        comment: String,
        reviewerName: String
    ): AppResult<MyReview> = safeApiCall {
        api.submitReview(ReviewRequestDto(productId, rating, comment, reviewerName)).toDomain()
    }.map { review ->
        myReviews.update { current -> listOf(review) + current.filterNot { it.productId == review.productId } }
        review
    }
}
