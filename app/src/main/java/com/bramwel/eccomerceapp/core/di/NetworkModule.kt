package com.bramwel.eccomerceapp.core.di

import com.bramwel.eccomerceapp.BuildConfig
import com.bramwel.eccomerceapp.core.network.BackendRetrofit
import com.bramwel.eccomerceapp.core.network.CatalogRetrofit
import com.bramwel.eccomerceapp.core.network.ClientIdInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()

    @Provides
    @Singleton
    @CatalogRetrofit
    fun provideCatalogRetrofit(client: OkHttpClient, json: Json): Retrofit =
        retrofit(BuildConfig.CATALOG_BASE_URL, client, json)

    @Provides
    @Singleton
    @BackendRetrofit
    fun provideBackendRetrofit(
        client: OkHttpClient,
        clientIdInterceptor: ClientIdInterceptor,
        json: Json
    ): Retrofit = retrofit(
        BuildConfig.BACKEND_BASE_URL,
        client.newBuilder().addInterceptor(clientIdInterceptor).build(),
        json
    )

    private fun retrofit(baseUrl: String, client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
