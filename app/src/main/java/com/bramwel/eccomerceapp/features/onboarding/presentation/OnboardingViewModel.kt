package com.bramwel.eccomerceapp.features.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bramwel.eccomerceapp.core.datastore.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: UserPreferences
) : ViewModel() {

    fun complete(onDone: () -> Unit) {
        viewModelScope.launch {
            preferences.setOnboardingCompleted()
            onDone()
        }
    }
}
