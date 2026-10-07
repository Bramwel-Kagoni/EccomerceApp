package com.bramwel.eccomerceapp.features.cart.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.core.common.toUserMessage
import com.bramwel.eccomerceapp.features.cart.domain.repository.CartRepository
import com.bramwel.eccomerceapp.features.cart.domain.usecase.CalculateCartSummaryUseCase
import com.bramwel.eccomerceapp.features.wishlist.domain.repository.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val wishlistRepository: WishlistRepository,
    calculateSummary: CalculateCartSummaryUseCase
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CartUiState> = combine(cartRepository.observeCart(), message) { items, msg ->
        CartUiState(isLoading = false, summary = calculateSummary(items), userMessage = msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartUiState())

    fun onEvent(event: CartEvent) {
        when (event) {
            is CartEvent.Increase -> changeBy(event.productId, +1)
            is CartEvent.Decrease -> changeBy(event.productId, -1)
            is CartEvent.Remove -> viewModelScope.launch {
                cartRepository.remove(event.productId)
                message.value = "Item removed"
            }
            is CartEvent.MoveToWishlist -> viewModelScope.launch {
                if (event.productId !in wishlistRepository.observeWishlistIds().first()) {
                    wishlistRepository.toggle(event.productId)
                }
                cartRepository.remove(event.productId)
                message.value = "Moved to wishlist"
            }
            CartEvent.ClearCart -> viewModelScope.launch {
                cartRepository.clear()
                message.value = "Cart cleared"
            }
            CartEvent.MessageShown -> message.value = null
        }
    }

    private fun changeBy(productId: Int, delta: Int) {
        val current = uiState.value.summary.items.firstOrNull { it.product.id == productId }?.quantity ?: return
        viewModelScope.launch {
            val result = cartRepository.setQuantity(productId, current + delta)
            if (result is AppResult.Error) message.value = result.error.toUserMessage()
        }
    }
}
