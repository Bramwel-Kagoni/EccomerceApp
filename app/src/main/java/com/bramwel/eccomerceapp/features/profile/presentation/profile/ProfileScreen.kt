package com.bramwel.eccomerceapp.features.profile.presentation.profile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bramwel.eccomerceapp.core.common.Constants
import com.bramwel.eccomerceapp.core.common.MpesaPhone
import com.bramwel.eccomerceapp.core.ui.components.AppTopBar
import com.bramwel.eccomerceapp.core.ui.components.NetworkImage
import com.bramwel.eccomerceapp.core.ui.theme.BrandIndigo
import com.bramwel.eccomerceapp.core.ui.theme.BrandIndigoDark
import com.bramwel.eccomerceapp.core.ui.theme.EccomerceAppTheme
import com.bramwel.eccomerceapp.features.profile.domain.model.UserProfile
import java.io.File

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onOrdersClick: () -> Unit,
    onWishlistClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onReviewsClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onEvent(ProfileEvent.PhotoPicked(it.toString())) }
    }
    ProfileContent(
        state = state,
        onEvent = viewModel::onEvent,
        onPickPhoto = {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onEditProfile = onEditProfile,
        onOrdersClick = onOrdersClick,
        onWishlistClick = onWishlistClick,
        onSettingsClick = onSettingsClick,
        onReviewsClick = onReviewsClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
    onPickPhoto: () -> Unit,
    onEditProfile: () -> Unit,
    onOrdersClick: () -> Unit,
    onWishlistClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onReviewsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showPhotoOptions by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            onEvent(ProfileEvent.MessageShown)
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "My profile") },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileHeader(
                profile = state.profile,
                isUpdatingPhoto = state.isUpdatingPhoto,
                onAvatarClick = {
                    if (state.profile.photoPath != null) showPhotoOptions = true else onPickPhoto()
                },
                onEditProfile = onEditProfile
            )
            StatsRow(state, onOrdersClick, onWishlistClick)

            MenuCard {
                MenuItem(Icons.Outlined.ReceiptLong, "My orders", "Track orders and view receipts", onOrdersClick)
                MenuDivider()
                MenuItem(Icons.Outlined.FavoriteBorder, "Wishlist", "${state.wishlistCount} saved items", onWishlistClick)
                MenuDivider()
                MenuItem(Icons.Outlined.RateReview, "My reviews", "Review products you've bought", onReviewsClick)
                MenuDivider()
                MenuItem(
                    Icons.Outlined.LocationOn,
                    "Delivery address",
                    listOf(state.profile.address, state.profile.city).filter { it.isNotBlank() }
                        .joinToString(", ").ifBlank { "Add your delivery address" },
                    onEditProfile
                )
            }
            MenuCard {
                MenuItem(Icons.Outlined.Settings, "Settings", "Theme, search history", onSettingsClick)
                MenuDivider()
                MenuItem(Icons.Outlined.SupportAgent, "Help & support", "Chat with us on WhatsApp") {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://wa.me/${Constants.SUPPORT_WHATSAPP_NUMBER}?text=" +
                            Uri.encode("Hello ${Constants.STORE_NAME}, I need help with my order."))
                    )
                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        // No browser or WhatsApp installed.
                    }
                }
                MenuDivider()
                MenuItem(Icons.Outlined.Info, "About", "Version, privacy") { showAbout = true }
            }
        }
    }

    if (showPhotoOptions) {
        ModalBottomSheet(onDismissRequest = { showPhotoOptions = false }) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
                ListItem(
                    headlineContent = { Text("Choose a new photo") },
                    leadingContent = { Icon(Icons.Outlined.PhotoLibrary, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        showPhotoOptions = false
                        onPickPhoto()
                    }
                )
                ListItem(
                    headlineContent = { Text("Remove photo", color = MaterialTheme.colorScheme.error) },
                    leadingContent = {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        showPhotoOptions = false
                        onEvent(ProfileEvent.RemovePhoto)
                    }
                )
            }
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("About ${Constants.STORE_NAME}") },
            text = {
                Text(
                    "Shop the latest products and pay securely with M-Pesa.\n\n" +
                        "Your profile details and photo are stored only on this device. " +
                        "Order details are sent to our server to process your payment and delivery."
                )
            },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("OK") } }
        )
    }
}

@Composable
private fun ProfileHeader(
    profile: UserProfile,
    isUpdatingPhoto: Boolean,
    onAvatarClick: () -> Unit,
    onEditProfile: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(BrandIndigo, BrandIndigoDark)))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.White, CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable(onClick = onAvatarClick),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isUpdatingPhoto -> CircularProgressIndicator(color = Color.White)
                        profile.photoPath != null -> NetworkImage(
                            model = File(profile.photoPath),
                            contentDescription = "Profile photo",
                            modifier = Modifier.fillMaxSize()
                        )
                        else -> Text(profile.initials, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    }
                }
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onAvatarClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = "Change photo", tint = BrandIndigo, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(profile.displayName, style = MaterialTheme.typography.titleLarge, color = Color.White)
            val contact = listOf(profile.email, profile.phone.takeIf { it.isNotBlank() }?.let(MpesaPhone::display))
                .filterNotNull()
                .filter { it.isNotBlank() }
                .joinToString("  •  ")
            if (contact.isNotBlank()) {
                Text(contact, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onEditProfile, border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)) {
                Icon(Icons.Outlined.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (profile.isComplete) "Edit profile" else "Complete your profile", color = Color.White)
            }
        }
    }
}

@Composable
private fun StatsRow(state: ProfileUiState, onOrdersClick: () -> Unit, onWishlistClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Orders", state.orderCount, onOrdersClick, Modifier.weight(1f))
        StatCard("Wishlist", state.wishlistCount, onWishlistClick, Modifier.weight(1f))
        StatCard("In cart", state.cartCount, null, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: Int, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MenuCard(content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) { Column { content() } }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(Modifier.padding(start = 64.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun MenuItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleSmall) },
        supportingContent = { Text(subtitle, maxLines = 1) },
        leadingContent = {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun ProfilePreview() {
    EccomerceAppTheme {
        ProfileContent(
            state = ProfileUiState(
                profile = UserProfile("Kagoni Livwege", "kagoni@example.com", "0712345678", "Moi Avenue", "Nairobi"),
                orderCount = 4, wishlistCount = 7, cartCount = 2
            ),
            onEvent = {}, onPickPhoto = {}, onEditProfile = {}, onOrdersClick = {}, onWishlistClick = {}, onSettingsClick = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun ProfileGuestPreview() {
    EccomerceAppTheme {
        ProfileContent(ProfileUiState(), {}, {}, {}, {}, {}, {})
    }
}
