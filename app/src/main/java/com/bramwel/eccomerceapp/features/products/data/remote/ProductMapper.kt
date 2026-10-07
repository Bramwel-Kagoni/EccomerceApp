package com.bramwel.eccomerceapp.features.products.data.remote

import com.bramwel.eccomerceapp.features.products.data.local.CategoryEntity
import com.bramwel.eccomerceapp.features.products.data.local.ProductEntity
import com.bramwel.eccomerceapp.features.products.data.local.ReviewEntity

fun ProductDto.toEntity(sortIndex: Int): ProductEntity = ProductEntity(
    id = id,
    title = title,
    description = description,
    category = category,
    brand = brand,
    priceUsd = price,
    discountPercentage = discountPercentage,
    rating = rating,
    stock = stock,
    thumbnail = thumbnail,
    images = images,
    tags = tags,
    reviews = reviews.map { ReviewEntity(it.rating, it.comment, it.date, it.reviewerName) },
    warranty = warrantyInformation,
    shipping = shippingInformation,
    returnPolicy = returnPolicy,
    minimumOrderQuantity = minimumOrderQuantity.coerceAtLeast(1),
    sortIndex = sortIndex
)

fun CategoryDto.toEntity(sortIndex: Int): CategoryEntity =
    CategoryEntity(slug = slug, name = name, sortIndex = sortIndex)
