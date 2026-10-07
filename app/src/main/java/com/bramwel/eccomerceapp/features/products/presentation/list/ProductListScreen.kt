package com.bramwel.eccomerceapp.features.products.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bramwel.eccomerceapp.core.ui.components.AppTopBar
import com.bramwel.eccomerceapp.core.ui.components.CartIconButton
import com.bramwel.eccomerceapp.core.ui.components.EmptyState
import com.bramwel.eccomerceapp.core.ui.components.ProductGridSkeleton
import com.bramwel.eccomerceapp.core.ui.theme.EccomerceAppTheme
import com.bramwel.eccomerceapp.features.products.domain.model.ProductSort
import com.bramwel.eccomerceapp.features.products.presentation.PreviewData
import com.bramwel.eccomerceapp.features.products.presentation.components.ProductItem

@Composable
fun ProductListScreen(
    onBackClick: () -> Unit,
    onProductClick: (Int) -> Unit,
    onCartClick: () -> Unit,
    viewModel: ProductListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProductListContent(state, viewModel::onEvent, onBackClick, onProductClick, onCartClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListContent(
    state: ProductListUiState,
    onEvent: (ProductListEvent) -> Unit,
    onBackClick: () -> Unit,
    onProductClick: (Int) -> Unit,
    onCartClick: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showSortSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            onEvent(ProductListEvent.MessageShown)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = state.title,
                onBackClick = onBackClick,
                actions = { CartIconButton(count = state.cartCount, onClick = onCartClick) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FilterBar(state = state, onEvent = onEvent, onSortClick = { showSortSheet = true })
            when {
                state.isLoading -> ProductGridSkeleton()
                state.products.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "No products found",
                    message = if (state.hasActiveFilters) "Try removing some filters." else "Check back soon for new arrivals.",
                    actionLabel = if (state.hasActiveFilters) "Clear filters" else null,
                    onAction = { onEvent(ProductListEvent.ResetFilters) }
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.products, key = { it.id }) { product ->
                        ProductItem(
                            product = product,
                            isWishlisted = product.id in state.wishlistIds,
                            onClick = { onProductClick(product.id) },
                            onWishlistClick = { onEvent(ProductListEvent.ToggleWishlist(product.id)) },
                            onAddToCart = { onEvent(ProductListEvent.AddToCart(product.id)) }
                        )
                    }
                }
            }
        }
    }

    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Text(
                "Sort by",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
                ProductSort.entries.forEach { sort ->
                    ListItem(
                        headlineContent = { Text(sort.label) },
                        trailingContent = {
                            if (sort == state.filter.sort) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onEvent(ProductListEvent.SortChanged(sort))
                                showSortSheet = false
                            }
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterBar(state: ProductListUiState, onEvent: (ProductListEvent) -> Unit, onSortClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = state.filter.sort != ProductSort.RECOMMENDED,
            onClick = onSortClick,
            label = { Text(state.filter.sort.label) },
            leadingIcon = { Icon(Icons.Outlined.SwapVert, contentDescription = null) }
        )
        FilterChip(
            selected = state.filter.inStockOnly,
            onClick = { onEvent(ProductListEvent.InStockOnlyChanged(!state.filter.inStockOnly)) },
            label = { Text("In stock") }
        )
        FilterChip(
            selected = state.filter.minRating >= 4.0,
            onClick = {
                onEvent(ProductListEvent.MinRatingChanged(if (state.filter.minRating >= 4.0) 0.0 else 4.0))
            },
            label = { Text("4★ & up") }
        )
        Text(
            "${state.products.size} items",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProductListPreview() {
    EccomerceAppTheme {
        ProductListContent(
            state = ProductListUiState(
                title = "Smartphones",
                isLoading = false,
                products = PreviewData.products,
                wishlistIds = setOf(2),
                cartCount = 1
            ),
            onEvent = {}, onBackClick = {}, onProductClick = {}, onCartClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProductListEmptyPreview() {
    EccomerceAppTheme {
        ProductListContent(
            state = ProductListUiState(
                title = "Top rated",
                isLoading = false,
                filter = com.bramwel.eccomerceapp.features.products.domain.model.ProductFilter(inStockOnly = true)
            ),
            onEvent = {}, onBackClick = {}, onProductClick = {}, onCartClick = {}
        )
    }
}
