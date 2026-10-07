package com.bramwel.eccomerceapp.features.wishlist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.core.common.toUserMessage
import com.bramwel.eccomerceapp.features.cart.domain.repository.CartRepository
import com.bramwel.eccomerceapp.features.products.domain.model.Product
import com.bramwel.eccomerceapp.features.wishlist.domain.repository.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WishlistUiState(
    val isLoading: Boolean = true,
    val items: List<Product> = emptyList(),
    val cartCount: Int = 0,
    val userMessage: String? = null,
    /** Last removed product id, so the snackbar can offer "Undo". */
    val lastRemovedId: Int? = null
)

sealed interface WishlistEvent {
    data class Remove(val productId: Int) : WishlistEvent
    data class UndoRemove(val productId: Int) : WishlistEvent
    data class MoveToCart(val productId: Int) : WishlistEvent
    data object MoveAllToCart : WishlistEvent
    data object MessageShown : WishlistEvent
}

@HiltViewModel
class WishlistViewModel @Inject constructor(
    private val wishlistRepository: WishlistRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private data class Message(val text: String, val removedId: Int? = null)

    private val message = MutableStateFlow<Message?>(null)

    val uiState: StateFlow<WishlistUiState> = combine(
        wishlistRepository.observeWishlist(),
        cartRepository.observeItemCount(),
        message
    ) { items, cartCount, msg ->
        WishlistUiState(
            isLoading = false,
            items = items,
            cartCount = cartCount,
            userMessage = msg?.text,
            lastRemovedId = msg?.removedId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WishlistUiState())

    fun onEvent(event: WishlistEvent) {
        when (event) {
            is WishlistEvent.Remove -> viewModelScope.launch {
                wishlistRepository.remove(event.productId)
                message.value = Message("Removed from wishlist", removedId = event.productId)
            }
            is WishlistEvent.UndoRemove -> viewModelScope.launch { wishlistRepository.toggle(event.productId) }
            is WishlistEvent.MoveToCart -> viewModelScope.launch {
                when (val result = cartRepository.add(event.productId)) {
                    is AppResult.Success -> {
                        wishlistRepository.remove(event.productId)
                        message.value = Message("Moved to cart")
                    }
                    is AppResult.Error -> message.value = Message(result.error.toUserMessage())
                }
            }
            WishlistEvent.MoveAllToCart -> viewModelScope.launch {
                var moved = 0
                uiState.value.items.filter { it.inStock }.forEach { product ->
                    if (cartRepository.add(product.id) is AppResult.Success) {
                        wishlistRepository.remove(product.id)
                        moved++
                    }
                }
                message.value = Message(if (moved > 0) "$moved item(s) moved to cart" else "No items in stock to move")
            }
            WishlistEvent.MessageShown -> message.value = null
        }
    }
}
