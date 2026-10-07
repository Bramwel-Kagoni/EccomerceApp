package com.bramwel.eccomerceapp.features.products.domain.repository

import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.features.products.domain.model.Category
import com.bramwel.eccomerceapp.features.products.domain.model.Product
import kotlinx.coroutines.flow.Flow

/** Offline-first catalog: the UI always observes the local cache, [refreshCatalog] updates it. */
interface ProductRepository {
    fun observeProducts(): Flow<List<Product>>
    fun observeProductsByCategory(categorySlug: String): Flow<List<Product>>
    fun observeProduct(productId: Int): Flow<Product?>
    fun searchProducts(query: String): Flow<List<Product>>
    fun observeCategories(): Flow<List<Category>>

    /** Downloads the catalog when the cache is empty, or always when [force] is true. */
    suspend fun refreshCatalog(force: Boolean = false): AppResult<Unit>
}
