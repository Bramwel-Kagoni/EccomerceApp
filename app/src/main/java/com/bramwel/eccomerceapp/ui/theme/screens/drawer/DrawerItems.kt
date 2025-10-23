package com.bramwel.eccomerceapp.ui.theme.screens.drawer

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Support
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bramwel.eccomerceapp.navigation.ROUTE_HOME
import com.bramwel.eccomerceapp.navigation.ROUTE_PROFILE
import com.bramwel.eccomerceapp.navigation.ROUTE_TERMS
import com.bramwel.eccomerceapp.ui.theme.screens.login.CircleShape
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


data class DrawerItem(
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector? = null,
    val onClick: (CoroutineScope, NavController, () -> Unit) -> Unit
)

object DrawerItemsProvider {

    fun mainItems(
        selectedItem: String,
        onItemSelected: (String) -> Unit
    ): List<DrawerItem> {
        return listOf(
            DrawerItem(
                title = "Home",
                icon = Icons.Outlined.Home,
                selectedIcon = Icons.Filled.Home,
                onClick = { scope, navController, closeDrawer ->
                    onItemSelected("Home")
                    navController.navigate(ROUTE_HOME)
                    scope.launch { closeDrawer() }
                }
            ),
            DrawerItem(
                title = "Profile",
                icon = Icons.Outlined.AccountCircle,
                selectedIcon = Icons.Filled.AccountCircle,
                onClick = { scope, navController, closeDrawer ->
                    onItemSelected("Profile")
                    navController.navigate(ROUTE_PROFILE)
                    scope.launch { closeDrawer() }
                }
            ),
            DrawerItem(
                title = "Favorites",
                icon = Icons.Outlined.FavoriteBorder,
                selectedIcon = Icons.Filled.Favorite,
                onClick = { scope, _, closeDrawer ->
                    onItemSelected("Favorites")
                    scope.launch { closeDrawer() }
                }
            ),
            DrawerItem(
                title = "Settings",
                icon = Icons.Outlined.Settings,
                selectedIcon = Icons.Filled.Settings,
                onClick = { scope, _, closeDrawer ->
                    onItemSelected("Settings")
                    scope.launch { closeDrawer() }
                }
            )
        )
    }

    fun otherItems(
        selectedItem: String,
        onItemSelected: (String) -> Unit
    ): List<DrawerItem> {
        return listOf(
            DrawerItem(
                title = "Terms & Conditions",
                icon = Icons.Default.Policy,
                onClick = { scope, navController, closeDrawer ->
                    onItemSelected("Terms")
                    navController.navigate(ROUTE_TERMS)
                    scope.launch { closeDrawer() }
                }
            ),
            DrawerItem(
                title = "Privacy Policy",
                icon = Icons.Default.Info,
                onClick = { scope, _, closeDrawer ->
                    onItemSelected("Privacy")
                    scope.launch { closeDrawer() }
                }
            ),
            DrawerItem(
                title = "Help & Support",
                icon = Icons.Default.Support,
                onClick = { scope, _, closeDrawer ->
                    onItemSelected("Support")
                    scope.launch { closeDrawer() }
                }
            ),
            DrawerItem(
                title = "Rate App",
                icon = Icons.Default.Star,
                onClick = { scope, _, closeDrawer ->
                    onItemSelected("Rate")
                    scope.launch { closeDrawer() }
                }
            ),
            DrawerItem(
                title = "Share App",
                icon = Icons.Default.Share,
                onClick = { scope, navController, closeDrawer ->
                    onItemSelected("Share")
                    scope.launch { closeDrawer() }

                    val context = navController.context
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Hey, download this app!")
                    }
                    context.startActivity(shareIntent)
                }
            )
        )
    }
}



@Composable
fun DrawerHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                )
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // User avatar
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "User Avatar",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(40.dp)
            )
        }

        // User info
        Column {
            Text(
                text = "John Doe",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = "john.doe@example.com",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
        }

        // Membership badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondary)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Premium Member",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}

@Composable
fun DrawerFooter(onLogout: () -> Unit) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Divider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onLogout)
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = "Logout",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Logout",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error
            )
        }

        // App version
        Text(
            text = "Version 1.0.0",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

