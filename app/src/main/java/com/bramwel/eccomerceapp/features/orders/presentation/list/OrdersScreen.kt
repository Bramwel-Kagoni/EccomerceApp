package com.bramwel.eccomerceapp.features.orders.presentation.list

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
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bramwel.eccomerceapp.core.common.DateFormatter
import com.bramwel.eccomerceapp.core.common.formatKes
import com.bramwel.eccomerceapp.core.ui.components.AppTopBar
import com.bramwel.eccomerceapp.core.ui.components.EmptyState
import com.bramwel.eccomerceapp.core.ui.components.ErrorView
import com.bramwel.eccomerceapp.core.ui.components.LoadingIndicator
import com.bramwel.eccomerceapp.core.ui.components.NetworkImage
import com.bramwel.eccomerceapp.core.ui.theme.EccomerceAppTheme
import com.bramwel.eccomerceapp.features.orders.domain.model.Order
import com.bramwel.eccomerceapp.features.orders.presentation.OrderPreviewData
import com.bramwel.eccomerceapp.features.orders.presentation.OrderStatusChip

@Composable
fun OrdersScreen(
    onBackClick: () -> Unit,
    onOrderClick: (orderId: String) -> Unit,
    onStartShopping: () -> Unit,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OrdersContent(state, onBackClick, onOrderClick, onStartShopping, onRefresh = viewModel::refresh)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersContent(
    state: OrdersUiState,
    onBackClick: () -> Unit,
    onOrderClick: (String) -> Unit,
    onStartShopping: () -> Unit,
    onRefresh: () -> Unit
) {
    Scaffold(topBar = { AppTopBar(title = "My orders", onBackClick = onBackClick) }) { padding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when {
            state.isLoading -> LoadingIndicator(modifier)
            state.orders.isEmpty() && state.errorMessage != null -> ErrorView(
                message = state.errorMessage,
                onRetry = onRefresh,
                icon = Icons.Outlined.CloudOff,
                modifier = modifier
            )
            state.orders.isEmpty() -> EmptyState(
                icon = Icons.Outlined.ReceiptLong,
                title = "No orders yet",
                message = "When you place an order it will show up here with its receipt.",
                actionLabel = "Start shopping",
                onAction = onStartShopping,
                modifier = modifier
            )
            else -> PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh, modifier = modifier) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.orders, key = { it.id }) { order ->
                        OrderCard(order, onClick = { onOrderClick(order.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: Order, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(order.orderNumber, style = MaterialTheme.typography.titleSmall)
                    Text(
                        DateFormatter.display(order.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OrderStatusChip(order.status)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                order.items.take(3).forEach { item ->
                    NetworkImage(
                        model = item.thumbnail,
                        contentDescription = item.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.small)
                    )
                }
                if (order.items.size > 3) {
                    Text("+${order.items.size - 3}", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${order.itemCount} item${if (order.itemCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        order.total.formatKes(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (order.canPay) {
                Spacer(Modifier.height(8.dp))
                Row {
                    Spacer(Modifier.width(0.dp))
                    Text(
                        "Tap to complete payment",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OrdersPreview() {
    EccomerceAppTheme {
        OrdersContent(
            state = OrdersUiState(
                isLoading = false,
                orders = listOf(OrderPreviewData.pendingOrder, OrderPreviewData.paidOrder, OrderPreviewData.failedOrder)
            ),
            onBackClick = {}, onOrderClick = {}, onStartShopping = {}, onRefresh = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OrdersEmptyPreview() {
    EccomerceAppTheme {
        OrdersContent(OrdersUiState(isLoading = false), {}, {}, {}, {})
    }
}
