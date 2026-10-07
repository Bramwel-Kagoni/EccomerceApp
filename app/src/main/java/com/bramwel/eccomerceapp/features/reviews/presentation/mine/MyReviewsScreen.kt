package com.bramwel.eccomerceapp.features.reviews.presentation.mine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bramwel.eccomerceapp.core.ui.components.AppTopBar
import com.bramwel.eccomerceapp.core.ui.components.EmptyState
import com.bramwel.eccomerceapp.core.ui.components.LoadingIndicator
import com.bramwel.eccomerceapp.core.ui.components.NetworkImage
import com.bramwel.eccomerceapp.core.ui.theme.DealRed
import com.bramwel.eccomerceapp.core.ui.theme.EccomerceAppTheme
import com.bramwel.eccomerceapp.features.reviews.domain.model.MyReview
import com.bramwel.eccomerceapp.features.reviews.domain.model.ReviewStatus
import com.bramwel.eccomerceapp.features.reviews.domain.model.ReviewableItem
import com.bramwel.eccomerceapp.features.reviews.presentation.components.ReviewStatusChip
import com.bramwel.eccomerceapp.features.reviews.presentation.components.StarRow

@Composable
fun MyReviewsScreen(
    onBackClick: () -> Unit,
    onWriteReview: (productId: Int, title: String, thumbnail: String) -> Unit,
    onStartShopping: () -> Unit,
    viewModel: MyReviewsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MyReviewsContent(state, onBackClick, onWriteReview, onStartShopping, onRefresh = viewModel::refresh)
}

@Composable
fun MyReviewsContent(
    state: MyReviewsUiState,
    onBackClick: () -> Unit,
    onWriteReview: (Int, String, String) -> Unit,
    onStartShopping: () -> Unit,
    onRefresh: () -> Unit
) {
    Scaffold(topBar = { AppTopBar(title = "My reviews", onBackClick = onBackClick) }) { padding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when {
            state.isLoading -> LoadingIndicator(modifier)
            state.isEmpty -> EmptyState(
                icon = Icons.Outlined.RateReview,
                title = "Nothing to review yet",
                message = state.errorMessage ?: "Products you buy and pay for will appear here so you can review them.",
                actionLabel = if (state.errorMessage != null) "Try again" else "Start shopping",
                onAction = if (state.errorMessage != null) onRefresh else onStartShopping,
                modifier = modifier
            )
            else -> PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh, modifier = modifier) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (state.toReview.isNotEmpty()) {
                        item { SectionTitle("Waiting for your review (${state.toReview.size})") }
                        items(state.toReview, key = { "to-${it.productId}" }) { item ->
                            ReviewableCard(item) { onWriteReview(item.productId, item.title, item.thumbnail) }
                        }
                    }
                    if (state.reviewed.isNotEmpty()) {
                        item { SectionTitle("Your reviews") }
                        items(state.reviewed, key = { "rev-${it.productId}" }) { item ->
                            ReviewableCard(item) { onWriteReview(item.productId, item.title, item.thumbnail) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun ReviewableCard(item: ReviewableItem, onWrite: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NetworkImage(
                    model = item.thumbnail.ifBlank { item.review?.productThumbnail },
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    item.review?.let {
                        Spacer(Modifier.height(4.dp))
                        ReviewStatusChip(it.status)
                    }
                }
                if (item.review == null) {
                    FilledTonalButton(onClick = onWrite) { Text("Review") }
                }
            }
            item.review?.let { review -> ReviewBody(review, onWrite) }
        }
    }
}

@Composable
private fun ReviewBody(review: MyReview, onEdit: () -> Unit) {
    Column(Modifier.padding(top = 10.dp)) {
        StarRow(review.rating, size = 16.dp)
        Spacer(Modifier.height(4.dp))
        Text(review.comment, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
        if (review.status == ReviewStatus.REJECTED && review.moderationNote.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(review.moderationNote, style = MaterialTheme.typography.bodySmall, color = DealRed)
        }
        TextButton(onClick = onEdit, contentPadding = PaddingValues(0.dp)) {
            Text(if (review.status == ReviewStatus.REJECTED) "Edit & resubmit" else "Edit review")
        }
    }
}

private fun preview(status: ReviewStatus?, id: Int, title: String) = ReviewableItem(
    productId = id,
    title = title,
    thumbnail = "",
    review = status?.let {
        MyReview(id, id, title, "", 4, "Really good quality, arrived fast.", "Kagoni L.", it,
            if (it == ReviewStatus.REJECTED) "Please don't include phone numbers." else "", "2026-09-26")
    }
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun MyReviewsPreview() {
    EccomerceAppTheme {
        MyReviewsContent(
            state = MyReviewsUiState(
                isLoading = false,
                toReview = listOf(preview(null, 1, "Apple Airpods")),
                reviewed = listOf(
                    preview(ReviewStatus.PENDING, 2, "Essence Mascara Lash Princess"),
                    preview(ReviewStatus.APPROVED, 3, "iPhone 13 Pro"),
                    preview(ReviewStatus.REJECTED, 4, "Gucci Bloom Eau de")
                )
            ),
            onBackClick = {}, onWriteReview = { _, _, _ -> }, onStartShopping = {}, onRefresh = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MyReviewsEmptyPreview() {
    EccomerceAppTheme {
        MyReviewsContent(MyReviewsUiState(isLoading = false), {}, { _, _, _ -> }, {}, {})
    }
}
