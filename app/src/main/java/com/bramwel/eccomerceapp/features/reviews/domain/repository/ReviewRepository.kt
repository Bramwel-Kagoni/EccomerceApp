package com.bramwel.eccomerceapp.features.reviews.domain.repository

import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.features.reviews.domain.model.MyReview
import com.bramwel.eccomerceapp.features.reviews.domain.model.ProductReviews
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    /** Approved reviews for a product (public). */
    suspend fun getProductReviews(productId: Int): AppResult<ProductReviews>

    /** This customer's reviews (any status). Updated by [refreshMyReviews] and [submitReview]. */
    fun observeMyReviews(): Flow<List<MyReview>>
    suspend fun refreshMyReviews(): AppResult<Unit>

    /** Creates or edits a review; it always goes back to "pending" until approved. */
    suspend fun submitReview(productId: Int, rating: Int, comment: String, reviewerName: String): AppResult<MyReview>
}
