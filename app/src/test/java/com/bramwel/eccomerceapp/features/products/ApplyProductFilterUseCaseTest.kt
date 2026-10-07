package com.bramwel.eccomerceapp.features.products

import com.bramwel.eccomerceapp.features.products.domain.model.ProductCollection
import com.bramwel.eccomerceapp.features.products.domain.model.ProductFilter
import com.bramwel.eccomerceapp.features.products.domain.model.ProductSort
import com.bramwel.eccomerceapp.features.products.domain.usecase.ApplyProductFilterUseCase
import com.bramwel.eccomerceapp.features.testProduct
import org.junit.Assert.assertEquals
import org.junit.Test

class ApplyProductFilterUseCaseTest {

    private val apply = ApplyProductFilterUseCase()
    private val products = listOf(
        testProduct(1, price = 300, discount = 20, rating = 4.8),
        testProduct(2, price = 100, discount = 5, rating = 3.9, stock = 0),
        testProduct(3, price = 200, discount = 30, rating = 4.2)
    )

    @Test
    fun `deals are sorted by biggest discount`() {
        val result = apply(products, ProductFilter(), ProductCollection.DEALS)
        assertEquals(listOf(3, 1), result.map { it.id })
    }

    @Test
    fun `price sort and stock filter`() {
        val result = apply(products, ProductFilter(sort = ProductSort.PRICE_LOW_HIGH, inStockOnly = true))
        assertEquals(listOf(3, 1), result.map { it.id })
    }

    @Test
    fun `minimum rating filter`() {
        val result = apply(products, ProductFilter(minRating = 4.0))
        assertEquals(listOf(1, 3), result.map { it.id })
    }
}
