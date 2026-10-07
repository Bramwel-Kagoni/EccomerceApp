package com.bramwel.eccomerceapp.features.checkout.presentation.checkout

import com.bramwel.eccomerceapp.features.cart.domain.model.CartItem
import com.bramwel.eccomerceapp.features.checkout.domain.usecase.DeliveryFormErrors
import com.bramwel.eccomerceapp.features.orders.domain.model.DeliveryDetails
import com.bramwel.eccomerceapp.features.orders.domain.model.DeliveryMethod
import com.bramwel.eccomerceapp.features.orders.domain.model.OrderQuote

enum class CheckoutField { NAME, PHONE, EMAIL, ADDRESS, CITY, NOTES }

data class CheckoutUiState(
    val isLoadingCart: Boolean = true,
    val items: List<CartItem> = emptyList(),
    val details: DeliveryDetails = DeliveryDetails("", "", "", "", "", ""),
    val errors: DeliveryFormErrors = DeliveryFormErrors(),
    val deliveryMethod: DeliveryMethod = DeliveryMethod.STANDARD,
    val promoInput: String = "",
    val appliedPromo: String = "",
    val quote: OrderQuote? = null,
    val isQuoting: Boolean = false,
    val quoteError: String? = null,
    val saveToProfile: Boolean = true,
    val isPlacingOrder: Boolean = false,
    val userMessage: String? = null
) {
    val itemCount: Int get() = items.sumOf { it.quantity }
    val localSubtotal: Long get() = items.sumOf { it.lineTotal }
    val canPlaceOrder: Boolean get() = items.isNotEmpty() && !isPlacingOrder && !isQuoting && quote != null
}

sealed interface CheckoutEvent {
    data class FieldChanged(val field: CheckoutField, val value: String) : CheckoutEvent
    data class DeliveryMethodChanged(val method: DeliveryMethod) : CheckoutEvent
    data class PromoInputChanged(val value: String) : CheckoutEvent
    data object ApplyPromo : CheckoutEvent
    data object RemovePromo : CheckoutEvent
    data class SaveToProfileChanged(val enabled: Boolean) : CheckoutEvent
    data object RetryQuote : CheckoutEvent
    data object PlaceOrder : CheckoutEvent
    data object MessageShown : CheckoutEvent
}

sealed interface CheckoutEffect {
    data class GoToPayment(val orderId: String, val phone: String) : CheckoutEffect
}
