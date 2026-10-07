package com.bramwel.eccomerceapp.features.reviews.presentation.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.core.common.toUserMessage
import com.bramwel.eccomerceapp.features.orders.domain.repository.OrderRepository
import com.bramwel.eccomerceapp.features.reviews.domain.model.ReviewableItem
import com.bramwel.eccomerceapp.features.reviews.domain.repository.ReviewRepository
import com.bramwel.eccomerceapp.features.reviews.domain.usecase.ObserveReviewablesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyReviewsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    /** Paid products without a review yet. */
    val toReview: List<ReviewableItem> = emptyList(),
    /** Products the customer already reviewed (any status). */
    val reviewed: List<ReviewableItem> = emptyList(),
    val errorMessage: String? = null
) {
    val isEmpty: Boolean get() = toReview.isEmpty() && reviewed.isEmpty()
}

@HiltViewModel
class MyReviewsViewModel @Inject constructor(
    observeReviewables: ObserveReviewablesUseCase,
    private val reviewRepository: ReviewRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val refreshing = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MyReviewsUiState> = combine(observeReviewables(), refreshing, error) { items, isRefreshing, err ->
        val (reviewed, toReview) = items.partition { it.review != null }
        MyReviewsUiState(
            isLoading = isRefreshing && items.isEmpty(),
            isRefreshing = isRefreshing && items.isNotEmpty(),
            toReview = toReview,
            reviewed = reviewed.sortedByDescending { it.review?.updatedAt },
            errorMessage = err
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyReviewsUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            val orders = async { orderRepository.refreshOrders() }
            val reviews = async { reviewRepository.refreshMyReviews() }
            val failure = listOf(orders.await(), reviews.await()).filterIsInstance<AppResult.Error>().firstOrNull()
            error.value = failure?.error?.toUserMessage()
            refreshing.value = false
        }
    }
}
