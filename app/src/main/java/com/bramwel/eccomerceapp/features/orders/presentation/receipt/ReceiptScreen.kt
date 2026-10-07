package com.bramwel.eccomerceapp.features.orders.presentation.receipt

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bramwel.eccomerceapp.core.common.DateFormatter
import com.bramwel.eccomerceapp.core.common.MpesaPhone
import com.bramwel.eccomerceapp.core.common.formatKes
import com.bramwel.eccomerceapp.core.ui.components.AppButton
import com.bramwel.eccomerceapp.core.ui.components.AppOutlinedButton
import com.bramwel.eccomerceapp.core.ui.components.AppTopBar
import com.bramwel.eccomerceapp.core.ui.components.ErrorView
import com.bramwel.eccomerceapp.core.ui.components.LoadingIndicator
import com.bramwel.eccomerceapp.core.ui.components.SummaryRow
import com.bramwel.eccomerceapp.core.ui.theme.EccomerceAppTheme
import com.bramwel.eccomerceapp.core.ui.theme.MpesaGreen
import com.bramwel.eccomerceapp.core.ui.theme.SuccessGreen
import com.bramwel.eccomerceapp.core.ui.theme.WarningAmber
import com.bramwel.eccomerceapp.features.orders.domain.model.Order
import com.bramwel.eccomerceapp.features.orders.domain.model.OrderStatus
import com.bramwel.eccomerceapp.features.orders.presentation.OrderPreviewData
import com.bramwel.eccomerceapp.features.orders.presentation.OrderStatusChip
import java.io.File

@Composable
fun ReceiptScreen(
    onBackClick: () -> Unit,
    onContinueShopping: () -> Unit,
    onPayNow: (orderId: String, phone: String) -> Unit,
    onWriteReview: (productId: Int, title: String, thumbnail: String) -> Unit,
    viewModel: ReceiptViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ReceiptEffect.SharePdf -> {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(effect.path))
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(Intent.createChooser(intent, "Share receipt"))
                    } catch (e: ActivityNotFoundException) {
                        // No app can handle PDFs; nothing else to do.
                    }
                }
            }
        }
    }
    ReceiptContent(
        state = state,
        onBackClick = onBackClick,
        onContinueShopping = onContinueShopping,
        onPayNow = onPayNow,
        onShare = viewModel::shareReceipt,
        onRetry = viewModel::refresh,
        onCopied = viewModel::onCopied,
        onMessageShown = viewModel::onMessageShown,
        onWriteReview = onWriteReview
    )
}

@Composable
fun ReceiptContent(
    state: ReceiptUiState,
    onBackClick: () -> Unit,
    onContinueShopping: () -> Unit,
    onPayNow: (String, String) -> Unit,
    onShare: () -> Unit,
    onRetry: () -> Unit,
    onCopied: () -> Unit,
    onMessageShown: () -> Unit,
    onWriteReview: (Int, String, String) -> Unit = { _, _, _ -> }
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }
    val order = state.order

    Scaffold(
        topBar = { AppTopBar(title = if (order?.status == OrderStatus.PAID) "Receipt" else "Order details", onBackClick = onBackClick) },
        bottomBar = {
            if (order != null) {
                ReceiptActions(order, state.isExporting, onShare, onContinueShopping, onPayNow)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            state.isLoading -> LoadingIndicator(Modifier.padding(padding))
            order == null -> ErrorView(
                message = state.errorMessage ?: "We couldn't load this order.",
                onRetry = onRetry,
                icon = Icons.Outlined.CloudOff,
                modifier = Modifier.padding(padding)
            )
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatusHeader(order)
                Spacer(Modifier.height(20.dp))
                ReceiptCard(order, onCopied, onWriteReview)
            }
        }
    }
}

