package com.bramwel.eccomerceapp.features.splash.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bramwel.eccomerceapp.core.datastore.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SplashDestination { ONBOARDING, HOME }

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val preferences: UserPreferences
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination?>(null)
    val destination: StateFlow<SplashDestination?> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            val onboarded = async { preferences.onboardingCompleted.first() }
            delay(MIN_SPLASH_MS) // let the brand animation play
            _destination.value = if (onboarded.await()) SplashDestination.HOME else SplashDestination.ONBOARDING
        }
    }

    private companion object {
        const val MIN_SPLASH_MS = 1_800L
    }
}
