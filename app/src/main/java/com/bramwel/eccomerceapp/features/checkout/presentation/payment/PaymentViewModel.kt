package com.bramwel.eccomerceapp.features.checkout.presentation.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bramwel.eccomerceapp.core.common.MpesaPhone
import com.bramwel.eccomerceapp.features.checkout.domain.model.PaymentProgress
import com.bramwel.eccomerceapp.features.checkout.domain.usecase.PayWithMpesaUseCase
import com.bramwel.eccomerceapp.features.orders.domain.model.OrderStatus
import com.bramwel.eccomerceapp.features.orders.domain.repository.OrderRepository
import com.bramwel.eccomerceapp.navigation.AppRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderRepository: OrderRepository,
    private val payWithMpesa: PayWithMpesaUseCase
) : ViewModel() {

    private val route = savedStateHandle.toRoute<AppRoute.Payment>()

    private val _uiState = MutableStateFlow(PaymentUiState(phone = MpesaPhone.display(route.phone)))
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private val _effects = Channel<PaymentEffect>(Channel.BUFFERED)
    val effects: Flow<PaymentEffect> = _effects.receiveAsFlow()

    private var paymentJob: Job? = null
    private var lastCheckoutRequestId: String? = null

    init {
        viewModelScope.launch {
            orderRepository.observeOrder(route.orderId).collect { order ->
                if (order != null) {
                    _uiState.update { it.copy(orderNumber = order.orderNumber, total = order.total) }
                }
            }
        }
        viewModelScope.launch {
            orderRepository.refreshOrder(route.orderId)
            val order = orderRepository.observeOrder(route.orderId).first()
            if (order?.status == OrderStatus.PAID) {
                _effects.send(PaymentEffect.ShowReceipt(order.id))
            } else if (route.fromCheckout) {
                pay() // coming straight from checkout: send the prompt immediately
            }
        }
    }

    fun onEvent(event: PaymentEvent) {
        when (event) {
            is PaymentEvent.PhoneChanged -> _uiState.update { it.copy(phone = event.phone, phoneError = null) }
            PaymentEvent.Pay -> pay()
            PaymentEvent.CheckAgain -> checkAgain()
        }
    }

    private fun pay() {
        val phone = _uiState.value.phone
        if (!MpesaPhone.isValid(phone)) {
            _uiState.update { it.copy(phoneError = "Enter a valid Safaricom number (07XX / 01XX)") }
            return
        }
        paymentJob?.cancel()
        paymentJob = viewModelScope.launch {
            payWithMpesa(route.orderId, phone, clearCart = route.fromCheckout).collect { onProgress(it) }
        }
    }

    private fun checkAgain() {
        val checkoutId = lastCheckoutRequestId ?: return pay()
        paymentJob?.cancel()
        paymentJob = viewModelScope.launch {
            payWithMpesa.checkAgain(route.orderId, checkoutId, clearCart = route.fromCheckout).collect { onProgress(it) }
        }
    }

    private suspend fun onProgress(progress: PaymentProgress) {
        if (progress is PaymentProgress.Unconfirmed) lastCheckoutRequestId = progress.checkoutRequestId
        _uiState.update { it.copy(progress = progress) }
        if (progress is PaymentProgress.Paid) {
            delay(1_600) // let the success animation play
            _effects.send(PaymentEffect.ShowReceipt(progress.orderId))
        }
    }
}
