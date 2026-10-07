package com.bramwel.eccomerceapp.features.products.domain.usecase

import com.bramwel.eccomerceapp.features.products.domain.model.Product
import com.bramwel.eccomerceapp.features.products.domain.model.ProductCollection
import com.bramwel.eccomerceapp.features.products.domain.model.ProductFilter
import com.bramwel.eccomerceapp.features.products.domain.model.ProductSort
import javax.inject.Inject

/** Collection selection + filtering + sorting rules shared by list, search and home. */
class ApplyProductFilterUseCase @Inject constructor() {

    operator fun invoke(
        products: List<Product>,
        filter: ProductFilter,
        collection: ProductCollection = ProductCollection.ALL
    ): List<Product> {
        val inCollection = when (collection) {
            ProductCollection.ALL -> products
            ProductCollection.DEALS -> products.filter { it.discountPercent >= DEAL_MIN_DISCOUNT }
            ProductCollection.TOP_RATED -> products.filter { it.rating >= TOP_RATED_MIN }
        }
        val filtered = inCollection.filter { product ->
            (!filter.inStockOnly || product.inStock) && product.rating >= filter.minRating
        }
        return when (filter.sort) {
            ProductSort.RECOMMENDED -> when (collection) {
                ProductCollection.DEALS -> filtered.sortedByDescending { it.discountPercent }
                ProductCollection.TOP_RATED -> filtered.sortedByDescending { it.rating }
                ProductCollection.ALL -> filtered
            }
            ProductSort.PRICE_LOW_HIGH -> filtered.sortedBy { it.price }
            ProductSort.PRICE_HIGH_LOW -> filtered.sortedByDescending { it.price }
            ProductSort.TOP_RATED -> filtered.sortedByDescending { it.rating }
            ProductSort.BIGGEST_DISCOUNT -> filtered.sortedByDescending { it.discountPercent }
        }
    }

    companion object {
        const val DEAL_MIN_DISCOUNT = 15
        const val TOP_RATED_MIN = 4.5
    }
}
