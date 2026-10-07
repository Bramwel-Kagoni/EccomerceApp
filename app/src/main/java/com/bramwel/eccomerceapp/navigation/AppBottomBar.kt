package com.bramwel.eccomerceapp.navigation

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bramwel.eccomerceapp.core.ui.theme.EccomerceAppTheme
import com.exyte.animatednavbar.AnimatedNavigationBar
import com.exyte.animatednavbar.animation.balltrajectory.Parabolic
import com.exyte.animatednavbar.animation.indendshape.Height
import com.exyte.animatednavbar.animation.indendshape.shapeCornerRadius
import com.exyte.animatednavbar.utils.noRippleClickable

/** Animated bottom bar (exyte) with live cart / wishlist badges. */
@Composable
fun AppBottomBar(
    selected: TopLevelDestination,
    cartCount: Int,
    wishlistCount: Int,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        AnimatedNavigationBar(
            modifier = Modifier.height(64.dp),
            selectedIndex = selected.ordinal,
            cornerRadius = shapeCornerRadius(cornerRadius = 34.dp),
            ballAnimation = Parabolic(tween(300)),
            indentAnimation = Height(tween(300)),
            barColor = MaterialTheme.colorScheme.primary,
            ballColor = MaterialTheme.colorScheme.primary
        ) {
            TopLevelDestination.entries.forEach { destination ->
                val isSelected = destination == selected
                val badge = when (destination) {
                    TopLevelDestination.CART -> cartCount
                    TopLevelDestination.WISHLIST -> wishlistCount
                    else -> 0
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .noRippleClickable { onSelect(destination) },
                    contentAlignment = Alignment.Center
                ) {
                    BadgedBox(
                        badge = {
                            if (badge > 0) {
                                Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                                    Text(if (badge > 99) "99+" else badge.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = destination.label,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AppBottomBarPreview() {
    EccomerceAppTheme {
        AppBottomBar(selected = TopLevelDestination.CART, cartCount = 3, wishlistCount = 12, onSelect = {})
    }
}
