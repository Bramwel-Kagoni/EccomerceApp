package com.bramwel.eccomerceapp.features.products.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ProductDao {

    @Query("SELECT * FROM products ORDER BY sortIndex")
    abstract fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE category = :slug ORDER BY sortIndex")
    abstract fun observeByCategory(slug: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    abstract fun observeById(id: Int): Flow<ProductEntity?>

    @Query(
        """
        SELECT * FROM products
        WHERE title LIKE '%' || :query || '%'
           OR brand LIKE '%' || :query || '%'
           OR category LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%'
        ORDER BY
            CASE WHEN title LIKE :query || '%' THEN 0 ELSE 1 END,
            rating DESC
        """
    )
    abstract fun search(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM categories ORDER BY sortIndex")
    abstract fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM products")
    abstract suspend fun count(): Int

    @Upsert
    abstract suspend fun upsertProducts(products: List<ProductEntity>)

    @Upsert
    abstract suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM products WHERE id NOT IN (:keepIds)")
    abstract suspend fun deleteProductsNotIn(keepIds: List<Int>)

    @Query("DELETE FROM categories WHERE slug NOT IN (:keepSlugs)")
    abstract suspend fun deleteCategoriesNotIn(keepSlugs: List<String>)

    @Transaction
    open suspend fun replaceCatalog(products: List<ProductEntity>, categories: List<CategoryEntity>) {
        upsertProducts(products)
        upsertCategories(categories)
        if (products.isNotEmpty()) deleteProductsNotIn(products.map { it.id })
        if (categories.isNotEmpty()) deleteCategoriesNotIn(categories.map { it.slug })
    }
}
