package com.bramwel.eccomerceapp.features.products.domain.model

/** Prices are whole Kenyan shillings. */
data class Product(
    val id: Int,
    val title: String,
    val description: String,
    val categorySlug: String,
    val brand: String?,
    val price: Long,
    val originalPrice: Long,
    val discountPercent: Int,
    val rating: Double,
    val stock: Int,
    val thumbnail: String,
    val images: List<String>,
    val tags: List<String>,
    val reviews: List<Review>,
    val warranty: String?,
    val shipping: String?,
    val returnPolicy: String?,
    val minimumOrderQuantity: Int
) {
    val inStock: Boolean get() = stock > 0
    val isLowStock: Boolean get() = stock in 1..5
    val hasDiscount: Boolean get() = discountPercent > 0 && originalPrice > price
}

data class Review(
    val rating: Int,
    val comment: String,
    val date: String,
    val reviewerName: String
)

data class Category(
    val slug: String,
    val name: String,
    val productCount: Int,
    val imageUrl: String?
)
