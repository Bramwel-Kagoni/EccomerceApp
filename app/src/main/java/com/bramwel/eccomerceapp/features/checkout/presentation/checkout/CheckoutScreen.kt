package com.bramwel.eccomerceapp.features.checkout.presentation.checkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bramwel.eccomerceapp.core.common.formatKes
import com.bramwel.eccomerceapp.core.ui.components.AppButton
import com.bramwel.eccomerceapp.core.ui.components.AppTextField
import com.bramwel.eccomerceapp.core.ui.components.AppTopBar
import com.bramwel.eccomerceapp.core.ui.components.EmptyState
import com.bramwel.eccomerceapp.core.ui.components.LoadingIndicator
import com.bramwel.eccomerceapp.core.ui.components.NetworkImage
import com.bramwel.eccomerceapp.core.ui.components.SummaryRow
import com.bramwel.eccomerceapp.core.ui.theme.DealRed
import com.bramwel.eccomerceapp.core.ui.theme.EccomerceAppTheme
import com.bramwel.eccomerceapp.core.ui.theme.MpesaGreen
import com.bramwel.eccomerceapp.core.ui.theme.SuccessGreen
import com.bramwel.eccomerceapp.features.cart.domain.model.CartItem
import com.bramwel.eccomerceapp.features.orders.domain.model.DeliveryDetails
import com.bramwel.eccomerceapp.features.orders.domain.model.DeliveryMethod
import com.bramwel.eccomerceapp.features.orders.domain.model.OrderQuote
import com.bramwel.eccomerceapp.features.products.presentation.PreviewData

@Composable
fun CheckoutScreen(
    onBackClick: () -> Unit,
    onGoToPayment: (orderId: String, phone: String) -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CheckoutEffect.GoToPayment -> onGoToPayment(effect.orderId, effect.phone)
            }
        }
    }
    CheckoutContent(state, viewModel::onEvent, onBackClick)
}

