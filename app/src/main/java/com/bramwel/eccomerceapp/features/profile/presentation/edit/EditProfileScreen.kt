package com.bramwel.eccomerceapp.features.profile.presentation.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bramwel.eccomerceapp.core.ui.components.AppButton
import com.bramwel.eccomerceapp.core.ui.components.AppTextField
import com.bramwel.eccomerceapp.core.ui.components.AppTopBar
import com.bramwel.eccomerceapp.core.ui.components.LoadingIndicator
import com.bramwel.eccomerceapp.core.ui.theme.EccomerceAppTheme

@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.saved.collect { onBackClick() } }
    EditProfileContent(state, viewModel::onFieldChanged, viewModel::save, onBackClick)
}

@Composable
fun EditProfileContent(
    state: EditProfileUiState,
    onFieldChanged: (ProfileField, String) -> Unit,
    onSave: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = { AppTopBar(title = "Edit profile", onBackClick = onBackClick) },
        bottomBar = {
            AppButton(
                text = "Save changes",
                onClick = onSave,
                loading = state.isSaving,
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp)
            )
        }
    ) { padding ->
        if (state.isLoading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "These details pre-fill checkout so you can pay faster. They're stored on this device only.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AppTextField(
                value = state.name, onValueChange = { onFieldChanged(ProfileField.NAME, it) }, label = "Full name",
                leadingIcon = Icons.Outlined.Person, error = state.nameError,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
            AppTextField(
                value = state.email, onValueChange = { onFieldChanged(ProfileField.EMAIL, it) }, label = "Email",
                leadingIcon = Icons.Outlined.Email, error = state.emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            AppTextField(
                value = state.phone, onValueChange = { onFieldChanged(ProfileField.PHONE, it) },
                label = "M-Pesa phone number", placeholder = "0712 345 678",
                leadingIcon = Icons.Outlined.PhoneAndroid, error = state.phoneError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            AppTextField(
                value = state.address, onValueChange = { onFieldChanged(ProfileField.ADDRESS, it) },
                label = "Delivery address", leadingIcon = Icons.Outlined.Home,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
            AppTextField(
                value = state.city, onValueChange = { onFieldChanged(ProfileField.CITY, it) },
                label = "Town / city", leadingIcon = Icons.Outlined.LocationCity,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditProfilePreview() {
    EccomerceAppTheme {
        EditProfileContent(
            state = EditProfileUiState(isLoading = false, name = "Kagoni", phone = "0712", phoneError = "Enter a valid Safaricom number"),
            onFieldChanged = { _, _ -> }, onSave = {}, onBackClick = {}
        )
    }
}
