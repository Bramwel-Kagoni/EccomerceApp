package com.bramwel.eccomerceapp.ui.theme.screens.home

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.bramwel.eccomerceapp.ui.theme.screens.bottom.BottomNavigationBarItems
import com.bramwel.eccomerceapp.ui.theme.screens.drawer.DrawerFooter
import com.bramwel.eccomerceapp.ui.theme.screens.drawer.DrawerHeader
import com.bramwel.eccomerceapp.ui.theme.screens.drawer.DrawerItemsProvider
import com.bramwel.eccomerceapp.ui.theme.screens.product.DualFloatingActions
import com.bramwel.eccomerceapp.ui.theme.screens.product.ProductScreen
import com.exyte.animatednavbar.AnimatedNavigationBar
import com.exyte.animatednavbar.animation.balltrajectory.Parabolic
import com.exyte.animatednavbar.animation.indendshape.Height
import com.exyte.animatednavbar.animation.indendshape.shapeCornerRadius
import com.exyte.animatednavbar.utils.noRippleClickable
import kotlinx.coroutines.launch



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedItem by remember { mutableStateOf("Home") }

    val drawerItems = DrawerItemsProvider.mainItems(selectedItem) { selectedItem = it }
    val otherItems = DrawerItemsProvider.otherItems(selectedItem) { selectedItem = it }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    DrawerHeader()
                    Spacer(modifier = Modifier.height(8.dp))

                    drawerItems.forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(text = item.title) },
                            selected = selectedItem == item.title,
                            onClick = { item.onClick(scope, navController) { scope.launch { drawerState.close() } } },
                            icon = {
                                Icon(
                                    imageVector = if (selectedItem == item.title && item.selectedIcon != null)
                                        item.selectedIcon else item.icon,
                                    contentDescription = item.title
                                )
                            }
                        )
                    }

                    HorizontalDivider(
                        Modifier.padding(8.dp),
                        DividerDefaults.Thickness,
                        DividerDefaults.color
                    )

                    otherItems.forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(text = item.title) },
                            selected = selectedItem == item.title,
                            onClick = { item.onClick(scope, navController) { scope.launch { drawerState.close() } } },
                            icon = { Icon(item.icon, contentDescription = item.title) }
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    DrawerFooter { scope.launch { drawerState.close() } }
                }
            }
        }
    )  {
        var selectedIndex by remember { mutableIntStateOf(0) }
        val navigationBarItems = remember { BottomNavigationBarItems.entries.toTypedArray() }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Fruit Store",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    navigationIcon = {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .padding(8.dp)
                                .clickable {
                                    scope.launch {
                                        drawerState.open()
                                    }
                                }
                        )
                    }
                )
            },
            floatingActionButton = {
                DualFloatingActions(
                    onCartClick = {
                        // Navigate to cart
                    },
                    onWhatsAppClick = {
                        // Open WhatsApp
                    },
                     // This would come from your view model
                    modifier = Modifier.padding(bottom = 16.dp, end = 16.dp)
                )
            },
            bottomBar = {
                AnimatedNavigationBar(
                    modifier = Modifier.height(64.dp),
                    selectedIndex = selectedIndex,
                    cornerRadius = shapeCornerRadius(cornerRadius = 34.dp),
                    ballAnimation = Parabolic(tween(300)),
                    indentAnimation = Height(tween(300)),
                    barColor = MaterialTheme.colorScheme.primary,
                    ballColor = MaterialTheme.colorScheme.primary
                ) {
                    navigationBarItems.forEach { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .noRippleClickable {
                                    selectedIndex = item.ordinal

                                    // 🚀 Handle navigation for each tab
                                    when (item) {
                                        BottomNavigationBarItems.Person -> {
                                            navController.navigate("profile") {
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                        BottomNavigationBarItems.Favorite -> {
                                            navController.navigate("favorites") {
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                        BottomNavigationBarItems.Settings -> {
                                            navController.navigate("settings") {
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.size(26.dp),
                                imageVector = item.icon,
                                contentDescription = item.name,
                                tint = if (selectedIndex == item.ordinal)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.inversePrimary
                            )
                        }
                    }
                }
            }

        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                ProductScreen()

            }
        }
    }
}



// Simple drawer preview
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(rememberNavController())
}

// Standalone drawer content preview
@Preview(showBackground = true, widthDp = 300)
@Composable
fun DrawerContentPreview() {
    ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
        DrawerHeader()
        Spacer(modifier = Modifier.height(16.dp))
        // You can preview individual sections here
    }
}