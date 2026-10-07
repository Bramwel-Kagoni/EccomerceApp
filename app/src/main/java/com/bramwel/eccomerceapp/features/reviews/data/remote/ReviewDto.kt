package com.bramwel.eccomerceapp.features.reviews.data.remote

import com.bramwel.eccomerceapp.features.reviews.domain.model.MyReview
import com.bramwel.eccomerceapp.features.reviews.domain.model.ProductReviews
import com.bramwel.eccomerceapp.features.reviews.domain.model.ReviewStatus
import com.bramwel.eccomerceapp.features.reviews.domain.model.StoreReview
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReviewRequestDto(
    @SerialName("product_id") val productId: Int,
    val rating: Int,
    val comment: String,
    @SerialName("reviewer_name") val reviewerName: String
)

@Serializable
data class PublicReviewDto(
    val id: Int,
    @SerialName("product_id") val productId: Int,
    val rating: Int,
    val comment: String = "",
    @SerialName("reviewer_name") val reviewerName: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ProductReviewsDto(
    @SerialName("average_rating") val averageRating: Double = 0.0,
    val count: Int = 0,
    val reviews: List<PublicReviewDto> = emptyList()
)

@Serializable
data class MyReviewDto(
    val id: Int,
    @SerialName("product_id") val productId: Int,
    @SerialName("product_title") val productTitle: String = "",
    @SerialName("product_thumbnail") val productThumbnail: String = "",
    val rating: Int,
    val comment: String = "",
    @SerialName("reviewer_name") val reviewerName: String = "",
    val status: String = "pending",
    @SerialName("moderation_note") val moderationNote: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

fun PublicReviewDto.toDomain() = StoreReview(id, productId, rating, comment, reviewerName, createdAt)

fun ProductReviewsDto.toDomain() = ProductReviews(averageRating, count, reviews.map { it.toDomain() })

fun MyReviewDto.toDomain() = MyReview(
    id = id,
    productId = productId,
    productTitle = productTitle,
    productThumbnail = productThumbnail,
    rating = rating,
    comment = comment,
    reviewerName = reviewerName,
    status = ReviewStatus.fromApi(status),
    moderationNote = moderationNote,
    updatedAt = updatedAt
)
