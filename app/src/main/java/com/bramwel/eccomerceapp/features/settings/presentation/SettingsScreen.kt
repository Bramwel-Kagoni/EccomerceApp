package com.bramwel.eccomerceapp.features.settings.presentation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.StarRate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bramwel.eccomerceapp.BuildConfig
import com.bramwel.eccomerceapp.core.common.Constants
import com.bramwel.eccomerceapp.core.datastore.ThemeMode
import com.bramwel.eccomerceapp.core.ui.components.AppTopBar
import com.bramwel.eccomerceapp.core.ui.theme.EccomerceAppTheme

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(
        state = state,
        onBackClick = onBackClick,
        onThemeSelected = viewModel::setTheme,
        onClearSearchHistory = viewModel::clearSearchHistory,
        onMessageShown = viewModel::onMessageShown
    )
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    onBackClick: () -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onClearSearchHistory: () -> Unit,
    onMessageShown: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }

    fun open(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Nothing installed to handle it.
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Settings", onBackClick = onBackClick) },
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
            SettingsGroup("Appearance") {
                ThemeOption("Use system setting", Icons.Outlined.PhoneAndroid, ThemeMode.SYSTEM, state.themeMode, onThemeSelected)
                ThemeOption("Light", Icons.Outlined.LightMode, ThemeMode.LIGHT, state.themeMode, onThemeSelected)
                ThemeOption("Dark", Icons.Outlined.DarkMode, ThemeMode.DARK, state.themeMode, onThemeSelected)
            }
            SettingsGroup("Privacy") {
                SettingsRow(
                    Icons.Outlined.History,
                    "Clear search history",
                    if (state.recentSearchCount == 0) "No saved searches" else "${state.recentSearchCount} saved searches",
                    enabled = state.recentSearchCount > 0,
                    onClick = onClearSearchHistory
                )
            }
            SettingsGroup("Support") {
                SettingsRow(Icons.Outlined.Email, "Email us", Constants.SUPPORT_EMAIL) {
                    open(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Constants.SUPPORT_EMAIL}")))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow(Icons.Outlined.StarRate, "Rate the app", "Tell us what you think") {
                    open(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow(
                    Icons.Outlined.Info,
                    "Version",
                    "${Constants.STORE_NAME} ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    enabled = false
                ) {}
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) { Column { content() } }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    icon: ImageVector,
    mode: ThemeMode,
    selected: ThemeMode,
    onSelected: (ThemeMode) -> Unit
) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { RadioButton(selected = mode == selected, onClick = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = mode == selected, role = Role.RadioButton, onClick = { onSelected(mode) })
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    EccomerceAppTheme {
        SettingsContent(SettingsUiState(ThemeMode.DARK, recentSearchCount = 3), {}, {}, {}, {})
    }
}
