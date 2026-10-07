package com.bramwel.eccomerceapp.features.profile.domain.repository

import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.features.profile.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile>
    suspend fun updateProfile(profile: UserProfile)

    /** Copies the picked image (content:// uri) into private storage and saves it as the avatar. */
    suspend fun updatePhoto(sourceUri: String): AppResult<Unit>
    suspend fun removePhoto()
}
