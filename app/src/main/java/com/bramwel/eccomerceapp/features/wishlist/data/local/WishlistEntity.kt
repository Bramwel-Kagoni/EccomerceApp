package com.bramwel.eccomerceapp.features.wishlist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wishlist")
data class WishlistEntity(
    @PrimaryKey val productId: Int,
    val addedAt: Long
)
