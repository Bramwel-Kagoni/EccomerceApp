package com.bramwel.eccomerceapp.features.reviews.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bramwel.eccomerceapp.core.common.DateFormatter
import com.bramwel.eccomerceapp.core.ui.theme.DealRed
import com.bramwel.eccomerceapp.core.ui.theme.EccomerceAppTheme
import com.bramwel.eccomerceapp.core.ui.theme.StarYellow
import com.bramwel.eccomerceapp.core.ui.theme.SuccessGreen
import com.bramwel.eccomerceapp.core.ui.theme.WarningAmber
import com.bramwel.eccomerceapp.features.reviews.domain.model.MyReview
import com.bramwel.eccomerceapp.features.reviews.domain.model.ReviewEligibility
import com.bramwel.eccomerceapp.features.reviews.domain.model.ReviewStatus
import com.bramwel.eccomerceapp.features.reviews.domain.model.StoreReview

val ratingLabels = listOf("", "Poor", "Fair", "Good", "Very good", "Excellent")

/** Tappable 1–5 star picker. */
@Composable
fun StarRatingInput(rating: Int, onRatingChange: (Int) -> Unit, modifier: Modifier = Modifier, starSize: Dp = 44.dp) {
    Row(modifier, horizontalArrangement = Arrangement.Center) {
        (1..5).forEach { star ->
            IconButton(onClick = { onRatingChange(star) }, modifier = Modifier.size(starSize + 8.dp)) {
                Icon(
                    imageVector = if (star <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "$star star${if (star > 1) "s" else ""}",
                    tint = if (star <= rating) StarYellow else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(starSize)
                )
            }
        }
    }
}

@Composable
fun StarRow(rating: Int, size: Dp = 14.dp) {
    Row {
        repeat(5) { index ->
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = if (index < rating) StarYellow else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(size)
            )
        }
    }
}

@Composable
fun ReviewStatusChip(status: ReviewStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        ReviewStatus.PENDING -> WarningAmber
        ReviewStatus.APPROVED -> SuccessGreen
        ReviewStatus.REJECTED -> DealRed
    }
    Text(
        status.label,
        color = color,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/** An approved store review with the "Verified purchase" badge. */
@Composable
fun StoreReviewItem(review: StoreReview, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(review.reviewerName.take(1), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(review.reviewerName, style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Verified purchase", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                }
            }
            StarRow(review.rating)
        }
        Spacer(Modifier.height(6.dp))
        Text(review.comment, style = MaterialTheme.typography.bodyMedium)
        Text(
            DateFormatter.displayDate(review.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Shown on the product page: invites buyers to review, or shows the state of their review. */
@Composable
fun ReviewPromptCard(
    eligibility: ReviewEligibility,
    onWriteReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, message, action) = when (eligibility) {
        ReviewEligibility.NotPurchased -> return
        ReviewEligibility.CanReview -> Triple("You bought this", "Help other shoppers by sharing your experience.", "Write a review")
        is ReviewEligibility.Reviewed -> when (eligibility.review.status) {
            ReviewStatus.PENDING -> Triple("Thanks for your review!", "It will appear here once our team approves it.", "Edit review")
            ReviewStatus.APPROVED -> Triple("Your review is live", "Thanks for helping other shoppers.", "Edit review")
            ReviewStatus.REJECTED -> Triple(
                "Your review wasn't approved",
                eligibility.review.moderationNote.ifBlank { "Please edit it and submit again." },
                "Edit & resubmit"
            )
        }
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    if (eligibility is ReviewEligibility.Reviewed) {
                        Spacer(Modifier.width(8.dp))
                        ReviewStatusChip(eligibility.review.status)
                    }
                }
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onWriteReview) { Text(action) }
        }
    }
}

private val previewReview = MyReview(1, 1, "Essence Mascara", "", 4, "Lasts all day, would buy again.", "Kagoni L.", ReviewStatus.PENDING, "", "")

@Preview(showBackground = true)
@Composable
private fun ReviewComponentsPreview() {
    EccomerceAppTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StarRatingInput(rating = 4, onRatingChange = {})
            ReviewPromptCard(ReviewEligibility.CanReview, onWriteReview = {})
            ReviewPromptCard(ReviewEligibility.Reviewed(previewReview), onWriteReview = {})
            ReviewPromptCard(
                ReviewEligibility.Reviewed(previewReview.copy(status = ReviewStatus.REJECTED, moderationNote = "Please avoid personal details.")),
                onWriteReview = {}
            )
            StoreReviewItem(StoreReview(1, 1, 5, "Arrived quickly and works great!", "Achieng O.", "2026-09-26T10:00:00+03:00"))
            Row { ReviewStatus.entries.forEach { ReviewStatusChip(it, Modifier.padding(end = 6.dp)) } }
        }
    }
}

