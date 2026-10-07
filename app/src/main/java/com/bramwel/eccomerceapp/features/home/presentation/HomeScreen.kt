package com.bramwel.eccomerceapp.features.home.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bramwel.eccomerceapp.core.ui.components.CartIconButton
import com.bramwel.eccomerceapp.core.ui.components.ErrorView
import com.bramwel.eccomerceapp.core.ui.components.NetworkImage
import com.bramwel.eccomerceapp.core.ui.components.OfflineBanner
import com.bramwel.eccomerceapp.core.ui.components.ProductGridSkeleton
import com.bramwel.eccomerceapp.core.ui.components.SectionHeader
import com.bramwel.eccomerceapp.core.ui.theme.BrandCoral
import com.bramwel.eccomerceapp.core.ui.theme.BrandIndigo
import com.bramwel.eccomerceapp.core.ui.theme.BrandIndigoDark
import com.bramwel.eccomerceapp.core.ui.theme.DealRed
import com.bramwel.eccomerceapp.core.ui.theme.EccomerceAppTheme
import com.bramwel.eccomerceapp.core.ui.theme.MpesaGreen
import com.bramwel.eccomerceapp.features.products.domain.model.Product
import com.bramwel.eccomerceapp.features.products.domain.model.ProductCollection
import com.bramwel.eccomerceapp.features.products.presentation.PreviewData
import com.bramwel.eccomerceapp.features.products.presentation.components.CategoryBubble
import com.bramwel.eccomerceapp.features.products.presentation.components.ProductItem
import kotlinx.coroutines.delay
import java.io.File
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen(
    onProductClick: (Int) -> Unit,
    onCategoryClick: (slug: String, name: String) -> Unit,
    onCollectionClick: (ProductCollection) -> Unit,
    onSeeAllCategories: () -> Unit,
    onSearchClick: () -> Unit,
    onCartClick: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onEvent = viewModel::onEvent,
        onProductClick = onProductClick,
        onCategoryClick = onCategoryClick,
        onCollectionClick = onCollectionClick,
        onSeeAllCategories = onSeeAllCategories,
        onSearchClick = onSearchClick,
        onCartClick = onCartClick,
        onProfileClick = onProfileClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    onProductClick: (Int) -> Unit,
    onCategoryClick: (String, String) -> Unit,
    onCollectionClick: (ProductCollection) -> Unit,
    onSeeAllCategories: () -> Unit,
    onSearchClick: () -> Unit,
    onCartClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            onEvent(HomeEvent.MessageShown)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .statusBarsPadding()
        ) {
            HomeHeader(
                firstName = state.userFirstName,
                initials = state.userInitials,
                photoPath = state.userPhotoPath,
                cartCount = state.cartCount,
                onCartClick = onCartClick,
                onProfileClick = onProfileClick
            )
            SearchField(onClick = onSearchClick)
            OfflineBanner(visible = state.isOffline)

            when {
                state.isLoading -> ProductGridSkeleton()
                state.isEmpty && state.errorMessage != null -> ErrorView(
                    message = state.errorMessage,
                    onRetry = { onEvent(HomeEvent.Retry) },
                    icon = Icons.Outlined.CloudOff
                )
                else -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { onEvent(HomeEvent.Refresh) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    HomeFeed(
                        state = state,
                        onEvent = onEvent,
                        onProductClick = onProductClick,
                        onCategoryClick = onCategoryClick,
                        onCollectionClick = onCollectionClick,
                        onSeeAllCategories = onSeeAllCategories
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeFeed(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    onProductClick: (Int) -> Unit,
    onCategoryClick: (String, String) -> Unit,
    onCollectionClick: (ProductCollection) -> Unit,
    onSeeAllCategories: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        fullWidth("banners") {
            PromoCarousel(onBannerClick = { banner ->
                when (val action = banner.action) {
                    is BannerAction.Collection -> onCollectionClick(action.collection)
                    is BannerAction.Category -> onCategoryClick(action.slug, action.name)
                }
            })
        }

        if (state.categories.isNotEmpty()) {
            fullWidth("categoriesHeader") {
                SectionHeader(
                    title = "Categories",
                    actionLabel = "See all",
                    onAction = onSeeAllCategories
                )
            }
            fullWidth("categories") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.categories, key = { it.slug }) { category ->
                        CategoryBubble(category, onClick = { onCategoryClick(category.slug, category.name) })
                    }
                }
            }
        }

        productRow(
            key = "deals",
            title = "Flash deals",
            products = state.flashDeals,
            state = state,
            onEvent = onEvent,
            onProductClick = onProductClick,
            onSeeAll = { onCollectionClick(ProductCollection.DEALS) },
            titleTrailing = { CountdownChip() }
        )
        productRow(
            key = "topRated",
            title = "Top rated",
            products = state.topRated,
            state = state,
            onEvent = onEvent,
            onProductClick = onProductClick,
            onSeeAll = { onCollectionClick(ProductCollection.TOP_RATED) }
        )

        fullWidth("recommendedHeader") {
            SectionHeader(
                title = "Recommended for you",
                actionLabel = "See all",
                onAction = { onCollectionClick(ProductCollection.ALL) }
            )
        }
        items(state.recommended, key = { "rec-${it.id}" }) { product ->
            ProductItem(
                product = product,
                isWishlisted = product.id in state.wishlistIds,
                onClick = { onProductClick(product.id) },
                onWishlistClick = { onEvent(HomeEvent.ToggleWishlist(product.id)) },
                onAddToCart = { onEvent(HomeEvent.AddToCart(product.id)) }
            )
        }
    }
}

