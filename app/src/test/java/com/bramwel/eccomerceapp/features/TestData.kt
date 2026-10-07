package com.bramwel.eccomerceapp.features

import com.bramwel.eccomerceapp.features.products.domain.model.Product

fun testProduct(
    id: Int,
    price: Long = 1_000,
    original: Long = price,
    discount: Int = 0,
    rating: Double = 4.0,
    stock: Int = 10
) = Product(
    id = id, title = "Product $id", description = "", categorySlug = "cat", brand = null,
    price = price, originalPrice = original, discountPercent = discount, rating = rating, stock = stock,
    thumbnail = "", images = emptyList(), tags = emptyList(), reviews = emptyList(),
    warranty = null, shipping = null, returnPolicy = null, minimumOrderQuantity = 1
)
