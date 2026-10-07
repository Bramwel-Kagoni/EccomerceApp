package com.bramwel.eccomerceapp.features.reviews.domain.usecase

import com.bramwel.eccomerceapp.features.orders.domain.model.OrderStatus
import com.bramwel.eccomerceapp.features.orders.domain.repository.OrderRepository
import com.bramwel.eccomerceapp.features.reviews.domain.model.ReviewEligibility
import com.bramwel.eccomerceapp.features.reviews.domain.model.ReviewableItem
import com.bramwel.eccomerceapp.features.reviews.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Products the customer has paid for, joined with the reviews they already wrote.
 * The backend enforces the same "paid orders only" rule.
 */
class ObserveReviewablesUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
    private val reviewRepository: ReviewRepository
) {
    operator fun invoke(): Flow<List<ReviewableItem>> =
        combine(orderRepository.observeOrders(), reviewRepository.observeMyReviews()) { orders, reviews ->
            val reviewsByProduct = reviews.associateBy { it.productId }
            orders.filter { it.status == OrderStatus.PAID }
                .flatMap { it.items }
                .distinctBy { it.productId }
                .map { item ->
                    ReviewableItem(item.productId, item.title, item.thumbnail, reviewsByProduct[item.productId])
                }
        }

    fun eligibility(productId: Int): Flow<ReviewEligibility> = invoke().map { items ->
        val item = items.firstOrNull { it.productId == productId }
        when {
            item == null -> ReviewEligibility.NotPurchased
            item.review != null -> ReviewEligibility.Reviewed(item.review)
            else -> ReviewEligibility.CanReview
        }
    }
}
