package com.bramwel.eccomerceapp.features.profile.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.core.common.toUserMessage
import com.bramwel.eccomerceapp.features.cart.domain.repository.CartRepository
import com.bramwel.eccomerceapp.features.orders.domain.repository.OrderRepository
import com.bramwel.eccomerceapp.features.profile.domain.model.UserProfile
import com.bramwel.eccomerceapp.features.profile.domain.repository.ProfileRepository
import com.bramwel.eccomerceapp.features.wishlist.domain.repository.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val orderCount: Int = 0,
    val wishlistCount: Int = 0,
    val cartCount: Int = 0,
    val isUpdatingPhoto: Boolean = false,
    val userMessage: String? = null
)

sealed interface ProfileEvent {
    data class PhotoPicked(val uri: String) : ProfileEvent
    data object RemovePhoto : ProfileEvent
    data object MessageShown : ProfileEvent
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    orderRepository: OrderRepository,
    wishlistRepository: WishlistRepository,
    cartRepository: CartRepository
) : ViewModel() {

    private val updatingPhoto = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    private val counts = combine(
        orderRepository.observeOrders().map { it.size },
        wishlistRepository.observeWishlistIds().map { it.size },
        cartRepository.observeItemCount()
    ) { orders, wishlist, cart -> Triple(orders, wishlist, cart) }

    val uiState: StateFlow<ProfileUiState> = combine(
        profileRepository.observeProfile(),
        counts,
        updatingPhoto,
        message
    ) { profile, (orders, wishlist, cart), updating, msg ->
        ProfileUiState(profile, orders, wishlist, cart, updating, msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.PhotoPicked -> viewModelScope.launch {
                updatingPhoto.value = true
                message.value = when (val result = profileRepository.updatePhoto(event.uri)) {
                    is AppResult.Success -> "Profile photo updated"
                    is AppResult.Error -> result.error.toUserMessage()
                }
                updatingPhoto.value = false
            }
            ProfileEvent.RemovePhoto -> viewModelScope.launch {
                profileRepository.removePhoto()
                message.value = "Profile photo removed"
            }
            ProfileEvent.MessageShown -> message.value = null
        }
    }
}
