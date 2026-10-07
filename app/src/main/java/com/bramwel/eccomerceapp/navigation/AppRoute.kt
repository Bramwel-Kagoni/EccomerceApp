package com.bramwel.eccomerceapp.navigation

import kotlinx.serialization.Serializable

/** Every screen in the app. Type-safe: arguments are properties, never string routes. */
sealed interface AppRoute {

    @Serializable
    data object Splash : AppRoute

    @Serializable
    data object Onboarding : AppRoute

    // ---- Top-level (bottom bar) destinations ----
    @Serializable
    data object Home : AppRoute

    @Serializable
    data object Categories : AppRoute

    @Serializable
    data object Wishlist : AppRoute

    @Serializable
    data object Cart : AppRoute

    @Serializable
    data object Profile : AppRoute

    // ---- Shopping ----
    @Serializable
    data class ProductList(
        val title: String,
        val categorySlug: String? = null,
        /** ProductCollection name (ALL / DEALS / TOP_RATED). */
        val collection: String? = null
    ) : AppRoute

    @Serializable
    data class ProductDetails(val productId: Int) : AppRoute

    @Serializable
    data object Search : AppRoute

    // ---- Checkout & orders ----
    @Serializable
    data object Checkout : AppRoute

    @Serializable
    data class Payment(
        val orderId: String,
        val phone: String,
        /** True when coming straight from checkout: auto-send the prompt and clear the cart on success. */
        val fromCheckout: Boolean
    ) : AppRoute

    @Serializable
    data class Receipt(val orderId: String) : AppRoute

    @Serializable
    data object Orders : AppRoute

    // ---- Reviews ----
    @Serializable
    data class WriteReview(
        val productId: Int,
        val productTitle: String,
        val productThumbnail: String
    ) : AppRoute

    @Serializable
    data object MyReviews : AppRoute

    // ---- Account ----
    @Serializable
    data object EditProfile : AppRoute

    @Serializable
    data object Settings : AppRoute
}
