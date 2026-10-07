package com.bramwel.eccomerceapp.features.reviews.presentation.write

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bramwel.eccomerceapp.core.ui.components.AppButton
import com.bramwel.eccomerceapp.core.ui.components.AppOutlinedButton
import com.bramwel.eccomerceapp.core.ui.components.AppTextField
import com.bramwel.eccomerceapp.core.ui.components.AppTopBar
import com.bramwel.eccomerceapp.core.ui.components.LoadingIndicator
import com.bramwel.eccomerceapp.core.ui.components.NetworkImage
import com.bramwel.eccomerceapp.core.ui.theme.EccomerceAppTheme
import com.bramwel.eccomerceapp.core.ui.theme.SuccessGreen
import com.bramwel.eccomerceapp.features.reviews.domain.usecase.ReviewFormErrors
import com.bramwel.eccomerceapp.features.reviews.domain.usecase.SubmitReviewUseCase
import com.bramwel.eccomerceapp.features.reviews.presentation.components.StarRatingInput
import com.bramwel.eccomerceapp.features.reviews.presentation.components.ratingLabels

@Composable
fun WriteReviewScreen(
    onBackClick: () -> Unit,
    onMyReviewsClick: () -> Unit,
    viewModel: WriteReviewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WriteReviewContent(state, viewModel::onEvent, onBackClick, onMyReviewsClick)
}

@Composable
fun WriteReviewContent(
    state: WriteReviewUiState,
    onEvent: (WriteReviewEvent) -> Unit,
    onBackClick: () -> Unit,
    onMyReviewsClick: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onEvent(WriteReviewEvent.ErrorShown)
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = if (state.isEditing) "Edit review" else "Write a review", onBackClick = onBackClick) },
        bottomBar = {
            if (!state.isLoading && !state.submitted) {
                AppButton(
                    text = "Submit for approval",
                    onClick = { onEvent(WriteReviewEvent.Submit) },
                    loading = state.isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            state.isLoading -> LoadingIndicator(Modifier.padding(padding))
            state.submitted -> SubmittedState(onBackClick, onMyReviewsClick, Modifier.padding(padding))
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProductHeader(state.productTitle, state.productThumbnail)

                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("How would you rate it?", style = MaterialTheme.typography.titleMedium)
                    StarRatingInput(rating = state.rating, onRatingChange = { onEvent(WriteReviewEvent.RatingChanged(it)) })
                    Text(
                        state.errors.rating ?: ratingLabels.getOrElse(state.rating) { "" },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (state.errors.rating != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                AppTextField(
                    value = state.comment,
                    onValueChange = { onEvent(WriteReviewEvent.CommentChanged(it)) },
                    label = "Your review",
                    placeholder = "What did you like or dislike? How was the quality?",
                    singleLine = false,
                    minLines = 5,
                    error = state.errors.comment,
                    supportingText = "${state.comment.length}/${SubmitReviewUseCase.MAX_COMMENT}",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                AppTextField(
                    value = state.reviewerName,
                    onValueChange = { onEvent(WriteReviewEvent.NameChanged(it)) },
                    label = "Name shown on review",
                    leadingIcon = Icons.Outlined.Person,
                    error = state.errors.name,
                    supportingText = "Only your first name and last initial are shown",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (state.isEditing) "Edited reviews are checked again by our team before they appear."
                            else "Reviews are checked by our team before they appear on the product page.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductHeader(title: String, thumbnail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        NetworkImage(
            model = thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(64.dp)
                .clip(MaterialTheme.shapes.medium)
        )
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SubmittedState(onDone: () -> Unit, onMyReviewsClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(96.dp))
        Spacer(Modifier.height(16.dp))
        Text("Thanks for your review!", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "It's now awaiting approval and will appear on the product page once our team has checked it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        AppButton(text = "Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        AppOutlinedButton(text = "View my reviews", onClick = onMyReviewsClick, modifier = Modifier.fillMaxWidth())
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WriteReviewPreview() {
    EccomerceAppTheme {
        WriteReviewContent(
            state = WriteReviewUiState(
                productTitle = "Essence Mascara Lash Princess",
                isLoading = false,
                rating = 4,
                comment = "Lasts all day and doesn't smudge.",
                reviewerName = "Kagoni Livwege"
            ),
            onEvent = {}, onBackClick = {}, onMyReviewsClick = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WriteReviewErrorsPreview() {
    EccomerceAppTheme {
        WriteReviewContent(
            state = WriteReviewUiState(
                productTitle = "Apple Airpods",
                isLoading = false,
                comment = "ok",
                errors = ReviewFormErrors(rating = "Tap a star to rate this product", comment = "Tell us a bit more (at least 10 characters)")
            ),
            onEvent = {}, onBackClick = {}, onMyReviewsClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WriteReviewSubmittedPreview() {
    EccomerceAppTheme {
        WriteReviewContent(WriteReviewUiState(isLoading = false, submitted = true), {}, {}, {})
    }
}
