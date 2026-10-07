package com.bramwel.eccomerceapp.features.cart.presentation

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
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bramwel.eccomerceapp.core.common.formatKes
import com.bramwel.eccomerceapp.core.ui.components.AppButton
import com.bramwel.eccomerceapp.core.ui.components.AppTopBar
import com.bramwel.eccomerceapp.core.ui.components.EmptyState
import com.bramwel.eccomerceapp.core.ui.components.LoadingIndicator
import com.bramwel.eccomerceapp.core.ui.components.NetworkImage
import com.bramwel.eccomerceapp.core.ui.components.QuantitySelector
import com.bramwel.eccomerceapp.core.ui.components.SummaryRow
import com.bramwel.eccomerceapp.core.ui.theme.EccomerceAppTheme
import com.bramwel.eccomerceapp.core.ui.theme.SuccessGreen
import com.bramwel.eccomerceapp.features.cart.domain.model.CartItem
import com.bramwel.eccomerceapp.features.cart.domain.model.CartSummary
import com.bramwel.eccomerceapp.features.cart.domain.usecase.CalculateCartSummaryUseCase
import com.bramwel.eccomerceapp.features.products.presentation.PreviewData

@Composable
fun CartScreen(
    onProductClick: (Int) -> Unit,
    onCheckoutClick: () -> Unit,
    onStartShopping: () -> Unit,
    viewModel: CartViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CartContent(state, viewModel::onEvent, onProductClick, onCheckoutClick, onStartShopping)
}

@Composable
fun CartContent(
    state: CartUiState,
    onEvent: (CartEvent) -> Unit,
    onProductClick: (Int) -> Unit,
    onCheckoutClick: () -> Unit,
    onStartShopping: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val summary = state.summary

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            onEvent(CartEvent.MessageShown)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (summary.isEmpty) "My cart" else "My cart (${summary.itemCount})",
                actions = {
                    if (!summary.isEmpty) {
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear cart")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!summary.isEmpty) CheckoutBar(total = summary.subtotal, onCheckoutClick = onCheckoutClick)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            state.isLoading -> LoadingIndicator(Modifier.padding(padding))
            summary.isEmpty -> EmptyState(
                icon = Icons.Outlined.ShoppingBag,
                title = "Your cart is empty",
                message = "Looks like you haven't added anything yet. Explore today's deals!",
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
                item { FreeDeliveryCard(summary) }
                items(summary.items, key = { it.product.id }) { item ->
                    CartItemRow(
                        item = item,
                        onClick = { onProductClick(item.product.id) },
                        onEvent = onEvent,
                        modifier = Modifier.animateItem()
                    )
                }
                item { SummaryCard(summary) }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear cart?") },
            text = { Text("All ${summary.itemCount} items will be removed from your cart.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onEvent(CartEvent.ClearCart)
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun FreeDeliveryCard(summary: CartSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(
                    if (summary.amountToFreeDelivery == 0L) "You've unlocked FREE standard delivery!"
                    else "Add ${summary.amountToFreeDelivery.formatKes()} more for free delivery",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { summary.freeDeliveryProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.small)
            )
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onClick: () -> Unit,
    onEvent: (CartEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val product = item.product
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(12.dp)) {
            NetworkImage(
                model = product.thumbnail,
                contentDescription = product.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(88.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClick = onClick)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    product.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = onClick)
                )
                Text(
                    "${product.price.formatKes()} each",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.quantity >= item.maxQuantity) {
                    Text(
                        "Max quantity reached",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QuantitySelector(
                        quantity = item.quantity,
                        onDecrease = {
                            if (item.quantity <= 1) onEvent(CartEvent.Remove(product.id))
                            else onEvent(CartEvent.Decrease(product.id))
                        },
                        onIncrease = { onEvent(CartEvent.Increase(product.id)) },
                        canIncrease = item.quantity < item.maxQuantity,
                        showDeleteAtOne = true,
                        compact = true
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        item.lineTotal.formatKes(),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(
                    onClick = { onEvent(CartEvent.MoveToWishlist(product.id)) },
                    contentPadding = PaddingValues(0.dp)
                ) { Text("Save for later", style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: CartSummary) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Order summary", style = MaterialTheme.typography.titleMedium)
            SummaryRow("Items (${summary.itemCount})", summary.subtotal.formatKes())
            if (summary.savings > 0) {
                SummaryRow("You save", "- ${summary.savings.formatKes()}", valueColor = SuccessGreen)
            }
            SummaryRow("Delivery", "Calculated at checkout")
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SummaryRow("Subtotal", summary.subtotal.formatKes(), bold = true)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = SuccessGreen)
                Spacer(Modifier.width(6.dp))
                Text(
                    "Secure checkout with M-Pesa",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CheckoutBar(total: Long, onCheckoutClick: () -> Unit) {
    Surface(shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Subtotal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(total.formatKes(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            }
            AppButton(text = "Checkout", onClick = onCheckoutClick, modifier = Modifier.weight(1.2f))
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun CartPreview() {
    val items = PreviewData.products.take(3).mapIndexed { index, product -> CartItem(product, index + 1) }
    EccomerceAppTheme {
        CartContent(
            state = CartUiState(isLoading = false, summary = CalculateCartSummaryUseCase()(items)),
            onEvent = {}, onProductClick = {}, onCheckoutClick = {}, onStartShopping = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CartEmptyPreview() {
    EccomerceAppTheme {
        CartContent(CartUiState(isLoading = false), {}, {}, {}, {})
    }
}
