package com.bramwel.eccomerceapp.features.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.core.common.toUserMessage
import com.bramwel.eccomerceapp.core.datastore.UserPreferences
import com.bramwel.eccomerceapp.features.cart.domain.repository.CartRepository
import com.bramwel.eccomerceapp.features.products.domain.model.Product
import com.bramwel.eccomerceapp.features.products.domain.repository.ProductRepository
import com.bramwel.eccomerceapp.features.wishlist.domain.repository.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    productRepository: ProductRepository,
    private val wishlistRepository: WishlistRepository,
    private val cartRepository: CartRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val userMessage = MutableStateFlow<String?>(null)

    private val results: Flow<List<Product>?> = query
        .map { it.trim() }
        .debounce(250)
        .distinctUntilChanged()
        .flatMapLatest { text ->
            if (text.length < MIN_QUERY) flowOf(null) else productRepository.searchProducts(text)
        }

    private val popularCategories = productRepository.observeCategories().map { categories ->
        categories.sortedByDescending { it.productCount }.take(8)
    }

    val uiState: StateFlow<SearchUiState> = combine(
        query,
        results,
        combine(preferences.recentSearches, popularCategories) { recent, popular -> recent to popular },
        wishlistRepository.observeWishlistIds(),
        userMessage
    ) { text, found, (recent, popular), wishlist, message ->
        SearchUiState(
            query = text,
            results = found,
            recentSearches = recent,
            popularCategories = popular,
            wishlistIds = wishlist,
            userMessage = message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChanged -> query.value = event.query
            SearchEvent.Submit -> saveRecent(query.value)
            is SearchEvent.RecentClicked -> {
                query.value = event.query
                saveRecent(event.query)
            }
            SearchEvent.ClearRecent -> viewModelScope.launch { preferences.clearRecentSearches() }
            is SearchEvent.ToggleWishlist -> viewModelScope.launch {
                saveRecent(query.value)
                val added = wishlistRepository.toggle(event.productId)
                userMessage.value = if (added) "Saved to your wishlist" else "Removed from wishlist"
            }
            is SearchEvent.AddToCart -> viewModelScope.launch {
                saveRecent(query.value)
                userMessage.value = when (val result = cartRepository.add(event.productId)) {
                    is AppResult.Success -> "Added to cart"
                    is AppResult.Error -> result.error.toUserMessage()
                }
            }
            SearchEvent.MessageShown -> userMessage.value = null
        }
    }

    /** Called when the user commits to a search (keyboard search or tapping a result). */
    fun saveRecent(text: String) {
        if (text.trim().length < MIN_QUERY) return
        viewModelScope.launch { preferences.addRecentSearch(text) }
    }

    private companion object {
        const val MIN_QUERY = 2
    }
}
