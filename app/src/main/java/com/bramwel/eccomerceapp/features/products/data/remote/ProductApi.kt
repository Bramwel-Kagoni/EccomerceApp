package com.bramwel.eccomerceapp.features.products.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/** DummyJSON catalog (https://dummyjson.com/docs/products). */
interface ProductApi {

    /** limit=0 returns the whole catalog (~200 products) in one call. */
    @GET("products")
    suspend fun getProducts(@Query("limit") limit: Int = 0): ProductsResponseDto

    @GET("products/categories")
    suspend fun getCategories(): List<CategoryDto>
}
