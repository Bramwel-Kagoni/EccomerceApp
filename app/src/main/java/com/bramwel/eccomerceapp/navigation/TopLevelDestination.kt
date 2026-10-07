package com.bramwel.eccomerceapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.reflect.KClass

enum class TopLevelDestination(
    val route: AppRoute,
    val routeClass: KClass<out AppRoute>,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(AppRoute.Home, AppRoute.Home::class, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    CATEGORIES(AppRoute.Categories, AppRoute.Categories::class, "Categories", Icons.Filled.GridView, Icons.Outlined.GridView),
    WISHLIST(AppRoute.Wishlist, AppRoute.Wishlist::class, "Wishlist", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    CART(AppRoute.Cart, AppRoute.Cart::class, "Cart", Icons.Filled.ShoppingBag, Icons.Outlined.ShoppingBag),
    PROFILE(AppRoute.Profile, AppRoute.Profile::class, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
}
