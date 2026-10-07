package com.bramwel.eccomerceapp.features.profile.domain.model

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    /** Absolute path of the photo copied into app storage, or null. */
    val photoPath: String? = null
) {
    val displayName: String get() = name.ifBlank { "Guest shopper" }
    val firstName: String get() = name.trim().substringBefore(' ').ifBlank { "there" }
    val initials: String
        get() = name.trim().split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "G" }
    val isComplete: Boolean get() = name.isNotBlank() && phone.isNotBlank() && address.isNotBlank()
}
