package com.bramwel.eccomerceapp.features.search.presentation

import com.bramwel.eccomerceapp.features.products.domain.model.Category
import com.bramwel.eccomerceapp.features.products.domain.model.Product

data class SearchUiState(
    val query: String = "",
    /** null = nothing searched yet (show suggestions). */
    val results: List<Product>? = null,
    val recentSearches: List<String> = emptyList(),
    val popularCategories: List<Category> = emptyList(),
    val wishlistIds: Set<Int> = emptySet(),
    val userMessage: String? = null
)

sealed interface SearchEvent {
    data class QueryChanged(val query: String) : SearchEvent
    data object Submit : SearchEvent
    data class RecentClicked(val query: String) : SearchEvent
    data object ClearRecent : SearchEvent
    data class ToggleWishlist(val productId: Int) : SearchEvent
    data class AddToCart(val productId: Int) : SearchEvent
    data object MessageShown : SearchEvent
}
