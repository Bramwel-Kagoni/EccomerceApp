package com.bramwel.eccomerceapp.core.common

sealed interface AppError {
    data object NoInternet : AppError
    data object ServerUnreachable : AppError
    data object Timeout : AppError
    data class Server(val code: Int, val message: String?) : AppError
    data class Validation(val message: String) : AppError
    data class Unknown(val message: String? = null) : AppError
}

fun AppError.toUserMessage(): String = when (this) {
    AppError.NoInternet -> "You're offline. Check your connection and try again."
    AppError.ServerUnreachable -> "We can't reach the shop server right now. Please try again shortly."
    AppError.Timeout -> "The request took too long. Please try again."
    is AppError.Server -> message ?: when (code) {
        404 -> "We couldn't find what you were looking for."
        in 500..599 -> "Something went wrong on our side. Please try again."
        else -> "Request failed ($code)."
    }
    is AppError.Validation -> message
    is AppError.Unknown -> message ?: "Something went wrong. Please try again."
}
