package com.bramwel.eccomerceapp.features.orders.presentation.receipt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.core.common.Constants
import com.bramwel.eccomerceapp.core.common.toUserMessage
import com.bramwel.eccomerceapp.features.orders.domain.model.Order
import com.bramwel.eccomerceapp.features.orders.domain.repository.OrderRepository
import com.bramwel.eccomerceapp.features.orders.domain.repository.ReceiptExporter
import com.bramwel.eccomerceapp.navigation.AppRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReceiptUiState(
    val isLoading: Boolean = true,
    val order: Order? = null,
    val isExporting: Boolean = false,
    val errorMessage: String? = null,
    val userMessage: String? = null
)

sealed interface ReceiptEffect {
    data class SharePdf(val path: String) : ReceiptEffect
}

@HiltViewModel
class ReceiptViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderRepository: OrderRepository,
    private val receiptExporter: ReceiptExporter
) : ViewModel() {

    private val orderId = savedStateHandle.toRoute<AppRoute.Receipt>().orderId

    private data class Local(
        val refreshing: Boolean = true,
        val exporting: Boolean = false,
        val error: String? = null,
        val message: String? = null
    )

    private val local = MutableStateFlow(Local())
    private val _effects = Channel<ReceiptEffect>(Channel.BUFFERED)
    val effects: Flow<ReceiptEffect> = _effects.receiveAsFlow()

    val uiState: StateFlow<ReceiptUiState> = combine(orderRepository.observeOrder(orderId), local) { order, l ->
        ReceiptUiState(
            isLoading = l.refreshing && order == null,
            order = order,
            isExporting = l.exporting,
            errorMessage = l.error.takeIf { order == null },
            userMessage = l.message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReceiptUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            local.update { it.copy(refreshing = true, error = null) }
            val result = orderRepository.refreshOrder(orderId)
            local.update {
                it.copy(refreshing = false, error = (result as? AppResult.Error)?.error?.toUserMessage())
            }
        }
    }

    fun shareReceipt() {
        val order = uiState.value.order ?: return
        viewModelScope.launch {
            local.update { it.copy(exporting = true) }
            when (val result = receiptExporter.export(order, Constants.STORE_NAME)) {
                is AppResult.Success -> _effects.send(ReceiptEffect.SharePdf(result.data))
                is AppResult.Error -> local.update { it.copy(message = result.error.toUserMessage()) }
            }
            local.update { it.copy(exporting = false) }
        }
    }

    fun onMessageShown() = local.update { it.copy(message = null) }

    fun onCopied() = local.update { it.copy(message = "M-Pesa code copied") }
}
