@file:Suppress("DEPRECATION")

package com.bramwel.eccomerceapp.ui.theme.screens.logout


import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay

@Composable
fun LogoutScreen(
    navController: NavController,
    onLoginClick: () -> Unit = {},
    onCancelClick: () -> Unit = {}
) {
    var showSuccess by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scaleAnim = remember { Animatable(0.8f) }
    val offsetAnim = remember { Animatable(100f) }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, delayMillis = 200)
        )
        offsetAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 500, delayMillis = 300)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        // Background decorative elements
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            // Animated floating circles
            AnimatedCircle(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                size = 120.dp,
                delay = 0,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-30).dp, y = (-20).dp)
            )

            AnimatedCircle(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                size = 80.dp,
                delay = 200,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = 60.dp)
            )

            AnimatedCircle(
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                size = 150.dp,
                delay = 400,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-50).dp, y = 40.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Success State
            if (showSuccess) {
                LogoutSuccessContent(
                    onLoginClick = onLoginClick
                )
            } else {
                // Logout Confirmation State
                LogoutConfirmationContent(
                    scaleAnim = scaleAnim,
                    offsetAnim = offsetAnim,
                    isLoading = isLoading,
                    onLogoutClick = {
                        isLoading = true
                        // Simulate logout process
                        // In real app, this would be your actual logout logic
                        // For demo, we'll show success after a delay
                        // Your actual logout code would go here:
                        // viewModel.logout()
                        // authService.signOut()

                        // Simulate API call delay
                        // Remove this in production and handle actual logout
                        // This is just for the animation demo
                        // Your actual implementation might look like:
                        // coroutineScope.launch {
                        //     authRepository.logout()
                        //     showSuccess = true
                        // }
                    },
                    onCancelClick = onCancelClick
                )
            }
        }
    }
}

@Composable
fun LogoutConfirmationContent(
    scaleAnim: Animatable<Float, *>,
    offsetAnim: Animatable<Float, *>,
    isLoading: Boolean,
    onLogoutClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .scale(scaleAnim.value)
            .offset(y = offsetAnim.value.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp)),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Icon Container
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            }

            // Text Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Logout?",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Are you sure you want to logout? You'll need to sign in again to access your account.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Logout Button
                Button(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Yes, Logout",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                // Cancel Button
                Button(
                    onClick = onCancelClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = !isLoading,
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LogoutSuccessContent(
    onLoginClick: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(0.8f) }
    var opacity by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        scale = 1f
    }

    Surface(
        modifier = Modifier
            .scale(scale)
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp)),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Success Icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            // Text Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Logged Out Successfully",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "You have been successfully logged out. Thank you for using our app!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Back to Login Button
            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Back to Login",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun AnimatedCircle(
    color: Color,
    size: Dp,
    delay: Long,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(0f) }
    var alpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        delay(delay)
        scale = 1f
        alpha = 1f
    }

    Box(
        modifier = modifier
            .size(size)
            .background(
                color = color,
                shape = CircleShape
            )
            .scale(scale)
    )
}

@Preview(showBackground = true)
@Composable
fun LogoutScreenPreview() {
    MaterialTheme {
        LogoutScreen(rememberNavController())
    }
}

@Preview(showBackground = true)
@Composable
fun LogoutSuccessPreview() {
    MaterialTheme {
        LogoutSuccessContent(onLoginClick = {})
    }
}