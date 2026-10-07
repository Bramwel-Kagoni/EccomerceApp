package com.bramwel.eccomerceapp.features.profile.presentation.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bramwel.eccomerceapp.core.common.MpesaPhone
import com.bramwel.eccomerceapp.features.profile.domain.model.UserProfile
import com.bramwel.eccomerceapp.features.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val isSaving: Boolean = false
)

enum class ProfileField { NAME, EMAIL, PHONE, ADDRESS, CITY }

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    private var original: UserProfile = UserProfile()
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    init {
        viewModelScope.launch {
            original = profileRepository.observeProfile().first()
            _uiState.value = EditProfileUiState(
                isLoading = false,
                name = original.name,
                email = original.email,
                phone = original.phone,
                address = original.address,
                city = original.city
            )
        }
    }

    fun onFieldChanged(field: ProfileField, value: String) {
        _uiState.update {
            when (field) {
                ProfileField.NAME -> it.copy(name = value, nameError = null)
                ProfileField.EMAIL -> it.copy(email = value, emailError = null)
                ProfileField.PHONE -> it.copy(phone = value, phoneError = null)
                ProfileField.ADDRESS -> it.copy(address = value)
                ProfileField.CITY -> it.copy(city = value)
            }
        }
    }

    fun save() {
        val state = _uiState.value
        val nameError = if (state.name.trim().length < 2) "Enter your name" else null
        val emailError = if (state.email.isNotBlank() && !emailRegex.matches(state.email.trim())) "Enter a valid email" else null
        val phoneError = if (state.phone.isNotBlank() && !MpesaPhone.isValid(state.phone)) "Enter a valid Safaricom number" else null
        if (nameError != null || emailError != null || phoneError != null) {
            _uiState.update { it.copy(nameError = nameError, emailError = emailError, phoneError = phoneError) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            profileRepository.updateProfile(
                original.copy(
                    name = state.name,
                    email = state.email,
                    phone = state.phone,
                    address = state.address,
                    city = state.city
                )
            )
            _uiState.update { it.copy(isSaving = false) }
            _saved.send(Unit)
        }
    }
}
