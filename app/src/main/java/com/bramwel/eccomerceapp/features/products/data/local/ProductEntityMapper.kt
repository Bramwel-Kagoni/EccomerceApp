package com.bramwel.eccomerceapp.features.products.data.local

import com.bramwel.eccomerceapp.core.common.Money
import com.bramwel.eccomerceapp.features.products.domain.model.Product
import com.bramwel.eccomerceapp.features.products.domain.model.Review
import kotlin.math.roundToInt

/** Currency conversion happens here so a rate change never needs a re-download. */
fun ProductEntity.toDomain(): Product {
    val price = Money.usdToKes(priceUsd)
    val original = Money.originalKes(priceUsd, discountPercentage).coerceAtLeast(price)
    return Product(
        id = id,
        title = title,
        description = description,
        categorySlug = category,
        brand = brand,
        price = price,
        originalPrice = original,
        discountPercent = discountPercentage.roundToInt(),
        rating = rating,
        stock = stock,
        thumbnail = thumbnail,
        images = images.ifEmpty { listOf(thumbnail) },
        tags = tags,
        reviews = reviews.map { Review(it.rating, it.comment, it.date, it.reviewerName) },
        warranty = warranty,
        shipping = shipping,
        returnPolicy = returnPolicy,
        minimumOrderQuantity = minimumOrderQuantity
    )
}
