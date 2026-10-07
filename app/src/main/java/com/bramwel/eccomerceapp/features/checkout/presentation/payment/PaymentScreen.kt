package com.bramwel.eccomerceapp.features.checkout.presentation.payment

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bramwel.eccomerceapp.core.common.formatKes
import com.bramwel.eccomerceapp.core.ui.components.AppButton
import com.bramwel.eccomerceapp.core.ui.components.AppOutlinedButton
import com.bramwel.eccomerceapp.core.ui.components.AppTextField
import com.bramwel.eccomerceapp.core.ui.components.AppTopBar
import com.bramwel.eccomerceapp.core.ui.theme.DealRed
import com.bramwel.eccomerceapp.core.ui.theme.EccomerceAppTheme
import com.bramwel.eccomerceapp.core.ui.theme.MpesaGreen
import com.bramwel.eccomerceapp.core.ui.theme.WarningAmber
import com.bramwel.eccomerceapp.features.checkout.domain.model.PaymentProgress

@Composable
fun PaymentScreen(
    onBackClick: () -> Unit,
    onShowReceipt: (orderId: String) -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PaymentEffect.ShowReceipt -> onShowReceipt(effect.orderId)
            }
        }
    }
    PaymentContent(state, viewModel::onEvent, onBackClick)
}

@Composable
fun PaymentContent(
    state: PaymentUiState,
    onEvent: (PaymentEvent) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = { AppTopBar(title = "M-Pesa payment", onBackClick = onBackClick) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AmountCard(state)
            Spacer(Modifier.height(28.dp))

            AnimatedContent(
                targetState = state.progress,
                contentKey = { it?.let { progress -> progress::class } },
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "paymentState"
            ) { progress ->
                when (progress) {
                    null -> PhoneEntry(state, onEvent, error = null)
                    is PaymentProgress.Failed -> PhoneEntry(state, onEvent, error = progress.reason)
                    PaymentProgress.SendingPrompt -> BusyState(
                        title = "Sending payment request…",
                        message = "Connecting to M-Pesa"
                    )
                    is PaymentProgress.AwaitingPin -> AwaitingPin(progress, state, onEvent)
                    is PaymentProgress.Unconfirmed -> Unconfirmed(onEvent)
                    is PaymentProgress.Paid -> PaidState(progress)
                }
            }

            Spacer(Modifier.height(32.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = MpesaGreen)
                Spacer(Modifier.width(6.dp))
                Text(
                    "Secured by M-Pesa. Never share your PIN with anyone.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AmountCard(state: PaymentUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MpesaGreen)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Amount to pay", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
            Text(
                if (state.total > 0) state.total.formatKes() else "—",
                color = Color.White,
                style = MaterialTheme.typography.displaySmall
            )
            if (state.orderNumber.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("Order ${state.orderNumber}", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PhoneEntry(state: PaymentUiState, onEvent: (PaymentEvent) -> Unit, error: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (error != null) {
            StatusBanner(Icons.Outlined.ErrorOutline, "Payment not completed", error, DealRed)
            Spacer(Modifier.height(20.dp))
        }
        AppTextField(
            value = state.phone,
            onValueChange = { onEvent(PaymentEvent.PhoneChanged(it)) },
            label = "M-Pesa phone number",
            placeholder = "0712 345 678",
            leadingIcon = Icons.Outlined.PhoneAndroid,
            error = state.phoneError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        Spacer(Modifier.height(16.dp))
        AppButton(
            text = if (error != null) "Try again" else "Send M-Pesa prompt",
            onClick = { onEvent(PaymentEvent.Pay) },
            containerColor = MpesaGreen,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "You'll receive a prompt on this phone. Enter your M-Pesa PIN to complete the payment.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BusyState(title: String, message: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator(color = MpesaGreen)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AwaitingPin(progress: PaymentProgress.AwaitingPin, state: PaymentUiState, onEvent: (PaymentEvent) -> Unit) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulseScale"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .size(120.dp)
                .scale(pulse)
                .background(MpesaGreen.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.PhoneIphone, contentDescription = null, tint = MpesaGreen, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Check your phone", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            progress.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (progress.amount > 0 && progress.amount != state.total) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Test mode: you will be charged ${progress.amount.formatKes()}",
                style = MaterialTheme.typography.labelMedium,
                color = WarningAmber
            )
        }
        Spacer(Modifier.height(20.dp))
        LinearProgressIndicator(color = MpesaGreen, modifier = Modifier.fillMaxWidth(0.6f))
        Spacer(Modifier.height(8.dp))
        Text("Waiting for confirmation…", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { onEvent(PaymentEvent.Pay) }) { Text("Didn't get the prompt? Resend") }
    }
}

@Composable
private fun Unconfirmed(onEvent: (PaymentEvent) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        StatusBanner(
            Icons.Outlined.HourglassTop,
            "Still waiting for M-Pesa",
            "We haven't received a confirmation yet. If you already entered your PIN, check again in a moment.",
            WarningAmber
        )
        Spacer(Modifier.height(20.dp))
        AppButton(
            text = "I've paid — check again",
            onClick = { onEvent(PaymentEvent.CheckAgain) },
            containerColor = MpesaGreen,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        AppOutlinedButton(text = "Send a new prompt", onClick = { onEvent(PaymentEvent.Pay) }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PaidState(progress: PaymentProgress.Paid) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MpesaGreen,
            modifier = Modifier
                .size(120.dp)
                .scale(scale.value)
        )
        Spacer(Modifier.height(16.dp))
        Text("Payment successful!", style = MaterialTheme.typography.headlineSmall)
        if (progress.mpesaReceiptNumber.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                "M-Pesa receipt: ${progress.mpesaReceiptNumber}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text("Preparing your receipt…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatusBanner(icon: ImageVector, title: String, message: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = color)
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = color)
                Text(message, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ---- Previews: one per payment state for easy UI tweaking ------------------

private val base = PaymentUiState(orderNumber = "ORD-7F3K9Q2A", total = 12_450, phone = "0712 345 678")

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun PaymentIdlePreview() {
    EccomerceAppTheme { PaymentContent(base, {}, {}) }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun PaymentAwaitingPreview() {
    EccomerceAppTheme {
        PaymentContent(
            base.copy(progress = PaymentProgress.AwaitingPin("Enter your M-Pesa PIN on 0712 345 678.", 1, "254712345678")),
            {}, {}
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun PaymentFailedPreview() {
    EccomerceAppTheme {
        PaymentContent(base.copy(progress = PaymentProgress.Failed("You cancelled the M-Pesa prompt.")), {}, {})
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun PaymentUnconfirmedPreview() {
    EccomerceAppTheme {
        PaymentContent(base.copy(progress = PaymentProgress.Unconfirmed("ws_CO_123")), {}, {})
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun PaymentPaidPreview() {
    EccomerceAppTheme {
        PaymentContent(base.copy(progress = PaymentProgress.Paid("id", "SIK7RT61SV")), {}, {})
    }
}
