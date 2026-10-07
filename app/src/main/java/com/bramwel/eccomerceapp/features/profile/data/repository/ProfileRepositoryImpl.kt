package com.bramwel.eccomerceapp.features.profile.data.repository

import android.content.Context
import android.net.Uri
import com.bramwel.eccomerceapp.core.common.AppError
import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.core.common.IoDispatcher
import com.bramwel.eccomerceapp.core.datastore.UserPreferences
import com.bramwel.eccomerceapp.features.profile.domain.model.UserProfile
import com.bramwel.eccomerceapp.features.profile.domain.repository.ProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: UserPreferences,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ProfileRepository {

    override fun observeProfile(): Flow<UserProfile> = preferences.profile.map { prefs ->
        UserProfile(
            name = prefs.name,
            email = prefs.email,
            phone = prefs.phone,
            address = prefs.address,
            city = prefs.city,
            photoPath = prefs.photoPath?.takeIf { File(it).exists() }
        )
    }

    override suspend fun updateProfile(profile: UserProfile) {
        preferences.updateProfile(
            name = profile.name.trim(),
            email = profile.email.trim(),
            phone = profile.phone.trim(),
            address = profile.address.trim(),
            city = profile.city.trim()
        )
    }

    override suspend fun updatePhoto(sourceUri: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val dir = File(context.filesDir, "profile").apply { mkdirs() }
            // New file name each time so image caches never show the old photo.
            val target = File(dir, "avatar_${System.currentTimeMillis()}.jpg")
            val input = context.contentResolver.openInputStream(Uri.parse(sourceUri))
                ?: return@withContext AppResult.Error(AppError.Validation("Couldn't open that image."))
            input.use { stream ->
                target.outputStream().use { output -> stream.copyTo(output) }
            }
            if (target.length() > MAX_PHOTO_BYTES) {
                target.delete()
                return@withContext AppResult.Error(AppError.Validation("Please pick an image smaller than 10 MB."))
            }
            val previous = preferences.profile.first().photoPath
            preferences.updatePhotoPath(target.absolutePath)
            previous?.let { File(it).delete() }
            AppResult.Success(Unit)
        } catch (e: IOException) {
            AppResult.Error(AppError.Unknown("Couldn't save your photo. Please try again."))
        } catch (e: SecurityException) {
            AppResult.Error(AppError.Unknown("Permission to read that image was denied."))
        }
    }

    override suspend fun removePhoto() {
        val previous = preferences.profile.first().photoPath
        preferences.updatePhotoPath(null)
        withContext(ioDispatcher) { previous?.let { File(it).delete() } }
    }

    private companion object {
        const val MAX_PHOTO_BYTES = 10L * 1024 * 1024
    }
}
