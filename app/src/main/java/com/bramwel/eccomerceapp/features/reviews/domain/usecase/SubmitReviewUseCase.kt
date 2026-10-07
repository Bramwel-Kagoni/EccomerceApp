package com.bramwel.eccomerceapp.features.reviews.domain.usecase

import com.bramwel.eccomerceapp.core.common.AppError
import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.features.reviews.domain.model.MyReview
import com.bramwel.eccomerceapp.features.reviews.domain.repository.ReviewRepository
import javax.inject.Inject

data class ReviewFormErrors(
    val rating: String? = null,
    val comment: String? = null,
    val name: String? = null
) {
    val hasErrors: Boolean get() = rating != null || comment != null || name != null
}

/** Same limits as the backend's ReviewInputSerializer. */
class SubmitReviewUseCase @Inject constructor(
    private val repository: ReviewRepository
) {
    fun validate(rating: Int, comment: String, name: String) = ReviewFormErrors(
        rating = if (rating !in 1..5) "Tap a star to rate this product" else null,
        comment = when {
            comment.trim().length < MIN_COMMENT -> "Tell us a bit more (at least $MIN_COMMENT characters)"
            comment.trim().length > MAX_COMMENT -> "Keep it under $MAX_COMMENT characters"
            else -> null
        },
        name = if (name.trim().length < 2) "Enter the name to show on your review" else null
    )

    suspend operator fun invoke(productId: Int, rating: Int, comment: String, name: String): AppResult<MyReview> {
        if (validate(rating, comment, name).hasErrors) {
            return AppResult.Error(AppError.Validation("Please complete your review."))
        }
        return repository.submitReview(productId, rating, comment.trim(), name.trim())
    }

    companion object {
        const val MIN_COMMENT = 10
        const val MAX_COMMENT = 1000
    }
}
