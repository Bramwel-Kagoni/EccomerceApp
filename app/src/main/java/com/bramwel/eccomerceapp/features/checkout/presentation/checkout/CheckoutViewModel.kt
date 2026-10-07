package com.bramwel.eccomerceapp.features.checkout.presentation.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.core.common.toUserMessage
import com.bramwel.eccomerceapp.features.cart.domain.repository.CartRepository
import com.bramwel.eccomerceapp.features.checkout.domain.usecase.PlaceOrderUseCase
import com.bramwel.eccomerceapp.features.checkout.domain.usecase.ValidateDeliveryDetailsUseCase
import com.bramwel.eccomerceapp.features.orders.domain.model.DeliveryDetails
import com.bramwel.eccomerceapp.features.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class CheckoutViewModel @Inject constructor(
    cartRepository: CartRepository,
    private val profileRepository: ProfileRepository,
    private val placeOrder: PlaceOrderUseCase,
    private val validate: ValidateDeliveryDetailsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private val _effects = Channel<CheckoutEffect>(Channel.BUFFERED)
    val effects: Flow<CheckoutEffect> = _effects.receiveAsFlow()

    private val quoteTrigger = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            val profile = profileRepository.observeProfile().first()
            _uiState.update {
                it.copy(
                    details = DeliveryDetails(
                        name = profile.name,
                        phone = profile.phone,
                        email = profile.email,
                        address = profile.address,
                        city = profile.city,
                        notes = ""
                    )
                )
            }
        }
        viewModelScope.launch {
            cartRepository.observeCart().collect { items ->
                _uiState.update { it.copy(items = items, isLoadingCart = false) }
            }
        }
        // Re-quote whenever the cart, delivery method or promo code changes.
        viewModelScope.launch {
            combine(
                _uiState.map { state -> state.items.map { it.product.id to it.quantity } }.distinctUntilChanged(),
                _uiState.map { it.deliveryMethod }.distinctUntilChanged(),
                _uiState.map { it.appliedPromo }.distinctUntilChanged(),
                quoteTrigger
            ) { lines, _, _, _ -> lines }
                .debounce(300)
                .collectLatest { lines -> if (lines.isNotEmpty()) requote() }
        }
    }

    fun onEvent(event: CheckoutEvent) {
        when (event) {
            is CheckoutEvent.FieldChanged -> _uiState.update { state ->
                val details = when (event.field) {
                    CheckoutField.NAME -> state.details.copy(name = event.value)
                    CheckoutField.PHONE -> state.details.copy(phone = event.value)
                    CheckoutField.EMAIL -> state.details.copy(email = event.value)
                    CheckoutField.ADDRESS -> state.details.copy(address = event.value)
                    CheckoutField.CITY -> state.details.copy(city = event.value)
                    CheckoutField.NOTES -> state.details.copy(notes = event.value)
                }
                // Re-validate live only after the first submit attempt showed errors.
                state.copy(details = details, errors = if (state.errors.hasErrors) validate(details) else state.errors)
            }
            is CheckoutEvent.DeliveryMethodChanged -> _uiState.update { it.copy(deliveryMethod = event.method) }
            is CheckoutEvent.PromoInputChanged -> _uiState.update { it.copy(promoInput = event.value.uppercase()) }
            CheckoutEvent.ApplyPromo -> _uiState.update { it.copy(appliedPromo = it.promoInput.trim()) }
            CheckoutEvent.RemovePromo -> _uiState.update { it.copy(appliedPromo = "", promoInput = "") }
            is CheckoutEvent.SaveToProfileChanged -> _uiState.update { it.copy(saveToProfile = event.enabled) }
            CheckoutEvent.RetryQuote -> quoteTrigger.update { it + 1 }
            CheckoutEvent.PlaceOrder -> submit()
            CheckoutEvent.MessageShown -> _uiState.update { it.copy(userMessage = null) }
        }
    }

    private suspend fun requote() {
        val state = _uiState.value
        _uiState.update { it.copy(isQuoting = true, quoteError = null) }
        when (val result = placeOrder.quote(state.deliveryMethod, state.appliedPromo)) {
            is AppResult.Success -> _uiState.update { it.copy(isQuoting = false, quote = result.data) }
            is AppResult.Error -> _uiState.update {
                it.copy(isQuoting = false, quote = null, quoteError = result.error.toUserMessage())
            }
        }
    }

    private fun submit() {
        val state = _uiState.value
        val errors = validate(state.details)
        if (errors.hasErrors) {
            _uiState.update { it.copy(errors = errors, userMessage = "Please check your delivery details") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isPlacingOrder = true, errors = errors) }
            val promo = state.quote?.takeIf { it.promoValid }?.promoCode.orEmpty()
            when (val result = placeOrder(state.details, state.deliveryMethod, promo)) {
                is AppResult.Success -> {
                    if (state.saveToProfile) saveDetailsToProfile(state.details)
                    _uiState.update { it.copy(isPlacingOrder = false) }
                    _effects.send(CheckoutEffect.GoToPayment(result.data.id, result.data.phone))
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(isPlacingOrder = false, userMessage = result.error.toUserMessage())
                }
            }
        }
    }

    private suspend fun saveDetailsToProfile(details: DeliveryDetails) {
        val current = profileRepository.observeProfile().first()
        profileRepository.updateProfile(
            current.copy(
                name = details.name,
                phone = details.phone,
                email = details.email.ifBlank { current.email },
                address = details.address,
                city = details.city
            )
        )
    }
}
