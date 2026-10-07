package com.bramwel.eccomerceapp.features.reviews

import com.bramwel.eccomerceapp.core.common.AppError
import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.features.orders.domain.model.CartLine
import com.bramwel.eccomerceapp.features.orders.domain.model.DeliveryDetails
import com.bramwel.eccomerceapp.features.orders.domain.model.DeliveryMethod
import com.bramwel.eccomerceapp.features.orders.domain.model.Order
import com.bramwel.eccomerceapp.features.orders.domain.model.OrderQuote
import com.bramwel.eccomerceapp.features.orders.domain.repository.OrderRepository
import com.bramwel.eccomerceapp.features.orders.presentation.OrderPreviewData
import com.bramwel.eccomerceapp.features.reviews.domain.model.MyReview
import com.bramwel.eccomerceapp.features.reviews.domain.model.ProductReviews
import com.bramwel.eccomerceapp.features.reviews.domain.model.ReviewEligibility
import com.bramwel.eccomerceapp.features.reviews.domain.model.ReviewStatus
import com.bramwel.eccomerceapp.features.reviews.domain.repository.ReviewRepository
import com.bramwel.eccomerceapp.features.reviews.domain.usecase.ObserveReviewablesUseCase
import com.bramwel.eccomerceapp.features.reviews.domain.usecase.SubmitReviewUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewUseCasesTest {

    private class FakeReviews : ReviewRepository {
        val mine = MutableStateFlow<List<MyReview>>(emptyList())
        var submitted = 0
        override suspend fun getProductReviews(productId: Int) = AppResult.Success(ProductReviews.Empty)
        override fun observeMyReviews(): Flow<List<MyReview>> = mine
        override suspend fun refreshMyReviews(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun submitReview(productId: Int, rating: Int, comment: String, reviewerName: String): AppResult<MyReview> {
            submitted++
            return AppResult.Success(review(productId, ReviewStatus.PENDING))
        }
    }

    private class FakeOrders(private val orders: List<Order>) : OrderRepository {
        override suspend fun getQuote(lines: List<CartLine>, deliveryMethod: DeliveryMethod, promoCode: String):
            AppResult<OrderQuote> = AppResult.Error(AppError.Unknown())
        override suspend fun placeOrder(
            lines: List<CartLine>, details: DeliveryDetails, deliveryMethod: DeliveryMethod, promoCode: String
        ): AppResult<Order> = AppResult.Error(AppError.Unknown())
        override fun observeOrders(): Flow<List<Order>> = flowOf(orders)
        override fun observeOrder(orderId: String): Flow<Order?> = flowOf(null)
        override suspend fun refreshOrders(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun refreshOrder(orderId: String): AppResult<Order> = AppResult.Error(AppError.Unknown())
    }

    companion object {
        fun review(productId: Int, status: ReviewStatus) =
            MyReview(1, productId, "P", "", 5, "Great product!!", "Kagoni", status, "", "")
    }

    @Test
    fun `validation mirrors the backend limits`() {
        val submit = SubmitReviewUseCase(FakeReviews())
        assertTrue(submit.validate(0, "Great product!!", "Kagoni").rating != null)
        assertTrue(submit.validate(5, "short", "Kagoni").comment != null)
        assertTrue(submit.validate(5, "Great product!!", "K").name != null)
        assertFalse(submit.validate(5, "Great product!!", "Kagoni").hasErrors)
    }

    @Test
    fun `invalid review never reaches the server`() = runTest {
        val repo = FakeReviews()
        val result = SubmitReviewUseCase(repo)(1, 0, "short", "K")
        assertTrue(result is AppResult.Error)
        assertEquals(0, repo.submitted)
    }

    @Test
    fun `only paid products are reviewable`() = runTest {
        val paid = OrderPreviewData.paidOrder          // items 1 and 6
        val pending = OrderPreviewData.pendingOrder.copy(
            items = listOf(paid.items.first().copy(productId = 99))
        )
        val reviews = FakeReviews()
        val useCase = ObserveReviewablesUseCase(FakeOrders(listOf(paid, pending)), reviews)

        assertEquals(listOf(1, 6), useCase().first().map { it.productId })
        assertEquals(ReviewEligibility.CanReview, useCase.eligibility(1).first())
        assertEquals(ReviewEligibility.NotPurchased, useCase.eligibility(99).first())

        reviews.mine.value = listOf(review(1, ReviewStatus.PENDING))
        val state = useCase.eligibility(1).first()
        assertTrue(state is ReviewEligibility.Reviewed && state.review.status == ReviewStatus.PENDING)
    }
}
