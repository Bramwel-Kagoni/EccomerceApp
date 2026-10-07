package com.bramwel.eccomerceapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bramwel.eccomerceapp.core.datastore.ThemeMode
import com.bramwel.eccomerceapp.core.datastore.UserPreferences
import com.bramwel.eccomerceapp.features.cart.domain.repository.CartRepository
import com.bramwel.eccomerceapp.features.wishlist.domain.repository.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** App-wide state: theme and the badge counts shown on the bottom bar. */
@HiltViewModel
class MainViewModel @Inject constructor(
    userPreferences: UserPreferences,
    cartRepository: CartRepository,
    wishlistRepository: WishlistRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = userPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val cartCount: StateFlow<Int> = cartRepository.observeItemCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val wishlistCount: StateFlow<Int> = wishlistRepository.observeWishlistIds()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
