package com.bramwel.eccomerceapp.features.orders.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.core.common.toUserMessage
import com.bramwel.eccomerceapp.features.orders.domain.model.Order
import com.bramwel.eccomerceapp.features.orders.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrdersUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val orders: List<Order> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val refreshing = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<OrdersUiState> = combine(
        orderRepository.observeOrders(),
        refreshing,
        error
    ) { orders, isRefreshing, errorMessage ->
        OrdersUiState(
            isLoading = isRefreshing && orders.isEmpty(),
            isRefreshing = isRefreshing && orders.isNotEmpty(),
            orders = orders,
            errorMessage = errorMessage
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrdersUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            error.value = (orderRepository.refreshOrders() as? AppResult.Error)?.error?.toUserMessage()
            refreshing.value = false
        }
    }
}