@Composable
fun CheckoutContent(
    state: CheckoutUiState,
    onEvent: (CheckoutEvent) -> Unit,
    onBackClick: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            onEvent(CheckoutEvent.MessageShown)
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Checkout", onBackClick = onBackClick) },
        bottomBar = {
            if (state.items.isNotEmpty()) {
                PlaceOrderBar(state = state, onPlaceOrder = { onEvent(CheckoutEvent.PlaceOrder) })
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            state.isLoadingCart -> LoadingIndicator(Modifier.padding(padding))
            state.items.isEmpty() -> EmptyState(
                icon = Icons.Outlined.ShoppingBag,
                title = "Nothing to check out",
                message = "Your cart is empty.",
                actionLabel = "Go back",
                onAction = onBackClick,
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
            ) {
                item { DeliveryDetailsSection(state, onEvent) }
                item { DeliveryMethodSection(state.deliveryMethod, onEvent) }
                item { PromoSection(state, onEvent) }
                item {
                    SectionCard(title = "Items (${state.itemCount})") {
                        state.items.forEach { OrderLine(it) }
                    }
                }
                item { TotalsSection(state, onEvent) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.saveToProfile,
                            onCheckedChange = { onEvent(CheckoutEvent.SaveToProfileChanged(it)) }
                        )
                        Text("Save these details to my profile", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun DeliveryDetailsSection(state: CheckoutUiState, onEvent: (CheckoutEvent) -> Unit) {
    val d = state.details
    val e = state.errors
    fun change(field: CheckoutField): (String) -> Unit = { onEvent(CheckoutEvent.FieldChanged(field, it)) }

    SectionCard(title = "Delivery details") {
        AppTextField(
            value = d.name, onValueChange = change(CheckoutField.NAME), label = "Full name",
            leadingIcon = Icons.Outlined.Person, error = e.name,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
        )
        AppTextField(
            value = d.phone, onValueChange = change(CheckoutField.PHONE), label = "M-Pesa phone number",
            placeholder = "0712 345 678", leadingIcon = Icons.Outlined.PhoneAndroid, error = e.phone,
            supportingText = "The STK prompt will be sent to this number",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
        )
        AppTextField(
            value = d.email, onValueChange = change(CheckoutField.EMAIL), label = "Email (optional)",
            leadingIcon = Icons.Outlined.Email, error = e.email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )
        AppTextField(
            value = d.address, onValueChange = change(CheckoutField.ADDRESS), label = "Street / building / house no.",
            leadingIcon = Icons.Outlined.Home, error = e.address,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
        )
        AppTextField(
            value = d.city, onValueChange = change(CheckoutField.CITY), label = "Town / city",
            placeholder = "Nairobi", leadingIcon = Icons.Outlined.LocationCity, error = e.city,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
        )
        AppTextField(
            value = d.notes, onValueChange = change(CheckoutField.NOTES), label = "Delivery notes (optional)",
            leadingIcon = Icons.Outlined.StickyNote2, singleLine = false, minLines = 2
        )
    }
}

@Composable
private fun DeliveryMethodSection(selected: DeliveryMethod, onEvent: (CheckoutEvent) -> Unit) {
    SectionCard(title = "Delivery method") {
        DeliveryMethod.entries.forEach { method ->
            val isSelected = method == selected
            Surface(
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                ),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onEvent(CheckoutEvent.DeliveryMethodChanged(method)) }
                    )
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isSelected, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(method.label, style = MaterialTheme.typography.titleSmall)
                        Text(
                            method.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PromoSection(state: CheckoutUiState, onEvent: (CheckoutEvent) -> Unit) {
    val quote = state.quote
    val applied = state.appliedPromo.isNotBlank()
    SectionCard(title = "Promo code") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppTextField(
                value = state.promoInput,
                onValueChange = { onEvent(CheckoutEvent.PromoInputChanged(it)) },
                label = "Enter code",
                placeholder = "WELCOME10",
                enabled = !applied,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            if (applied) {
                TextButton(onClick = { onEvent(CheckoutEvent.RemovePromo) }) { Text("Remove") }
            } else {
                OutlinedButton(
                    onClick = { onEvent(CheckoutEvent.ApplyPromo) },
                    enabled = state.promoInput.isNotBlank()
                ) { Text("Apply") }
            }
        }
        if (applied && quote != null && quote.promoMessage.isNotBlank()) {
            Text(
                quote.promoMessage,
                style = MaterialTheme.typography.bodySmall,
                color = if (quote.promoValid) SuccessGreen else DealRed
            )
        }
    }
}

@Composable
private fun OrderLine(item: CartItem) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        NetworkImage(
            model = item.product.thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.product.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "Qty ${item.quantity} × ${item.product.price.formatKes()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(item.lineTotal.formatKes(), style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun TotalsSection(state: CheckoutUiState, onEvent: (CheckoutEvent) -> Unit) {
    SectionCard(title = "Payment summary") {
        val quote = state.quote
        when {
            state.isQuoting && quote == null -> {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Calculating your total…", style = MaterialTheme.typography.bodySmall)
            }
            quote == null -> {
                Text(
                    state.quoteError ?: "We couldn't calculate your total.",
                    color = DealRed,
                    style = MaterialTheme.typography.bodySmall
                )
                TextButton(onClick = { onEvent(CheckoutEvent.RetryQuote) }) { Text("Try again") }
            }
            else -> {
                if (state.isQuoting) LinearProgressIndicator(Modifier.fillMaxWidth())
                SummaryRow("Subtotal", quote.subtotal.formatKes())
                SummaryRow(
                    "Delivery",
                    if (quote.deliveryFee == 0L) "FREE" else quote.deliveryFee.formatKes(),
                    valueColor = if (quote.deliveryFee == 0L) SuccessGreen else MaterialTheme.colorScheme.onSurface
                )
                if (quote.discount > 0) {
                    SummaryRow("Discount (${quote.promoCode})", "- ${quote.discount.formatKes()}", valueColor = SuccessGreen)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SummaryRow("Total", quote.total.formatKes(), bold = true)
                if (quote.subtotal != state.localSubtotal) {
                    Text(
                        "Prices were updated to the latest store prices.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceOrderBar(state: CheckoutUiState, onPlaceOrder: () -> Unit) {
    Surface(shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val total = state.quote?.total
            AppButton(
                text = if (total != null) "Pay ${total.formatKes()} with M-Pesa" else "Place order",
                onClick = onPlaceOrder,
                enabled = state.canPlaceOrder,
                loading = state.isPlacingOrder,
                containerColor = MpesaGreen,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "You'll get an M-Pesa prompt on your phone to confirm.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

private val previewItems = PreviewData.products.take(2).map { CartItem(it, 1) }

@Preview(showBackground = true, heightDp = 1800)
@Composable
private fun CheckoutPreview() {
    EccomerceAppTheme {
        CheckoutContent(
            state = CheckoutUiState(
                isLoadingCart = false,
                items = previewItems,
                details = DeliveryDetails("Kagoni Livwege", "0712345678", "", "Moi Avenue, Hse 4", "Nairobi", ""),
                appliedPromo = "WELCOME10",
                promoInput = "WELCOME10",
                quote = OrderQuote(
                    items = emptyList(), subtotal = 143_288, deliveryFee = 0, discount = 1000,
                    total = 142_288, promoCode = "WELCOME10", promoValid = true,
                    promoMessage = "10% off your order (up to KSh 1,000)", freeDeliveryThreshold = 5000
                )
            ),
            onEvent = {}, onBackClick = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun CheckoutQuoteErrorPreview() {
    EccomerceAppTheme {
        CheckoutContent(
            state = CheckoutUiState(
                isLoadingCart = false,
                items = previewItems,
                quoteError = "We can't reach the shop server right now."
            ),
            onEvent = {}, onBackClick = {}
        )
    }
}
