package com.bramwel.eccomerceapp.features.products.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bramwel.eccomerceapp.core.ui.components.ProductCard
import com.bramwel.eccomerceapp.features.products.domain.model.Product

/** Maps a [Product] onto the shared core [ProductCard]. */
@Composable
fun ProductItem(
    product: Product,
    isWishlisted: Boolean,
    onClick: () -> Unit,
    onWishlistClick: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProductCard(
        title = product.title,
        imageUrl = product.thumbnail,
        price = product.price,
        originalPrice = product.originalPrice,
        discountPercent = product.discountPercent,
        rating = product.rating,
        isWishlisted = isWishlisted,
        inStock = product.inStock,
        brand = product.brand,
        onClick = onClick,
        onWishlistClick = onWishlistClick,
        onAddToCart = onAddToCart,
        modifier = modifier
    )
}
