package com.bramwel.eccomerceapp.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.bramwel.eccomerceapp.features.cart.presentation.CartScreen
import com.bramwel.eccomerceapp.features.checkout.presentation.checkout.CheckoutScreen
import com.bramwel.eccomerceapp.features.checkout.presentation.payment.PaymentScreen
import com.bramwel.eccomerceapp.features.home.presentation.HomeScreen
import com.bramwel.eccomerceapp.features.onboarding.presentation.OnboardingScreen
import com.bramwel.eccomerceapp.features.orders.presentation.list.OrdersScreen
import com.bramwel.eccomerceapp.features.orders.presentation.receipt.ReceiptScreen
import com.bramwel.eccomerceapp.features.products.presentation.categories.CategoriesScreen
import com.bramwel.eccomerceapp.features.products.presentation.details.ProductDetailsScreen
import com.bramwel.eccomerceapp.features.products.presentation.list.ProductListScreen
import com.bramwel.eccomerceapp.features.profile.presentation.edit.EditProfileScreen
import com.bramwel.eccomerceapp.features.profile.presentation.profile.ProfileScreen
import com.bramwel.eccomerceapp.features.reviews.presentation.mine.MyReviewsScreen
import com.bramwel.eccomerceapp.features.reviews.presentation.write.WriteReviewScreen
import com.bramwel.eccomerceapp.features.search.presentation.SearchScreen
import com.bramwel.eccomerceapp.features.settings.presentation.SettingsScreen
import com.bramwel.eccomerceapp.features.splash.presentation.SplashScreen
import com.bramwel.eccomerceapp.features.wishlist.presentation.WishlistScreen

