package com.bramwel.eccomerceapp.features.checkout.domain.usecase

import com.bramwel.eccomerceapp.core.common.MpesaPhone
import com.bramwel.eccomerceapp.features.orders.domain.model.DeliveryDetails
import javax.inject.Inject

data class DeliveryFormErrors(
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val city: String? = null
) {
    val hasErrors: Boolean get() = listOf(name, phone, email, address, city).any { it != null }
}

class ValidateDeliveryDetailsUseCase @Inject constructor() {

    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    operator fun invoke(details: DeliveryDetails): DeliveryFormErrors = DeliveryFormErrors(
        name = if (details.name.trim().length < 2) "Enter your full name" else null,
        phone = if (!MpesaPhone.isValid(details.phone)) "Enter a valid Safaricom number (07XX / 01XX)" else null,
        email = if (details.email.isNotBlank() && !emailRegex.matches(details.email.trim())) {
            "Enter a valid email or leave it empty"
        } else null,
        address = if (details.address.trim().length < 3) "Enter a delivery address" else null,
        city = if (details.city.isBlank()) "Enter your town or city" else null
    )
}
