package com.bramwel.eccomerceapp.navigation

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy

/** Switch bottom-bar tabs, keeping each tab's own back stack and scroll state. */
fun NavController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(AppRoute.Home) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Clear everything above Home (e.g. "Continue shopping" after a purchase). */
fun NavController.navigateHomeClearingStack() {
    navigate(AppRoute.Home) {
        popUpTo(AppRoute.Home) { inclusive = false }
        launchSingleTop = true
    }
}

fun NavController.navigateToCategory(slug: String, name: String) =
    navigate(AppRoute.ProductList(title = name, categorySlug = slug))

fun NavDestination?.isTopLevel(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true

fun NavDestination?.currentTopLevel(): TopLevelDestination? =
    TopLevelDestination.entries.firstOrNull { isTopLevel(it) }
