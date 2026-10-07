package com.bramwel.eccomerceapp.features.products.presentation.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.core.common.toUserMessage
import com.bramwel.eccomerceapp.features.cart.domain.repository.CartRepository
import com.bramwel.eccomerceapp.features.products.domain.model.Product
import com.bramwel.eccomerceapp.features.products.domain.repository.ProductRepository
import com.bramwel.eccomerceapp.features.reviews.domain.model.ProductReviews
import com.bramwel.eccomerceapp.features.reviews.domain.repository.ReviewRepository
import com.bramwel.eccomerceapp.features.reviews.domain.usecase.ObserveReviewablesUseCase
import com.bramwel.eccomerceapp.features.wishlist.domain.repository.WishlistRepository
import com.bramwel.eccomerceapp.navigation.AppRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProductDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    productRepository: ProductRepository,
    private val wishlistRepository: WishlistRepository,
    private val cartRepository: CartRepository,
    private val reviewRepository: ReviewRepository,
    observeReviewables: ObserveReviewablesUseCase
) : ViewModel() {

    private val productId = savedStateHandle.toRoute<AppRoute.ProductDetails>().productId

    private data class Local(
        val quantity: Int = 1,
        val isAddingToCart: Boolean = false,
        val storeReviews: ProductReviews = ProductReviews.Empty,
        val userMessage: String? = null
    )

    private val local = MutableStateFlow(Local())
    private val _effects = Channel<ProductDetailsEffect>(Channel.BUFFERED)
    val effects: Flow<ProductDetailsEffect> = _effects.receiveAsFlow()

    private val productWithSimilar: Flow<Pair<Product?, List<Product>>> =
        productRepository.observeProduct(productId).flatMapLatest { product ->
            if (product == null) {
                flowOf(null to emptyList())
            } else {
                productRepository.observeProductsByCategory(product.categorySlug).map { list ->
                    product to list.filter { it.id != product.id }.sortedByDescending { it.rating }.take(10)
                }
            }
        }

    private val productState = combine(
        productWithSimilar,
        observeReviewables.eligibility(productId)
    ) { (product, similar), eligibility -> Triple(product, similar, eligibility) }

    val uiState: StateFlow<ProductDetailsUiState> = combine(
        productState,
        wishlistRepository.observeWishlistIds(),
        cartRepository.observeQuantity(productId),
        cartRepository.observeItemCount(),
        local
    ) { (product, similar, eligibility), wishlist, inCart, cartCount, localState ->
        ProductDetailsUiState(
            isLoading = false,
            product = product,
            quantity = localState.quantity,
            inCartQuantity = inCart,
            similar = similar,
            wishlistIds = wishlist,
            cartCount = cartCount,
            isAddingToCart = localState.isAddingToCart,
            storeReviews = localState.storeReviews,
            reviewEligibility = eligibility,
            userMessage = localState.userMessage
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductDetailsUiState())

    init {
        viewModelScope.launch {
            (reviewRepository.getProductReviews(productId) as? AppResult.Success)?.data?.let { reviews ->
                local.update { it.copy(storeReviews = reviews) }
            }
        }
        viewModelScope.launch { reviewRepository.refreshMyReviews() }
    }

    fun onEvent(event: ProductDetailsEvent) {
        when (event) {
            ProductDetailsEvent.IncreaseQuantity -> local.update {
                it.copy(quantity = (it.quantity + 1).coerceAtMost(uiState.value.maxQuantity.coerceAtLeast(1)))
            }
            ProductDetailsEvent.DecreaseQuantity -> local.update { it.copy(quantity = (it.quantity - 1).coerceAtLeast(1)) }
            is ProductDetailsEvent.ToggleWishlist -> viewModelScope.launch {
                val added = wishlistRepository.toggle(event.productId)
                showMessage(if (added) "Saved to your wishlist" else "Removed from wishlist")
            }
            ProductDetailsEvent.AddToCart -> addToCart(goToCheckout = false)
            ProductDetailsEvent.BuyNow -> addToCart(goToCheckout = true)
            is ProductDetailsEvent.AddSimilarToCart -> viewModelScope.launch {
                when (val result = cartRepository.add(event.productId)) {
                    is AppResult.Success -> showMessage("Added to cart")
                    is AppResult.Error -> showMessage(result.error.toUserMessage())
                }
            }
            ProductDetailsEvent.MessageShown -> local.update { it.copy(userMessage = null) }
        }
    }

    private fun addToCart(goToCheckout: Boolean) {
        val state = uiState.value
        val product = state.product ?: return
        viewModelScope.launch {
            // "Buy now" with the item already in the cart just goes to checkout.
            if (goToCheckout && state.inCartQuantity > 0 && state.maxQuantity == 0) {
                _effects.send(ProductDetailsEffect.NavigateToCheckout)
                return@launch
            }
            local.update { it.copy(isAddingToCart = true) }
            val result = cartRepository.add(product.id, state.quantity)
            local.update { it.copy(isAddingToCart = false, quantity = 1) }
            when (result) {
                is AppResult.Success -> if (goToCheckout) {
                    _effects.send(ProductDetailsEffect.NavigateToCheckout)
                } else {
                    showMessage("Added ${state.quantity} to cart")
                }
                is AppResult.Error -> showMessage(result.error.toUserMessage())
            }
        }
    }

    private fun showMessage(message: String) = local.update { it.copy(userMessage = message) }
}
