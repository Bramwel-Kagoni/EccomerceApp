package com.bramwel.eccomerceapp.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bramwel.eccomerceapp.core.common.formatKes
import com.bramwel.eccomerceapp.core.ui.theme.DealRed
import com.bramwel.eccomerceapp.core.ui.theme.EccomerceAppTheme
import com.bramwel.eccomerceapp.core.ui.theme.StarYellow
import java.util.Locale

/**
 * Shared product tile. Takes plain values (not a feature model) so it can be reused by
 * home, catalog, search and wishlist without coupling core to a feature.
 */
@Composable
fun ProductCard(
    title: String,
    imageUrl: String,
    price: Long,
    originalPrice: Long,
    discountPercent: Int,
    rating: Double,
    isWishlisted: Boolean,
    inStock: Boolean,
    onClick: () -> Unit,
    onWishlistClick: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier,
    brand: String? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box {
            NetworkImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
            if (discountPercent > 0) {
                DiscountBadge(
                    percent = discountPercent,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                )
            }
            WishlistButton(
                isWishlisted = isWishlisted,
                onClick = onWishlistClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )
            if (!inStock) {
                Text(
                    text = "Out of stock",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            if (!brand.isNullOrBlank()) {
                Text(
                    brand,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            RatingRow(rating = rating)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PriceText(
                    price = price,
                    originalPrice = originalPrice,
                    modifier = Modifier.weight(1f)
                )
                FilledIconButton(
                    onClick = onAddToCart,
                    enabled = inStock,
                    modifier = Modifier.size(34.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add $title to cart", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun PriceText(
    price: Long,
    originalPrice: Long,
    modifier: Modifier = Modifier,
    priceStyle: TextStyle = MaterialTheme.typography.titleSmall
) {
    Column(modifier) {
        Text(
            price.formatKes(),
            style = priceStyle.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
        if (originalPrice > price) {
            Text(
                originalPrice.formatKes(),
                style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.LineThrough),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun RatingRow(rating: Double, modifier: Modifier = Modifier, reviewCount: Int? = null) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Star, contentDescription = null, tint = StarYellow, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(3.dp))
        Text(
            String.format(Locale.US, "%.1f", rating),
            style = MaterialTheme.typography.labelMedium
        )
        if (reviewCount != null) {
            Text(
                " ($reviewCount reviews)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DiscountBadge(percent: Int, modifier: Modifier = Modifier) {
    Text(
        text = "-$percent%",
        color = Color.White,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        modifier = modifier
            .background(DealRed, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
fun WishlistButton(isWishlisted: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(
        targetValue = if (isWishlisted) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.35f),
        label = "heartScale"
    )
    val tint by animateColorAsState(
        targetValue = if (isWishlisted) DealRed else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "heartTint"
    )
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Icon(
            imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (isWishlisted) "Remove from wishlist" else "Add to wishlist",
            tint = tint,
            modifier = Modifier
                .size(20.dp)
                .scale(scale)
        )
    }
}

@Preview(showBackground = true, widthDp = 190)
@Composable
private fun ProductCardPreview() {
    EccomerceAppTheme {
        ProductCard(
            title = "Essence Mascara Lash Princess",
            imageUrl = "",
            price = 1289,
            originalPrice = 1400,
            discountPercent = 8,
            rating = 4.6,
            isWishlisted = true,
            inStock = true,
            brand = "Essence",
            onClick = {},
            onWishlistClick = {},
            onAddToCart = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 190)
@Composable
private fun ProductCardOutOfStockPreview() {
    EccomerceAppTheme {
        ProductCard(
            title = "Apple MacBook Pro 14 Inch Space Grey",
            imageUrl = "",
            price = 257_999,
            originalPrice = 257_999,
            discountPercent = 0,
            rating = 3.7,
            isWishlisted = false,
            inStock = false,
            onClick = {},
            onWishlistClick = {},
            onAddToCart = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PriceAndRatingPreview() {
    EccomerceAppTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PriceText(price = 1289, originalPrice = 1500)
            RatingRow(rating = 4.56, reviewCount = 3)
            DiscountBadge(percent = 20)
        }
    }
}
