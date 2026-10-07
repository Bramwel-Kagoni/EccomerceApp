package com.bramwel.eccomerceapp.features.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.core.common.toUserMessage
import com.bramwel.eccomerceapp.core.network.NetworkMonitor
import com.bramwel.eccomerceapp.features.cart.domain.repository.CartRepository
import com.bramwel.eccomerceapp.features.products.domain.model.ProductCollection
import com.bramwel.eccomerceapp.features.products.domain.model.ProductFilter
import com.bramwel.eccomerceapp.features.products.domain.repository.ProductRepository
import com.bramwel.eccomerceapp.features.products.domain.usecase.ApplyProductFilterUseCase
import com.bramwel.eccomerceapp.features.profile.domain.repository.ProfileRepository
import com.bramwel.eccomerceapp.features.wishlist.domain.repository.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val wishlistRepository: WishlistRepository,
    private val cartRepository: CartRepository,
    profileRepository: ProfileRepository,
    networkMonitor: NetworkMonitor,
    private val applyFilter: ApplyProductFilterUseCase
) : ViewModel() {

    private data class Transient(
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val errorMessage: String? = null,
        val userMessage: String? = null
    )

    private val transient = MutableStateFlow(Transient())

    private val catalog = combine(
        productRepository.observeProducts(),
        productRepository.observeCategories(),
        wishlistRepository.observeWishlistIds()
    ) { products, categories, wishlist -> Triple(products, categories, wishlist) }

    val uiState: StateFlow<HomeUiState> = combine(
        catalog,
        profileRepository.observeProfile(),
        cartRepository.observeItemCount(),
        networkMonitor.isOnline,
        transient
    ) { (products, categories, wishlist), profile, cartCount, online, state ->
        HomeUiState(
            isLoading = state.isLoading && products.isEmpty(),
            isRefreshing = state.isRefreshing,
            userFirstName = profile.firstName,
            userInitials = profile.initials,
            userPhotoPath = profile.photoPath,
            categories = categories,
            flashDeals = applyFilter(products, ProductFilter(), ProductCollection.DEALS).take(12),
            topRated = applyFilter(products, ProductFilter(), ProductCollection.TOP_RATED).take(12),
            recommended = products,
            wishlistIds = wishlist,
            cartCount = cartCount,
            isOffline = !online,
            errorMessage = state.errorMessage,
            userMessage = state.userMessage
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        refresh(force = false)
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.Refresh -> refresh(force = true)
            HomeEvent.Retry -> refresh(force = true)
            is HomeEvent.ToggleWishlist -> toggleWishlist(event.productId)
            is HomeEvent.AddToCart -> addToCart(event.productId)
            HomeEvent.MessageShown -> transient.update { it.copy(userMessage = null) }
        }
    }

    private fun refresh(force: Boolean) {
        viewModelScope.launch {
            transient.update {
                it.copy(isLoading = !force, isRefreshing = force, errorMessage = null)
            }
            val result = productRepository.refreshCatalog(force)
            transient.update { state ->
                val error = (result as? AppResult.Error)?.error?.toUserMessage()
                state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = error,
                    userMessage = if (force && error != null) error else state.userMessage
                )
            }
        }
    }

    private fun toggleWishlist(productId: Int) {
        viewModelScope.launch {
            val added = wishlistRepository.toggle(productId)
            transient.update {
                it.copy(userMessage = if (added) "Saved to your wishlist" else "Removed from wishlist")
            }
        }
    }

    private fun addToCart(productId: Int) {
        viewModelScope.launch {
            val message = when (val result = cartRepository.add(productId)) {
                is AppResult.Success -> "Added to cart"
                is AppResult.Error -> result.error.toUserMessage()
            }
            transient.update { it.copy(userMessage = message) }
        }
    }
}
