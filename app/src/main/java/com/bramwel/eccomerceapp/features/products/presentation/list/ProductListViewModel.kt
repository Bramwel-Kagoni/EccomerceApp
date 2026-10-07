package com.bramwel.eccomerceapp.features.products.presentation.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.core.common.toUserMessage
import com.bramwel.eccomerceapp.features.cart.domain.repository.CartRepository
import com.bramwel.eccomerceapp.features.products.domain.model.ProductCollection
import com.bramwel.eccomerceapp.features.products.domain.model.ProductFilter
import com.bramwel.eccomerceapp.features.products.domain.repository.ProductRepository
import com.bramwel.eccomerceapp.features.products.domain.usecase.ApplyProductFilterUseCase
import com.bramwel.eccomerceapp.features.wishlist.domain.repository.WishlistRepository
import com.bramwel.eccomerceapp.navigation.AppRoute
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
class ProductListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    productRepository: ProductRepository,
    private val wishlistRepository: WishlistRepository,
    private val cartRepository: CartRepository,
    private val applyFilter: ApplyProductFilterUseCase
) : ViewModel() {

    private val route = savedStateHandle.toRoute<AppRoute.ProductList>()
    private val collection = ProductCollection.fromKey(route.collection)
    private val filter = MutableStateFlow(ProductFilter())
    private val userMessage = MutableStateFlow<String?>(null)

    private val source = route.categorySlug
        ?.let(productRepository::observeProductsByCategory)
        ?: productRepository.observeProducts()

    val uiState: StateFlow<ProductListUiState> = combine(
        source,
        filter,
        wishlistRepository.observeWishlistIds(),
        cartRepository.observeItemCount(),
        userMessage
    ) { products, filter, wishlist, cartCount, message ->
        ProductListUiState(
            title = route.title,
            isLoading = false,
            products = applyFilter(products, filter, collection),
            wishlistIds = wishlist,
            cartCount = cartCount,
            filter = filter,
            userMessage = message
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ProductListUiState(title = route.title)
    )

    fun onEvent(event: ProductListEvent) {
        when (event) {
            is ProductListEvent.SortChanged -> filter.update { it.copy(sort = event.sort) }
            is ProductListEvent.InStockOnlyChanged -> filter.update { it.copy(inStockOnly = event.enabled) }
            is ProductListEvent.MinRatingChanged -> filter.update { it.copy(minRating = event.minRating) }
            ProductListEvent.ResetFilters -> filter.value = ProductFilter()
            is ProductListEvent.ToggleWishlist -> viewModelScope.launch {
                val added = wishlistRepository.toggle(event.productId)
                userMessage.value = if (added) "Saved to your wishlist" else "Removed from wishlist"
            }
            is ProductListEvent.AddToCart -> viewModelScope.launch {
                userMessage.value = when (val result = cartRepository.add(event.productId)) {
                    is AppResult.Success -> "Added to cart"
                    is AppResult.Error -> result.error.toUserMessage()
                }
            }
            ProductListEvent.MessageShown -> userMessage.value = null
        }
    }
}
