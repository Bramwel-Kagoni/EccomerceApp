package com.bramwel.eccomerceapp.features.cart.presentation

import com.bramwel.eccomerceapp.features.cart.domain.model.CartSummary

data class CartUiState(
    val isLoading: Boolean = true,
    val summary: CartSummary = CartSummary(),
    val userMessage: String? = null
)

sealed interface CartEvent {
    data class Increase(val productId: Int) : CartEvent
    data class Decrease(val productId: Int) : CartEvent
    data class Remove(val productId: Int) : CartEvent
    data class MoveToWishlist(val productId: Int) : CartEvent
    data object ClearCart : CartEvent
    data object MessageShown : CartEvent
}
