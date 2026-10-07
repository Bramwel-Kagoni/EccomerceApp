package com.bramwel.eccomerceapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bramwel.eccomerceapp.navigation.AppBottomBar
import com.bramwel.eccomerceapp.navigation.AppNavHost
import com.bramwel.eccomerceapp.navigation.currentTopLevel
import com.bramwel.eccomerceapp.navigation.navigateToTopLevel

/** App shell: navigation graph + bottom bar that is only shown on the top-level tabs. */
@Composable
fun EcommerceApp(
    navController: NavHostController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val cartCount by viewModel.cartCount.collectAsStateWithLifecycle()
    val wishlistCount by viewModel.wishlistCount.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.destination.currentTopLevel()

    Scaffold(
        // Each screen handles its own status-bar insets.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = currentTab != null,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                if (currentTab != null) {
                    AppBottomBar(
                        selected = currentTab,
                        cartCount = cartCount,
                        wishlistCount = wishlistCount,
                        onSelect = navController::navigateToTopLevel
                    )
                }
            }
        }
    ) { padding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
        )
    }
}
