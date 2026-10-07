package com.bramwel.eccomerceapp.features.wishlist.presentation

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.bramwel.eccomerceapp.core.ui.components.CartIconButton
import com.bramwel.eccomerceapp.core.ui.components.EmptyState
import com.bramwel.eccomerceapp.core.ui.components.LoadingIndicator
import com.bramwel.eccomerceapp.core.ui.components.NetworkImage
import com.bramwel.eccomerceapp.core.ui.components.PriceText
import com.bramwel.eccomerceapp.core.ui.components.RatingRow
import com.bramwel.eccomerceapp.core.ui.theme.DealRed
import com.bramwel.eccomerceapp.core.ui.theme.EccomerceAppTheme
import com.bramwel.eccomerceapp.features.products.domain.model.Product
import com.bramwel.eccomerceapp.features.products.presentation.PreviewData

@Composable
fun WishlistScreen(
    onProductClick: (Int) -> Unit,
    onCartClick: () -> Unit,
    onStartShopping: () -> Unit,
    viewModel: WishlistViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WishlistContent(state, viewModel::onEvent, onProductClick, onCartClick, onStartShopping)
}

@Composable
fun WishlistContent(
    state: WishlistUiState,
    onEvent: (WishlistEvent) -> Unit,
    onProductClick: (Int) -> Unit,
    onCartClick: () -> Unit,
    onStartShopping: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.userMessage) {
        val message = state.userMessage ?: return@LaunchedEffect
        val removedId = state.lastRemovedId
        val result = snackbarHostState.showSnackbar(message, actionLabel = if (removedId != null) "Undo" else null)
        onEvent(WishlistEvent.MessageShown)
        if (result == SnackbarResult.ActionPerformed && removedId != null) onEvent(WishlistEvent.UndoRemove(removedId))
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Wishlist",
                actions = { CartIconButton(count = state.cartCount, onClick = onCartClick) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            state.isLoading -> LoadingIndicator(Modifier.padding(padding))
            state.items.isEmpty() -> EmptyState(
                icon = Icons.Outlined.FavoriteBorder,
                title = "Your wishlist is empty",
                message = "Tap the heart on any product to save it for later.",
                actionLabel = "Start shopping",
                onAction = onStartShopping,
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${state.items.size} saved item${if (state.items.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { onEvent(WishlistEvent.MoveAllToCart) }) { Text("Move all to cart") }
                    }
                }
                items(state.items, key = { it.id }) { product ->
                    WishlistItem(
                        product = product,
                        onClick = { onProductClick(product.id) },
                        onRemove = { onEvent(WishlistEvent.Remove(product.id)) },
                        onMoveToCart = { onEvent(WishlistEvent.MoveToCart(product.id)) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun WishlistItem(
    product: Product,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onMoveToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            NetworkImage(
                model = product.thumbnail,
                contentDescription = product.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(96.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(product.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                RatingRow(product.rating)
                Spacer(Modifier.height(4.dp))
                PriceText(price = product.price, originalPrice = product.originalPrice)
                Spacer(Modifier.height(8.dp))
                if (product.inStock) {
                    FilledTonalButton(onClick = onMoveToCart, contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Icon(Icons.Outlined.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Move to cart")
                    }
                } else {
                    Text("Out of stock", color = DealRed, style = MaterialTheme.typography.labelLarge)
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.Top)) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove from wishlist")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WishlistPreview() {
    EccomerceAppTheme {
        WishlistContent(
            state = WishlistUiState(isLoading = false, items = PreviewData.products.take(3), cartCount = 2),
            onEvent = {}, onProductClick = {}, onCartClick = {}, onStartShopping = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WishlistEmptyPreview() {
    EccomerceAppTheme {
        WishlistContent(WishlistUiState(isLoading = false), {}, {}, {}, {})
    }
}
