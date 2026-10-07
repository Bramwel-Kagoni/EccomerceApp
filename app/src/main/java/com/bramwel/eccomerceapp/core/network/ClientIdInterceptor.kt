package com.bramwel.eccomerceapp.core.network

import com.bramwel.eccomerceapp.core.datastore.UserPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * There is no login yet, so every install gets a random id that scopes its orders
 * on the backend. Replace with an Authorization header once auth is added.
 */
@Singleton
class ClientIdInterceptor @Inject constructor(
    private val userPreferences: UserPreferences
) : Interceptor {

    @Volatile
    private var cachedId: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        // OkHttp calls interceptors on its own background threads, so blocking once is safe.
        val id = cachedId ?: runBlocking { userPreferences.clientId() }.also { cachedId = it }
        val request = chain.request().newBuilder()
            .header(HEADER, id)
            .build()
        return chain.proceed(request)
    }

    private companion object {
        const val HEADER = "X-Client-Id"
    }
}
