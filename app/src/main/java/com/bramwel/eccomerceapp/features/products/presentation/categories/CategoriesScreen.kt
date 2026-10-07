package com.bramwel.eccomerceapp.features.products.presentation.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bramwel.eccomerceapp.core.ui.components.AppTopBar
import com.bramwel.eccomerceapp.core.ui.components.ErrorView
import com.bramwel.eccomerceapp.core.ui.components.ProductGridSkeleton
import com.bramwel.eccomerceapp.core.ui.theme.EccomerceAppTheme
import com.bramwel.eccomerceapp.features.products.presentation.PreviewData
import com.bramwel.eccomerceapp.features.products.presentation.components.CategoryTile

@Composable
fun CategoriesScreen(
    onCategoryClick: (slug: String, name: String) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CategoriesContent(state, onCategoryClick, onSearchClick, onRetry = viewModel::retry)
}

@Composable
fun CategoriesContent(
    state: CategoriesUiState,
    onCategoryClick: (String, String) -> Unit,
    onSearchClick: () -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Categories",
                actions = {
                    IconButton(onClick = onSearchClick) { Icon(Icons.Outlined.Search, contentDescription = "Search") }
                }
            )
        }
    ) { padding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when {
            state.isLoading -> ProductGridSkeleton(modifier)
            state.errorMessage != null -> ErrorView(
                message = state.errorMessage,
                onRetry = onRetry,
                icon = Icons.Outlined.CloudOff,
                modifier = modifier
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = modifier
            ) {
                items(state.categories, key = { it.slug }) { category ->
                    CategoryTile(category, onClick = { onCategoryClick(category.slug, category.name) })
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoriesPreview() {
    EccomerceAppTheme {
        CategoriesContent(
            state = CategoriesUiState(isLoading = false, categories = PreviewData.categories),
            onCategoryClick = { _, _ -> }, onSearchClick = {}, onRetry = {}
        )
    }
}
