package com.bramwel.eccomerceapp.features.products.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ProductsResponseDto(
    val products: List<ProductDto> = emptyList(),
    val total: Int = 0
)

@Serializable
data class ProductDto(
    val id: Int,
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val discountPercentage: Double = 0.0,
    val rating: Double = 0.0,
    val stock: Int = 0,
    val tags: List<String> = emptyList(),
    val brand: String? = null,
    val warrantyInformation: String? = null,
    val shippingInformation: String? = null,
    val availabilityStatus: String? = null,
    val reviews: List<ReviewDto> = emptyList(),
    val returnPolicy: String? = null,
    val minimumOrderQuantity: Int = 1,
    val images: List<String> = emptyList(),
    val thumbnail: String = ""
)

@Serializable
data class ReviewDto(
    val rating: Int = 0,
    val comment: String = "",
    val date: String = "",
    val reviewerName: String = ""
)

@Serializable
data class CategoryDto(
    val slug: String,
    val name: String,
    val url: String? = null
)
