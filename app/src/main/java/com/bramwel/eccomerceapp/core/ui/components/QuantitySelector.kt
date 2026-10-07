package com.bramwel.eccomerceapp.core.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bramwel.eccomerceapp.core.ui.theme.EccomerceAppTheme

/**
 * - / quantity / + stepper. When [showDeleteAtOne] is true the minus becomes a bin at quantity 1.
 */
@Composable
fun QuantitySelector(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    canIncrease: Boolean = true,
    showDeleteAtOne: Boolean = false,
    compact: Boolean = false
) {
    val buttonSize = if (compact) 32.dp else 40.dp
    Row(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onDecrease,
            enabled = quantity > 1 || showDeleteAtOne,
            modifier = Modifier.size(buttonSize)
        ) {
            Icon(
                imageVector = if (showDeleteAtOne && quantity <= 1) Icons.Outlined.DeleteOutline else Icons.Default.Remove,
                contentDescription = if (showDeleteAtOne && quantity <= 1) "Remove" else "Decrease",
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 28.dp)
        )
        IconButton(onClick = onIncrease, enabled = canIncrease, modifier = Modifier.size(buttonSize)) {
            Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuantitySelectorPreview() {
    EccomerceAppTheme {
        QuantitySelector(quantity = 2, onDecrease = {}, onIncrease = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun QuantitySelectorCompactPreview() {
    EccomerceAppTheme {
        QuantitySelector(quantity = 1, onDecrease = {}, onIncrease = {}, showDeleteAtOne = true, compact = true)
    }
}
