package com.bramwel.eccomerceapp.features.products.presentation

import com.bramwel.eccomerceapp.features.products.domain.model.Category
import com.bramwel.eccomerceapp.features.products.domain.model.Product
import com.bramwel.eccomerceapp.features.products.domain.model.Review

/** Sample data for @Preview functions only. */
object PreviewData {

    val reviews = listOf(
        Review(5, "Very satisfied, arrived in two days!", "2026-09-01", "Achieng O."),
        Review(4, "Good value for money.", "2026-08-20", "Brian K."),
        Review(3, "Okay, packaging could be better.", "2026-08-11", "Wanjiru M.")
    )

    val products = listOf(
        product(1, "Essence Mascara Lash Princess", "beauty", "Essence", 1289, 1400, 8, 4.9, 5),
        product(2, "iPhone 13 Pro", "smartphones", "Apple", 141_999, 157_000, 10, 4.6, 12),
        product(3, "Nike Air Jordan 1 Red And Black", "mens-shoes", "Nike", 19_349, 25_800, 25, 4.3, 0),
        product(4, "Samsung Galaxy Book", "laptops", "Samsung", 154_799, 170_000, 9, 4.1, 3),
        product(5, "Gucci Bloom Eau de", "fragrances", "Gucci", 10_319, 11_000, 6, 4.4, 40),
        product(6, "Apple Airpods", "mobile-accessories", "Apple", 16_769, 18_900, 11, 4.7, 60)
    )

    val categories = listOf(
        Category("beauty", "Beauty", 5, null),
        Category("smartphones", "Smartphones", 16, null),
        Category("laptops", "Laptops", 5, null),
        Category("mens-shoes", "Mens Shoes", 5, null),
        Category("fragrances", "Fragrances", 5, null),
        Category("groceries", "Groceries", 27, null)
    )

    private fun product(
        id: Int,
        title: String,
        category: String,
        brand: String,
        price: Long,
        original: Long,
        discount: Int,
        rating: Double,
        stock: Int
    ) = Product(
        id = id,
        title = title,
        description = "A customer favourite with great quality and fast delivery across Kenya. " +
            "Backed by warranty and easy returns.",
        categorySlug = category,
        brand = brand,
        price = price,
        originalPrice = original,
        discountPercent = discount,
        rating = rating,
        stock = stock,
        thumbnail = "",
        images = listOf("", ""),
        tags = listOf(category),
        reviews = reviews,
        warranty = "1 year warranty",
        shipping = "Ships in 1-2 business days",
        returnPolicy = "30 days return policy",
        minimumOrderQuantity = 1
    )
}
