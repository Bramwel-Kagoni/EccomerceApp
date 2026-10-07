package com.bramwel.eccomerceapp.features.products.presentation.details

import com.bramwel.eccomerceapp.core.common.Constants
import com.bramwel.eccomerceapp.features.products.domain.model.Product
import com.bramwel.eccomerceapp.features.reviews.domain.model.ProductReviews
import com.bramwel.eccomerceapp.features.reviews.domain.model.ReviewEligibility

data class ProductDetailsUiState(
    val isLoading: Boolean = true,
    val product: Product? = null,
    val quantity: Int = 1,
    val inCartQuantity: Int = 0,
    val similar: List<Product> = emptyList(),
    val wishlistIds: Set<Int> = emptySet(),
    val cartCount: Int = 0,
    val isAddingToCart: Boolean = false,
    /** Approved, purchase-verified reviews from our own store. */
    val storeReviews: ProductReviews = ProductReviews.Empty,
    val reviewEligibility: ReviewEligibility = ReviewEligibility.NotPurchased,
    val userMessage: String? = null
) {
    val totalReviewCount: Int get() = storeReviews.count + (product?.reviews?.size ?: 0)

    val isWishlisted: Boolean get() = product?.let { it.id in wishlistIds } ?: false
    /** How many more units can still be added (stock and per-item limit minus what's in the cart). */
    val maxQuantity: Int
        get() = product
            ?.let { minOf(it.stock, Constants.MAX_QUANTITY_PER_ITEM) - inCartQuantity }
            ?.coerceAtLeast(0) ?: 0
    val notFound: Boolean get() = !isLoading && product == null
}

sealed interface ProductDetailsEvent {
    data object IncreaseQuantity : ProductDetailsEvent
    data object DecreaseQuantity : ProductDetailsEvent
    data class ToggleWishlist(val productId: Int) : ProductDetailsEvent
    data object AddToCart : ProductDetailsEvent
    data object BuyNow : ProductDetailsEvent
    data class AddSimilarToCart(val productId: Int) : ProductDetailsEvent
    data object MessageShown : ProductDetailsEvent
}

sealed interface ProductDetailsEffect {
    data object NavigateToCheckout : ProductDetailsEffect
}
