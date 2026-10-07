package com.bramwel.eccomerceapp.features.products.presentation.details

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AssignmentReturn
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bramwel.eccomerceapp.core.common.formatKes
import com.bramwel.eccomerceapp.core.ui.components.AppButton
import com.bramwel.eccomerceapp.core.ui.components.AppOutlinedButton
import com.bramwel.eccomerceapp.core.ui.components.AppTopBar
import com.bramwel.eccomerceapp.core.ui.components.CartIconButton
import com.bramwel.eccomerceapp.core.ui.components.DiscountBadge
import com.bramwel.eccomerceapp.core.ui.components.EmptyState
import com.bramwel.eccomerceapp.core.ui.components.LoadingIndicator
import com.bramwel.eccomerceapp.core.ui.components.NetworkImage
import com.bramwel.eccomerceapp.core.ui.components.PriceText
import com.bramwel.eccomerceapp.core.ui.components.QuantitySelector
import com.bramwel.eccomerceapp.core.ui.components.RatingRow
import com.bramwel.eccomerceapp.core.ui.components.SectionHeader
import com.bramwel.eccomerceapp.core.ui.theme.DealRed
import com.bramwel.eccomerceapp.core.ui.theme.EccomerceAppTheme
import com.bramwel.eccomerceapp.core.ui.theme.StarYellow
import com.bramwel.eccomerceapp.core.ui.theme.SuccessGreen
import com.bramwel.eccomerceapp.core.ui.theme.WarningAmber
import com.bramwel.eccomerceapp.features.products.domain.model.Product
import com.bramwel.eccomerceapp.features.products.domain.model.Review
import com.bramwel.eccomerceapp.features.products.presentation.PreviewData
import com.bramwel.eccomerceapp.features.products.presentation.components.ProductItem
import com.bramwel.eccomerceapp.features.reviews.domain.model.ProductReviews
import com.bramwel.eccomerceapp.features.reviews.domain.model.ReviewEligibility
import com.bramwel.eccomerceapp.features.reviews.domain.model.StoreReview
import com.bramwel.eccomerceapp.features.reviews.presentation.components.ReviewPromptCard
import com.bramwel.eccomerceapp.features.reviews.presentation.components.StoreReviewItem

@Composable
fun ProductDetailsScreen(
    onBackClick: () -> Unit,
    onProductClick: (Int) -> Unit,
    onCartClick: () -> Unit,
    onCheckout: () -> Unit,
    onWriteReview: (productId: Int, title: String, thumbnail: String) -> Unit,
    viewModel: ProductDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProductDetailsEffect.NavigateToCheckout -> onCheckout()
            }
        }
    }
    ProductDetailsContent(state, viewModel::onEvent, onBackClick, onProductClick, onCartClick, onWriteReview)
}

@Composable
fun ProductDetailsContent(
    state: ProductDetailsUiState,
    onEvent: (ProductDetailsEvent) -> Unit,
    onBackClick: () -> Unit,
    onProductClick: (Int) -> Unit,
    onCartClick: () -> Unit,
    onWriteReview: (Int, String, String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.userMessage) {
        val message = state.userMessage ?: return@LaunchedEffect
        val isCartMessage = message.startsWith("Added")
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = if (isCartMessage) "View cart" else null,
            duration = SnackbarDuration.Short
        )
        onEvent(ProductDetailsEvent.MessageShown)
        if (result == SnackbarResult.ActionPerformed) onCartClick()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Details",
                onBackClick = onBackClick,
                actions = {
                    state.product?.let { product ->
                        IconButton(onClick = {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Check out ${product.title} for ${product.price.formatKes()} on Soko!"
                                )
                            }
                            context.startActivity(Intent.createChooser(share, "Share product"))
                        }) { Icon(Icons.Outlined.Share, contentDescription = "Share") }
                        IconButton(onClick = { onEvent(ProductDetailsEvent.ToggleWishlist(product.id)) }) {
                            Icon(
                                if (state.isWishlisted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Wishlist",
                                tint = if (state.isWishlisted) DealRed else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    CartIconButton(count = state.cartCount, onClick = onCartClick)
                }
            )
        },
        bottomBar = {
            state.product?.let { product ->
                PurchaseBar(product = product, state = state, onEvent = onEvent)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            state.isLoading -> LoadingIndicator(Modifier.padding(padding))
            state.product == null -> EmptyState(
                icon = Icons.Outlined.Inventory2,
                title = "Product not found",
                message = "This product may have been removed from the store.",
                actionLabel = "Go back",
                onAction = onBackClick,
                modifier = Modifier.padding(padding)
            )
            else -> state.product?.let { product ->
                ProductDetailsBody(
                    product = product,
                    state = state,
                    onEvent = onEvent,
                    onProductClick = onProductClick,
                    onWriteReview = { onWriteReview(product.id, product.title, product.thumbnail) },
                    contentPadding = padding
                )
            }
        }
    }
}