private fun LazyGridScope.fullWidth(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

private fun LazyGridScope.productRow(
    key: String,
    title: String,
    products: List<Product>,
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    onProductClick: (Int) -> Unit,
    onSeeAll: () -> Unit,
    titleTrailing: (@Composable () -> Unit)? = null
) {
    if (products.isEmpty()) return
    fullWidth("${key}Header") {
        SectionHeader(title = title, actionLabel = "See all", onAction = onSeeAll, trailing = titleTrailing)
    }
    fullWidth(key) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(products, key = { "$key-${it.id}" }) { product ->
                ProductItem(
                    product = product,
                    isWishlisted = product.id in state.wishlistIds,
                    onClick = { onProductClick(product.id) },
                    onWishlistClick = { onEvent(HomeEvent.ToggleWishlist(product.id)) },
                    onAddToCart = { onEvent(HomeEvent.AddToCart(product.id)) },
                    modifier = Modifier.width(168.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    firstName: String,
    initials: String,
    photoPath: String?,
    cartCount: Int,
    onCartClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            if (photoPath != null) {
                NetworkImage(model = File(photoPath), contentDescription = "Profile photo", modifier = Modifier.fillMaxSize())
            } else {
                Text(initials, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Hello, $firstName",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "What are you shopping for today?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        CartIconButton(count = cartCount, onClick = onCartClick)
    }
}

@Composable
private fun SearchField(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(
                "Search products, brands…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Outlined.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

// ---- Promo banners ---------------------------------------------------------

private sealed interface BannerAction {
    data class Collection(val collection: ProductCollection) : BannerAction
    data class Category(val slug: String, val name: String) : BannerAction
}

private data class PromoBanner(
    val title: String,
    val subtitle: String,
    val cta: String,
    val icon: ImageVector,
    val colors: List<Color>,
    val action: BannerAction
)

private val banners = listOf(
    PromoBanner(
        "Flash Sale", "Up to 25% off selected items today", "Shop deals", Icons.Outlined.Bolt,
        listOf(BrandIndigo, BrandIndigoDark), BannerAction.Collection(ProductCollection.DEALS)
    ),
    PromoBanner(
        "New Smartphones", "Latest phones, pay easily with M-Pesa", "Explore", Icons.Outlined.PhoneAndroid,
        listOf(Color(0xFF0F9D8A), MpesaGreen), BannerAction.Category("smartphones", "Smartphones")
    ),
    PromoBanner(
        "Free Delivery", "On orders above KSh 5,000. Use WELCOME10 for 10% off", "Start shopping",
        Icons.Outlined.LocalShipping, listOf(BrandCoral, DealRed), BannerAction.Collection(ProductCollection.ALL)
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PromoCarousel(onBannerClick: (PromoBanner) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { banners.size })
    LaunchedEffect(pagerState) {
        while (true) {
            delay(4_500)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % banners.size)
        }
    }
    Column {
        HorizontalPager(state = pagerState, pageSpacing = 12.dp) { page ->
            BannerCard(banner = banners[page], onClick = { onBannerClick(banners[page]) })
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(banners.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(if (selected) 20.dp else 6.dp)
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

@Composable
private fun BannerCard(banner: PromoBanner, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(banner.colors))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Icon(
            banner.icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.22f),
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.CenterEnd)
        )
        Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(0.72f)) {
            Text(banner.title, color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(banner.subtitle, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = banner.colors.first(), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    banner.cta,
                    color = banner.colors.first(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

/** Deals "end" at midnight, a common flash-sale pattern. */
@Composable
private fun CountdownChip() {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    val midnight = remember(now / 86_400_000L) {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val remaining = ((midnight - now) / 1000).coerceAtLeast(0)
    val text = String.format(Locale.US, "%02d:%02d:%02d", remaining / 3600, (remaining % 3600) / 60, remaining % 60)
    Text(
        text,
        color = Color.White,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier
            .padding(start = 10.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DealRed)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

// ---- Previews --------------------------------------------------------------

private val previewState = HomeUiState(
    isLoading = false,
    userFirstName = "Kagoni",
    userInitials = "KL",
    categories = PreviewData.categories,
    flashDeals = PreviewData.products.filter { it.discountPercent >= 8 },
    topRated = PreviewData.products.sortedByDescending { it.rating },
    recommended = PreviewData.products,
    wishlistIds = setOf(1),
    cartCount = 3
)

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun HomeContentPreview() {
    EccomerceAppTheme {
        HomeContent(previewState, {}, {}, { _, _ -> }, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeLoadingPreview() {
    EccomerceAppTheme {
        HomeContent(HomeUiState(isLoading = true), {}, {}, { _, _ -> }, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeErrorPreview() {
    EccomerceAppTheme {
        HomeContent(
            HomeUiState(isLoading = false, errorMessage = "You're offline. Check your connection and try again."),
            {}, {}, { _, _ -> }, {}, {}, {}, {}, {}
        )
    }
}
