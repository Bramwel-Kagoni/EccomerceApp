package com.bramwel.eccomerceapp.features.reviews.domain.model

/** An approved, purchase-verified review from our own store. */
data class StoreReview(
    val id: Int,
    val productId: Int,
    val rating: Int,
    val comment: String,
    val reviewerName: String,
    val createdAt: String
)

data class ProductReviews(
    val averageRating: Double,
    val count: Int,
    val reviews: List<StoreReview>
) {
    companion object {
        val Empty = ProductReviews(0.0, 0, emptyList())
    }
}

/** A review written by this customer, including its moderation state. */
data class MyReview(
    val id: Int,
    val productId: Int,
    val productTitle: String,
    val productThumbnail: String,
    val rating: Int,
    val comment: String,
    val reviewerName: String,
    val status: ReviewStatus,
    val moderationNote: String,
    val updatedAt: String
)

enum class ReviewStatus(val label: String) {
    PENDING("Awaiting approval"),
    APPROVED("Published"),
    REJECTED("Not approved");

    companion object {
        fun fromApi(value: String): ReviewStatus = when (value) {
            "approved" -> APPROVED
            "rejected" -> REJECTED
            else -> PENDING
        }
    }
}

/** Whether the customer may review a product, and what they already wrote. */
sealed interface ReviewEligibility {
    data object NotPurchased : ReviewEligibility
    data object CanReview : ReviewEligibility
    data class Reviewed(val review: MyReview) : ReviewEligibility
}

/** A product the customer has paid for, with their review if they wrote one. */
data class ReviewableItem(
    val productId: Int,
    val title: String,
    val thumbnail: String,
    val review: MyReview?
)