/**
 * The whole navigation graph. Screens only expose callbacks; this file decides where they lead.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val openProduct: (Int) -> Unit = { id -> navController.navigate(AppRoute.ProductDetails(id)) }
    val openCart: () -> Unit = { navController.navigateToTopLevel(TopLevelDestination.CART) }
    val openCategory: (String, String) -> Unit = { slug, name -> navController.navigateToCategory(slug, name) }
    val goBack: () -> Unit = { navController.popBackStack() }
    val writeReview: (Int, String, String) -> Unit = { id, title, thumbnail ->
        navController.navigate(AppRoute.WriteReview(id, title, thumbnail))
    }

    NavHost(
        navController = navController,
        startDestination = AppRoute.Splash,
        modifier = modifier,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        composable<AppRoute.Splash> {
            SplashScreen(onFinished = { showOnboarding ->
                val next: AppRoute = if (showOnboarding) AppRoute.Onboarding else AppRoute.Home
                navController.navigate(next) {
                    popUpTo(AppRoute.Splash) { inclusive = true }
                }
            })
        }

        composable<AppRoute.Onboarding> {
            OnboardingScreen(onFinished = {
                navController.navigate(AppRoute.Home) {
                    popUpTo(AppRoute.Onboarding) { inclusive = true }
                }
            })
        }

        // ---- Bottom bar tabs ----
        composable<AppRoute.Home> {
            HomeScreen(
                onProductClick = openProduct,
                onCategoryClick = openCategory,
                onCollectionClick = { collection ->
                    navController.navigate(AppRoute.ProductList(title = collection.title, collection = collection.name))
                },
                onSeeAllCategories = { navController.navigateToTopLevel(TopLevelDestination.CATEGORIES) },
                onSearchClick = { navController.navigate(AppRoute.Search) },
                onCartClick = openCart,
                onProfileClick = { navController.navigateToTopLevel(TopLevelDestination.PROFILE) }
            )
        }

        composable<AppRoute.Categories> {
            CategoriesScreen(
                onCategoryClick = openCategory,
                onSearchClick = { navController.navigate(AppRoute.Search) }
            )
        }

        composable<AppRoute.Wishlist> {
            WishlistScreen(
                onProductClick = openProduct,
                onCartClick = openCart,
                onStartShopping = { navController.navigateToTopLevel(TopLevelDestination.HOME) }
            )
        }

        composable<AppRoute.Cart> {
            CartScreen(
                onProductClick = openProduct,
                onCheckoutClick = { navController.navigate(AppRoute.Checkout) },
                onStartShopping = { navController.navigateToTopLevel(TopLevelDestination.HOME) }
            )
        }

        composable<AppRoute.Profile> {
            ProfileScreen(
                onEditProfile = { navController.navigate(AppRoute.EditProfile) },
                onOrdersClick = { navController.navigate(AppRoute.Orders) },
                onWishlistClick = { navController.navigateToTopLevel(TopLevelDestination.WISHLIST) },
                onSettingsClick = { navController.navigate(AppRoute.Settings) },
                onReviewsClick = { navController.navigate(AppRoute.MyReviews) }
            )
        }

        // ---- Shopping ----
        composable<AppRoute.ProductList> {
            ProductListScreen(onBackClick = goBack, onProductClick = openProduct, onCartClick = openCart)
        }

        composable<AppRoute.ProductDetails> {
            ProductDetailsScreen(
                onBackClick = goBack,
                onProductClick = openProduct,
                onCartClick = openCart,
                onCheckout = { navController.navigate(AppRoute.Checkout) },
                onWriteReview = writeReview
            )
        }

        composable<AppRoute.Search> {
            SearchScreen(onBackClick = goBack, onProductClick = openProduct, onCategoryClick = openCategory)
        }

        // ---- Checkout, payment, receipts ----
        composable<AppRoute.Checkout> {
            CheckoutScreen(
                onBackClick = goBack,
                onGoToPayment = { orderId, phone ->
                    navController.navigate(AppRoute.Payment(orderId, phone, fromCheckout = true)) {
                        // The order now exists; going back should not re-open checkout.
                        popUpTo(AppRoute.Checkout) { inclusive = true }
                    }
                }
            )
        }

        composable<AppRoute.Payment> { entry ->
            val route = entry.toRoute<AppRoute.Payment>()
            PaymentScreen(
                onBackClick = goBack,
                onShowReceipt = { orderId ->
                    navController.navigate(AppRoute.Receipt(orderId)) {
                        if (route.fromCheckout) {
                            popUpTo<AppRoute.Payment> { inclusive = true }
                        } else {
                            // Came from an existing receipt ("Pay now"): replace it with the fresh one.
                            popUpTo<AppRoute.Receipt> { inclusive = true }
                        }
                    }
                }
            )
        }

        composable<AppRoute.Receipt> {
            ReceiptScreen(
                onBackClick = goBack,
                onContinueShopping = { navController.navigateHomeClearingStack() },
                onPayNow = { orderId, phone ->
                    navController.navigate(AppRoute.Payment(orderId, phone, fromCheckout = false))
                },
                onWriteReview = writeReview
            )
        }

        composable<AppRoute.Orders> {
            OrdersScreen(
                onBackClick = goBack,
                onOrderClick = { orderId -> navController.navigate(AppRoute.Receipt(orderId)) },
                onStartShopping = { navController.navigateHomeClearingStack() }
            )
        }

        // ---- Reviews ----
        composable<AppRoute.WriteReview> {
            WriteReviewScreen(
                onBackClick = goBack,
                onMyReviewsClick = {
                    navController.navigate(AppRoute.MyReviews) {
                        popUpTo<AppRoute.WriteReview> { inclusive = true }
                    }
                }
            )
        }

        composable<AppRoute.MyReviews> {
            MyReviewsScreen(
                onBackClick = goBack,
                onWriteReview = writeReview,
                onStartShopping = { navController.navigateHomeClearingStack() }
            )
        }

        // ---- Account ----
        composable<AppRoute.EditProfile> { EditProfileScreen(onBackClick = goBack) }

        composable<AppRoute.Settings> { SettingsScreen(onBackClick = goBack) }
    }
}
