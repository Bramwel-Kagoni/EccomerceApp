package com.bramwel.eccomerceapp.features.reviews.presentation.write

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.core.common.toUserMessage
import com.bramwel.eccomerceapp.features.profile.domain.repository.ProfileRepository
import com.bramwel.eccomerceapp.features.reviews.domain.model.MyReview
import com.bramwel.eccomerceapp.features.reviews.domain.repository.ReviewRepository
import com.bramwel.eccomerceapp.features.reviews.domain.usecase.ReviewFormErrors
import com.bramwel.eccomerceapp.features.reviews.domain.usecase.SubmitReviewUseCase
import com.bramwel.eccomerceapp.navigation.AppRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WriteReviewUiState(
    val productTitle: String = "",
    val productThumbnail: String = "",
    val isLoading: Boolean = true,
    val rating: Int = 0,
    val comment: String = "",
    val reviewerName: String = "",
    val errors: ReviewFormErrors = ReviewFormErrors(),
    val existing: MyReview? = null,
    val isSubmitting: Boolean = false,
    val submitted: Boolean = false,
    val errorMessage: String? = null
) {
    val isEditing: Boolean get() = existing != null
}

sealed interface WriteReviewEvent {
    data class RatingChanged(val rating: Int) : WriteReviewEvent
    data class CommentChanged(val comment: String) : WriteReviewEvent
    data class NameChanged(val name: String) : WriteReviewEvent
    data object Submit : WriteReviewEvent
    data object ErrorShown : WriteReviewEvent
}

@HiltViewModel
class WriteReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reviewRepository: ReviewRepository,
    private val profileRepository: ProfileRepository,
    private val submitReview: SubmitReviewUseCase
) : ViewModel() {

    private val route = savedStateHandle.toRoute<AppRoute.WriteReview>()

    private val _uiState = MutableStateFlow(
        WriteReviewUiState(productTitle = route.productTitle, productThumbnail = route.productThumbnail)
    )
    val uiState: StateFlow<WriteReviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            reviewRepository.refreshMyReviews() // best effort: pre-fill an existing review
            val existing = reviewRepository.observeMyReviews().first().firstOrNull { it.productId == route.productId }
            val profileName = profileRepository.observeProfile().first().name
            _uiState.update {
                it.copy(
                    isLoading = false,
                    existing = existing,
                    rating = existing?.rating ?: 0,
                    comment = existing?.comment.orEmpty(),
                    reviewerName = existing?.reviewerName ?: profileName
                )
            }
        }
    }

    fun onEvent(event: WriteReviewEvent) {
        when (event) {
            is WriteReviewEvent.RatingChanged -> _uiState.update {
                it.copy(rating = event.rating, errors = it.errors.copy(rating = null))
            }
            is WriteReviewEvent.CommentChanged -> _uiState.update {
                it.copy(comment = event.comment.take(SubmitReviewUseCase.MAX_COMMENT), errors = it.errors.copy(comment = null))
            }
            is WriteReviewEvent.NameChanged -> _uiState.update {
                it.copy(reviewerName = event.name, errors = it.errors.copy(name = null))
            }
            WriteReviewEvent.Submit -> submit()
            WriteReviewEvent.ErrorShown -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun submit() {
        val state = _uiState.value
        val errors = submitReview.validate(state.rating, state.comment, state.reviewerName)
        if (errors.hasErrors) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val result = submitReview(route.productId, state.rating, state.comment, state.reviewerName)) {
                is AppResult.Success -> _uiState.update { it.copy(isSubmitting = false, submitted = true) }
                is AppResult.Error -> _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = result.error.toUserMessage())
                }
            }
        }
    }
}
