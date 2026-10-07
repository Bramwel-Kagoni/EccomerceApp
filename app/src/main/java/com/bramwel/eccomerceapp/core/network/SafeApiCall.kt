package com.bramwel.eccomerceapp.core.network

import com.bramwel.eccomerceapp.core.common.AppError
import com.bramwel.eccomerceapp.core.common.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Wraps a Retrofit call and converts failures into [AppError]s the UI can show. */
suspend fun <T> safeApiCall(call: suspend () -> T): AppResult<T> = try {
    AppResult.Success(call())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    val detail = e.response()?.errorBody()?.string()?.let(::extractDetail)
    AppResult.Error(AppError.Server(e.code(), detail))
} catch (e: SocketTimeoutException) {
    AppResult.Error(AppError.Timeout)
} catch (e: UnknownHostException) {
    AppResult.Error(AppError.NoInternet)
} catch (e: ConnectException) {
    AppResult.Error(AppError.ServerUnreachable)
} catch (e: IOException) {
    AppResult.Error(AppError.NoInternet)
} catch (e: SerializationException) {
    AppResult.Error(AppError.Unknown("We received an unexpected response from the server."))
} catch (e: IllegalArgumentException) {
    AppResult.Error(AppError.Unknown(e.message))
}

private val errorJson = Json { ignoreUnknownKeys = true }

/** Django REST Framework errors look like {"detail": "..."}. */
private fun extractDetail(body: String): String? = try {
    errorJson.parseToJsonElement(body).jsonObject["detail"]?.jsonPrimitive?.contentOrNull
} catch (e: Exception) {
    null
}
