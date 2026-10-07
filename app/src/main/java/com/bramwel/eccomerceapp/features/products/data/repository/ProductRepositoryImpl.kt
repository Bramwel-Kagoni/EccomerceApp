package com.bramwel.eccomerceapp.features.products.data.repository

import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.core.network.safeApiCall
import com.bramwel.eccomerceapp.features.products.data.local.ProductDao
import com.bramwel.eccomerceapp.features.products.data.local.toDomain
import com.bramwel.eccomerceapp.features.products.data.remote.ProductApi
import com.bramwel.eccomerceapp.features.products.data.remote.toEntity
import com.bramwel.eccomerceapp.features.products.domain.model.Category
import com.bramwel.eccomerceapp.features.products.domain.model.Product
import com.bramwel.eccomerceapp.features.products.domain.repository.ProductRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val api: ProductApi,
    private val dao: ProductDao
) : ProductRepository {

    private val refreshMutex = Mutex()

    override fun observeProducts(): Flow<List<Product>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeProductsByCategory(categorySlug: String): Flow<List<Product>> =
        dao.observeByCategory(categorySlug).map { list -> list.map { it.toDomain() } }

    override fun observeProduct(productId: Int): Flow<Product?> =
        dao.observeById(productId).map { it?.toDomain() }

    override fun searchProducts(query: String): Flow<List<Product>> =
        dao.search(query.trim()).map { list -> list.map { it.toDomain() } }

    override fun observeCategories(): Flow<List<Category>> =
        combine(dao.observeCategories(), dao.observeAll()) { categories, products ->
            val byCategory = products.groupBy { it.category }
            categories.map { category ->
                val items = byCategory[category.slug].orEmpty()
                Category(
                    slug = category.slug,
                    name = category.name,
                    productCount = items.size,
                    imageUrl = items.maxByOrNull { it.rating }?.thumbnail
                )
            }.filter { it.productCount > 0 }
        }

    override suspend fun refreshCatalog(force: Boolean): AppResult<Unit> = refreshMutex.withLock {
        if (!force && dao.count() > 0) return AppResult.Success(Unit)

        safeApiCall {
            coroutineScope {
                val products = async { api.getProducts() }
                val categories = async { api.getCategories() }
                dao.replaceCatalog(
                    products = products.await().products.mapIndexed { index, dto -> dto.toEntity(index) },
                    categories = categories.await().mapIndexed { index, dto -> dto.toEntity(index) }
                )
            }
        }
    }
}