@Composable
private fun StatusHeader(order: Order) {
    val paid = order.status == OrderStatus.PAID
    Box(
        Modifier
            .size(88.dp)
            .background((if (paid) SuccessGreen else WarningAmber).copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (paid) Icons.Filled.CheckCircle else Icons.Outlined.HourglassTop,
            contentDescription = null,
            tint = if (paid) SuccessGreen else WarningAmber,
            modifier = Modifier.size(52.dp)
        )
    }
    Spacer(Modifier.height(12.dp))
    Text(
        if (paid) "Thank you for your order!" else "Awaiting payment",
        style = MaterialTheme.typography.headlineSmall
    )
    Text(
        if (paid) "Your payment was received and your order is being prepared."
        else "Complete the M-Pesa payment to confirm this order.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ReceiptCard(order: Order, onCopied: () -> Unit, onWriteReview: (Int, String, String) -> Unit) {
    val clipboard = LocalClipboardManager.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Order number", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(order.orderNumber, style = MaterialTheme.typography.titleMedium)
                }
                OrderStatusChip(order.status)
            }
            val receiptCode = order.payment?.mpesaReceiptNumber.orEmpty()
            if (order.status == OrderStatus.PAID) {
                Surface(color = MpesaGreen.copy(alpha = 0.1f), shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("M-Pesa receipt", style = MaterialTheme.typography.labelMedium, color = MpesaGreen)
                            Text(
                                receiptCode.ifBlank { "Confirmed" },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        if (receiptCode.isNotBlank()) {
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(receiptCode))
                                onCopied()
                            }) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy M-Pesa code") }
                        }
                    }
                }
            }
            SummaryRow("Date", DateFormatter.display(order.createdAt))
            order.paidAt?.let { SummaryRow("Paid on", DateFormatter.display(it)) }
            order.payment?.takeIf { order.status == OrderStatus.PAID }?.let {
                SummaryRow("Paid from", MpesaPhone.display(it.phone))
            }
            SummaryRow("Payment method", "M-Pesa")

            DashedDivider()
            Text("Items", style = MaterialTheme.typography.titleSmall)
            order.items.forEach { item ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        "${item.quantity} ×",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp)
                    )
                    Text(
                        item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(item.lineTotal.formatKes(), style = MaterialTheme.typography.bodyMedium)
                }
                if (order.status == OrderStatus.PAID) {
                    TextButton(
                        onClick = { onWriteReview(item.productId, item.title, item.thumbnail) },
                        contentPadding = PaddingValues(start = 36.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Outlined.RateReview, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Rate & review", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            DashedDivider()
            SummaryRow("Subtotal", order.subtotal.formatKes())
            SummaryRow(
                "Delivery (${order.deliveryMethod.label.substringBefore(' ')})",
                if (order.deliveryFee == 0L) "FREE" else order.deliveryFee.formatKes()
            )
            if (order.discount > 0) {
                SummaryRow(
                    "Discount${if (order.promoCode.isNotBlank()) " (${order.promoCode})" else ""}",
                    "- ${order.discount.formatKes()}",
                    valueColor = SuccessGreen
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SummaryRow("Total", order.total.formatKes(), bold = true)

            DashedDivider()
            Text("Delivery to", style = MaterialTheme.typography.titleSmall)
            Text(order.customerName, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${order.address}, ${order.city}\n${MpesaPhone.display(order.phone)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DashedDivider() {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(40) {
            Box(
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )
        }
    }
}

@Composable
private fun ReceiptActions(
    order: Order,
    isExporting: Boolean,
    onShare: () -> Unit,
    onContinueShopping: () -> Unit,
    onPayNow: (String, String) -> Unit
) {
    Surface(shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (order.canPay) {
                AppButton(
                    text = "Pay ${order.total.formatKes()} with M-Pesa",
                    onClick = { onPayNow(order.id, order.phone) },
                    containerColor = MpesaGreen,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                AppButton(
                    text = "Share receipt (PDF)",
                    onClick = onShare,
                    loading = isExporting,
                    leadingIcon = Icons.Outlined.PictureAsPdf,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            AppOutlinedButton(text = "Continue shopping", onClick = onContinueShopping, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview(showBackground = true, heightDp = 1300)
@Composable
private fun ReceiptPaidPreview() {
    EccomerceAppTheme {
        ReceiptContent(
            state = ReceiptUiState(isLoading = false, order = OrderPreviewData.paidOrder),
            onBackClick = {}, onContinueShopping = {}, onPayNow = { _, _ -> }, onShare = {},
            onRetry = {}, onCopied = {}, onMessageShown = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun ReceiptPendingPreview() {
    EccomerceAppTheme {
        ReceiptContent(
            state = ReceiptUiState(isLoading = false, order = OrderPreviewData.pendingOrder),
            onBackClick = {}, onContinueShopping = {}, onPayNow = { _, _ -> }, onShare = {},
            onRetry = {}, onCopied = {}, onMessageShown = {}
        )
    }
}