@Composable
private fun ProductDetailsBody(
    product: Product,
    state: ProductDetailsUiState,
    onEvent: (ProductDetailsEvent) -> Unit,
    onProductClick: (Int) -> Unit,
    onWriteReview: () -> Unit,
    contentPadding: PaddingValues
) {
    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize()
    ) {
        item { ImageGallery(product) }
        item {
            Column(Modifier.padding(16.dp)) {
                if (!product.brand.isNullOrBlank()) {
                    Text(
                        product.brand.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(product.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                RatingRow(rating = product.rating, reviewCount = product.reviews.size)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PriceText(
                        price = product.price,
                        originalPrice = product.originalPrice,
                        priceStyle = MaterialTheme.typography.headlineSmall
                    )
                    if (product.hasDiscount) {
                        Spacer(Modifier.width(12.dp))
                        DiscountBadge(percent = product.discountPercent)
                    }
                }
                Spacer(Modifier.height(12.dp))
                StockLabel(product = product, inCart = state.inCartQuantity)
            }
        }
        item {
            Row(
                Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quantity", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                QuantitySelector(
                    quantity = state.quantity,
                    onDecrease = { onEvent(ProductDetailsEvent.DecreaseQuantity) },
                    onIncrease = { onEvent(ProductDetailsEvent.IncreaseQuantity) },
                    canIncrease = state.quantity < state.maxQuantity
                )
            }
        }
        item {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoRow(Icons.Outlined.LocalShipping, "Delivery", product.shipping ?: "Delivered in 2–4 business days")
                InfoRow(Icons.Outlined.VerifiedUser, "Warranty", product.warranty ?: "Seller warranty")
                InfoRow(Icons.Outlined.AssignmentReturn, "Returns", product.returnPolicy ?: "7 days return policy")
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("Description", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    product.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Spacer(Modifier.height(20.dp))
            SectionHeader(
                title = "Reviews (${state.totalReviewCount})",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            ReviewPromptCard(
                eligibility = state.reviewEligibility,
                onWriteReview = onWriteReview,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (state.totalReviewCount == 0) {
                Text(
                    "No reviews yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
        items(state.storeReviews.reviews, key = { "store-review-${it.id}" }) { review -> StoreReviewItem(review) }
        items(product.reviews) { review -> ReviewItem(review) }
        if (state.similar.isNotEmpty()) {
            item {
                Spacer(Modifier.height(20.dp))
                SectionHeader(title = "You may also like", modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.similar, key = { it.id }) { similar ->
                        ProductItem(
                            product = similar,
                            isWishlisted = similar.id in state.wishlistIds,
                            onClick = { onProductClick(similar.id) },
                            onWishlistClick = { onEvent(ProductDetailsEvent.ToggleWishlist(similar.id)) },
                            onAddToCart = { onEvent(ProductDetailsEvent.AddSimilarToCart(similar.id)) },
                            modifier = Modifier.width(160.dp)
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageGallery(product: Product) {
    val images = product.images.ifEmpty { listOf(product.thumbnail) }
    val pagerState = rememberPagerState(pageCount = { images.size })
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            NetworkImage(
                model = images[page],
                contentDescription = "${product.title} image ${page + 1}",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentScale = ContentScale.Fit
            )
        }
        if (images.size > 1) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
            ) {
                repeat(images.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        Modifier
                            .padding(3.dp)
                            .size(if (selected) 10.dp else 7.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun StockLabel(product: Product, inCart: Int) {
    val (text, color) = when {
        !product.inStock -> "Out of stock" to DealRed
        product.isLowStock -> "Only ${product.stock} left — order soon" to WarningAmber
        else -> "In stock" to SuccessGreen
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
        if (inCart > 0) {
            Text(
                "  •  $inCart in your cart",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, value: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ReviewItem(review: Review) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    review.reviewerName.take(1),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(review.reviewerName, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            repeat(5) { index ->
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = if (index < review.rating) StarYellow else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(review.comment, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun PurchaseBar(product: Product, state: ProductDetailsUiState, onEvent: (ProductDetailsEvent) -> Unit) {
    Surface(shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(0.8f)) {
                Text("Total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    (product.price * state.quantity).formatKes(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            val canBuy = product.inStock && (state.maxQuantity > 0 || state.inCartQuantity > 0)
            AppOutlinedButton(
                text = "Add",
                onClick = { onEvent(ProductDetailsEvent.AddToCart) },
                enabled = product.inStock && state.maxQuantity > 0,
                leadingIcon = Icons.Outlined.ShoppingCart,
                modifier = Modifier.weight(1f)
            )
            AppButton(
                text = "Buy now",
                onClick = { onEvent(ProductDetailsEvent.BuyNow) },
                enabled = canBuy,
                loading = state.isAddingToCart,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 1500)
@Composable
private fun ProductDetailsPreview() {
    EccomerceAppTheme {
        ProductDetailsContent(
            state = ProductDetailsUiState(
                isLoading = false,
                product = PreviewData.products.first(),
                quantity = 2,
                inCartQuantity = 1,
                similar = PreviewData.products.drop(1),
                cartCount = 3,
                reviewEligibility = ReviewEligibility.CanReview,
                storeReviews = ProductReviews(
                    averageRating = 5.0,
                    count = 1,
                    reviews = listOf(StoreReview(1, 1, 5, "Lasts all day, would buy again!", "Kagoni L.", "2026-09-26T10:00:00+03:00"))
                )
            ),
            onEvent = {}, onBackClick = {}, onProductClick = {}, onCartClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProductNotFoundPreview() {
    EccomerceAppTheme {
        ProductDetailsContent(
            state = ProductDetailsUiState(isLoading = false),
            onEvent = {}, onBackClick = {}, onProductClick = {}, onCartClick = {}
        )
    }
}
