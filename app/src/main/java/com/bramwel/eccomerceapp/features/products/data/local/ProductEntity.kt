package com.bramwel.eccomerceapp.features.products.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val brand: String?,
    val priceUsd: Double,
    val discountPercentage: Double,
    val rating: Double,
    val stock: Int,
    val thumbnail: String,
    val images: List<String>,
    val tags: List<String>,
    val reviews: List<ReviewEntity>,
    val warranty: String?,
    val shipping: String?,
    val returnPolicy: String?,
    val minimumOrderQuantity: Int,
    val sortIndex: Int
)

@Serializable
data class ReviewEntity(
    val rating: Int,
    val comment: String,
    val date: String,
    val reviewerName: String
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val slug: String,
    val name: String,
    val sortIndex: Int
)
