package com.bramwel.eccomerceapp.features.orders.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bramwel.eccomerceapp.core.ui.theme.DealRed
import com.bramwel.eccomerceapp.core.ui.theme.SuccessGreen
import com.bramwel.eccomerceapp.core.ui.theme.WarningAmber
import com.bramwel.eccomerceapp.features.orders.domain.model.OrderStatus

@Composable
fun OrderStatusChip(status: OrderStatus, modifier: Modifier = Modifier) {
    val color: Color = when (status) {
        OrderStatus.PAID -> SuccessGreen
        OrderStatus.PENDING_PAYMENT -> WarningAmber
        OrderStatus.PAYMENT_FAILED, OrderStatus.CANCELLED -> DealRed
        OrderStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
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
