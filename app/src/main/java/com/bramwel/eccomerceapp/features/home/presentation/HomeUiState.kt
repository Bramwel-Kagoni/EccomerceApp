package com.bramwel.eccomerceapp.features.home.presentation

import com.bramwel.eccomerceapp.features.products.domain.model.Category
import com.bramwel.eccomerceapp.features.products.domain.model.Product

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val userFirstName: String = "",
    val userInitials: String = "G",
    val userPhotoPath: String? = null,
    val categories: List<Category> = emptyList(),
    val flashDeals: List<Product> = emptyList(),
    val topRated: List<Product> = emptyList(),
    val recommended: List<Product> = emptyList(),
    val wishlistIds: Set<Int> = emptySet(),
    val cartCount: Int = 0,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val userMessage: String? = null
) {
    val isEmpty: Boolean get() = recommended.isEmpty()
}

sealed interface HomeEvent {
    data object Refresh : HomeEvent
    data object Retry : HomeEvent
    data class ToggleWishlist(val productId: Int) : HomeEvent
    data class AddToCart(val productId: Int) : HomeEvent
    data object MessageShown : HomeEvent
}
