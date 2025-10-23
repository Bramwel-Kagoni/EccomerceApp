package com.bramwel.eccomerceapp.ui.theme.screens.bottom

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavigationBarItems(
    val icon: ImageVector,
    val route: String
) {
    Person(Icons.Default.Person, "profile"),
    Favorite(Icons.Default.Favorite, "favorites"),
    Settings(Icons.Default.Settings, "settings")
}
