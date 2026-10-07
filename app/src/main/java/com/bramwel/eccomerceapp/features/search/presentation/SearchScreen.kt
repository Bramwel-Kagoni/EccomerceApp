package com.bramwel.eccomerceapp.features.search.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bramwel.eccomerceapp.core.ui.components.EmptyState
import com.bramwel.eccomerceapp.core.ui.theme.EccomerceAppTheme
import com.bramwel.eccomerceapp.features.products.presentation.PreviewData
import com.bramwel.eccomerceapp.features.products.presentation.components.ProductItem

@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onProductClick: (Int) -> Unit,
    onCategoryClick: (slug: String, name: String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SearchContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBackClick = onBackClick,
        onProductClick = { id ->
            viewModel.saveRecent(state.query)
            onProductClick(id)
        },
        onCategoryClick = onCategoryClick,
        autoFocus = true
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchContent(
    state: SearchUiState,
    onEvent: (SearchEvent) -> Unit,
    onBackClick: () -> Unit,
    onProductClick: (Int) -> Unit,
    onCategoryClick: (String, String) -> Unit,
    autoFocus: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { if (autoFocus) focusRequester.requestFocus() }
    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            onEvent(SearchEvent.MessageShown)
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { onEvent(SearchEvent.QueryChanged(it)) },
                    placeholder = { Text("Search products, brands…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { onEvent(SearchEvent.QueryChanged("")) }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        onEvent(SearchEvent.Submit)
                        focusManager.clearFocus()
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val results = state.results
        when {
            results == null -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (state.recentSearches.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent searches", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onEvent(SearchEvent.ClearRecent) }) { Text("Clear") }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.recentSearches.forEach { recent ->
                            AssistChip(
                                onClick = { onEvent(SearchEvent.RecentClicked(recent)) },
                                label = { Text(recent) },
                                leadingIcon = { Icon(Icons.Outlined.History, contentDescription = null) }
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
                if (state.popularCategories.isNotEmpty()) {
                    Text("Popular categories", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.popularCategories.forEach { category ->
                            SuggestionChip(
                                onClick = { onCategoryClick(category.slug, category.name) },
                                label = { Text(category.name) }
                            )
                        }
                    }
                }
            }
            results.isEmpty() -> EmptyState(
                icon = Icons.Outlined.SearchOff,
                title = "No results for \"${state.query.trim()}\"",
                message = "Check the spelling or try a more general term.",
                modifier = Modifier.padding(padding)
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(results, key = { it.id }) { product ->
                    ProductItem(
                        product = product,
                        isWishlisted = product.id in state.wishlistIds,
                        onClick = { onProductClick(product.id) },
                        onWishlistClick = { onEvent(SearchEvent.ToggleWishlist(product.id)) },
                        onAddToCart = { onEvent(SearchEvent.AddToCart(product.id)) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchSuggestionsPreview() {
    EccomerceAppTheme {
        SearchContent(
            state = SearchUiState(
                recentSearches = listOf("iphone", "mascara", "sneakers"),
                popularCategories = PreviewData.categories
            ),
            onEvent = {}, onBackClick = {}, onProductClick = {}, onCategoryClick = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchResultsPreview() {
    EccomerceAppTheme {
        SearchContent(
            state = SearchUiState(query = "apple", results = PreviewData.products.take(4)),
            onEvent = {}, onBackClick = {}, onProductClick = {}, onCategoryClick = { _, _ -> }
        )
    }
}
