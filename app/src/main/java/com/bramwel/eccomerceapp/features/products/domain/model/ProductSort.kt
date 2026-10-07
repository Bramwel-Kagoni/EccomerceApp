package com.bramwel.eccomerceapp.features.products.domain.model

enum class ProductSort(val label: String) {
    RECOMMENDED("Recommended"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    TOP_RATED("Top rated"),
    BIGGEST_DISCOUNT("Biggest discount")
}

/** Pre-defined product collections shown on Home ("See all"). */
enum class ProductCollection(val title: String) {
    ALL("All products"),
    DEALS("Flash deals"),
    TOP_RATED("Top rated");

    companion object {
        fun fromKey(key: String?): ProductCollection =
            entries.firstOrNull { it.name == key } ?: ALL
    }
}

data class ProductFilter(
    val sort: ProductSort = ProductSort.RECOMMENDED,
    val inStockOnly: Boolean = false,
    val minRating: Double = 0.0
)
