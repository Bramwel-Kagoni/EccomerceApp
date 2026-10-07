package com.bramwel.eccomerceapp.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Raw profile values as persisted. The profile feature maps these to its domain model. */
data class ProfilePreferences(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val photoPath: String? = null
)

/**
 * Lightweight, non-sensitive preferences. No secrets or tokens are stored here.
 */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.userDataStore

    private val data: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    val profile: Flow<ProfilePreferences> = data.map { prefs ->
        ProfilePreferences(
            name = prefs[Keys.NAME].orEmpty(),
            email = prefs[Keys.EMAIL].orEmpty(),
            phone = prefs[Keys.PHONE].orEmpty(),
            address = prefs[Keys.ADDRESS].orEmpty(),
            city = prefs[Keys.CITY].orEmpty(),
            photoPath = prefs[Keys.PHOTO_PATH]
        )
    }

    val themeMode: Flow<ThemeMode> = data.map { prefs ->
        prefs[Keys.THEME]?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.SYSTEM
    }

    val onboardingCompleted: Flow<Boolean> = data.map { it[Keys.ONBOARDING_DONE] ?: false }

    val recentSearches: Flow<List<String>> = data.map { prefs ->
        prefs[Keys.RECENT_SEARCHES]
            ?.split(SEPARATOR)
            ?.filter { it.isNotBlank() }
            .orEmpty()
    }

    suspend fun updateProfile(
        name: String,
        email: String,
        phone: String,
        address: String,
        city: String
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.NAME] = name
            prefs[Keys.EMAIL] = email
            prefs[Keys.PHONE] = phone
            prefs[Keys.ADDRESS] = address
            prefs[Keys.CITY] = city
        }
    }

    suspend fun updatePhotoPath(path: String?) {
        dataStore.edit { prefs ->
            if (path == null) prefs.remove(Keys.PHOTO_PATH) else prefs[Keys.PHOTO_PATH] = path
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setOnboardingCompleted() {
        dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    }

    suspend fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        dataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_SEARCHES]?.split(SEPARATOR).orEmpty()
            val updated = (listOf(trimmed) + current.filterNot { it.equals(trimmed, ignoreCase = true) })
                .filter { it.isNotBlank() }
                .take(MAX_RECENT_SEARCHES)
            prefs[Keys.RECENT_SEARCHES] = updated.joinToString(SEPARATOR)
        }
    }

    suspend fun clearRecentSearches() {
        dataStore.edit { it.remove(Keys.RECENT_SEARCHES) }
    }

    /** Stable anonymous id for this install; scopes orders on the backend until login exists. */
    suspend fun clientId(): String {
        dataStore.data.first()[Keys.CLIENT_ID]?.let { return it }
        var id = ""
        dataStore.edit { prefs ->
            id = prefs[Keys.CLIENT_ID] ?: UUID.randomUUID().toString().also { prefs[Keys.CLIENT_ID] = it }
        }
        return id
    }

    private object Keys {
        val NAME = stringPreferencesKey("profile_name")
        val EMAIL = stringPreferencesKey("profile_email")
        val PHONE = stringPreferencesKey("profile_phone")
        val ADDRESS = stringPreferencesKey("profile_address")
        val CITY = stringPreferencesKey("profile_city")
        val PHOTO_PATH = stringPreferencesKey("profile_photo_path")
        val THEME = stringPreferencesKey("theme_mode")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches")
        val CLIENT_ID = stringPreferencesKey("client_id")
    }

    private companion object {
        const val SEPARATOR = "\u001F"
        const val MAX_RECENT_SEARCHES = 8
    }
}
