package com.bramwel.eccomerceapp.navigation

import SplashScreen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bramwel.eccomerceapp.ui.theme.screens.favorite.FavoriteScreen
import com.bramwel.eccomerceapp.ui.theme.screens.home.HomeScreen
import com.bramwel.eccomerceapp.ui.theme.screens.login.LoginScreen
import com.bramwel.eccomerceapp.ui.theme.screens.logout.LogoutScreen
import com.bramwel.eccomerceapp.ui.theme.screens.profile.ProfileScreen
import com.bramwel.eccomerceapp.ui.theme.screens.register.RegisterScreen
import com.bramwel.eccomerceapp.ui.theme.screens.settings.SettingsScreen
import com.bramwel.eccomerceapp.ui.theme.screens.terms.TermsAndConditionsScreen


@Composable
fun AppNavHost(modifier: Modifier=Modifier,navController:NavHostController= rememberNavController(),startDestination:String= ROUTE_SPLASH) {

    NavHost(navController = navController, modifier=modifier, startDestination = startDestination ){
        composable(ROUTE_SPLASH){
            SplashScreen(navController)
        }

        composable(ROUTE_HOME) {
            HomeScreen(navController = navController)
        }
        composable(ROUTE_PROFILE) {
            ProfileScreen(navController = navController)
        }
        composable(ROUTE_TERMS) {
            TermsAndConditionsScreen(navController = navController)
        }
        composable(ROUTE_REGISTER) {
            RegisterScreen (navController = navController)
        }
        composable(ROUTE_LOGIN) {
            LoginScreen (navController = navController)
        }
        composable(ROUTE_FAVORITES) {
            FavoriteScreen (navController = navController)
        }
        composable(ROUTE_SETTINGS) {
            SettingsScreen (navController = navController)
        }
        composable(ROUTE_LOGOUT) {
            LogoutScreen(navController = navController)
        }

    }


}