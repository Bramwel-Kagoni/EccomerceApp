package com.bramwel.eccomerceapp.features.checkout.domain.usecase

import com.bramwel.eccomerceapp.core.common.AppError
import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.core.common.MpesaPhone
import com.bramwel.eccomerceapp.features.cart.domain.repository.CartRepository
import com.bramwel.eccomerceapp.features.orders.domain.model.CartLine
import com.bramwel.eccomerceapp.features.orders.domain.model.DeliveryDetails
import com.bramwel.eccomerceapp.features.orders.domain.model.DeliveryMethod
import com.bramwel.eccomerceapp.features.orders.domain.model.Order
import com.bramwel.eccomerceapp.features.orders.domain.model.OrderQuote
import com.bramwel.eccomerceapp.features.orders.domain.repository.OrderRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Turns the current cart into a backend order (prices are re-verified server side). */
class PlaceOrderUseCase @Inject constructor(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository,
    private val validateDeliveryDetails: ValidateDeliveryDetailsUseCase
) {
    suspend operator fun invoke(
        details: DeliveryDetails,
        deliveryMethod: DeliveryMethod,
        promoCode: String
    ): AppResult<Order> {
        if (validateDeliveryDetails(details).hasErrors) {
            return AppResult.Error(AppError.Validation("Please fix the highlighted fields."))
        }
        val lines = currentLines()
        if (lines.isEmpty()) return AppResult.Error(AppError.Validation("Your cart is empty."))

        val normalizedPhone = MpesaPhone.normalize(details.phone) ?: details.phone
        return orderRepository.placeOrder(
            lines = lines,
            details = details.copy(phone = normalizedPhone),
            deliveryMethod = deliveryMethod,
            promoCode = promoCode
        )
    }

    suspend fun quote(deliveryMethod: DeliveryMethod, promoCode: String): AppResult<OrderQuote> {
        val lines = currentLines()
        if (lines.isEmpty()) return AppResult.Error(AppError.Validation("Your cart is empty."))
        return orderRepository.getQuote(lines, deliveryMethod, promoCode)
    }

    private suspend fun currentLines(): List<CartLine> =
        cartRepository.observeCart().first().map { CartLine(it.product.id, it.quantity) }
}
