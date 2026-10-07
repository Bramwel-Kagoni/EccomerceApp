package com.bramwel.eccomerceapp.features.products.presentation.list

import com.bramwel.eccomerceapp.features.products.domain.model.Product
import com.bramwel.eccomerceapp.features.products.domain.model.ProductFilter
import com.bramwel.eccomerceapp.features.products.domain.model.ProductSort

data class ProductListUiState(
    val title: String = "",
    val isLoading: Boolean = true,
    val products: List<Product> = emptyList(),
    val wishlistIds: Set<Int> = emptySet(),
    val cartCount: Int = 0,
    val filter: ProductFilter = ProductFilter(),
    val userMessage: String? = null
) {
    val hasActiveFilters: Boolean get() = filter != ProductFilter()
}

sealed interface ProductListEvent {
    data class SortChanged(val sort: ProductSort) : ProductListEvent
    data class InStockOnlyChanged(val enabled: Boolean) : ProductListEvent
    data class MinRatingChanged(val minRating: Double) : ProductListEvent
    data object ResetFilters : ProductListEvent
    data class ToggleWishlist(val productId: Int) : ProductListEvent
    data class AddToCart(val productId: Int) : ProductListEvent
    data object MessageShown : ProductListEvent
}
