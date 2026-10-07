package com.bramwel.eccomerceapp.features.products.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.core.common.toUserMessage
import com.bramwel.eccomerceapp.features.products.domain.model.Category
import com.bramwel.eccomerceapp.features.products.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val loading = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CategoriesUiState> = combine(
        productRepository.observeCategories(),
        loading,
        error
    ) { categories, isLoading, errorMessage ->
        CategoriesUiState(
            isLoading = isLoading && categories.isEmpty(),
            categories = categories,
            errorMessage = errorMessage.takeIf { categories.isEmpty() }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    init {
        retry()
    }

    fun retry() {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            val result = productRepository.refreshCatalog(force = false)
            error.value = (result as? AppResult.Error)?.error?.toUserMessage()
            loading.value = false
        }
    }
}
