package com.bramwel.eccomerceapp.core.network

import javax.inject.Qualifier

/** Retrofit for the public product catalog (DummyJSON). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CatalogRetrofit

/** Retrofit for our Django backend (orders + M-Pesa). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BackendRetrofit
