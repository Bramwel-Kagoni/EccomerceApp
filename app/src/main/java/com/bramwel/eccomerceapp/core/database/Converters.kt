package com.bramwel.eccomerceapp.core.database

import androidx.room.TypeConverter
import com.bramwel.eccomerceapp.features.products.data.local.ReviewEntity
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class Converters {

    @TypeConverter
    fun stringListToJson(value: List<String>): String =
        json.encodeToString(ListSerializer(String.serializer()), value)

    @TypeConverter
    fun jsonToStringList(value: String): List<String> =
        runCatching { json.decodeFromString(ListSerializer(String.serializer()), value) }.getOrDefault(emptyList())

    @TypeConverter
    fun reviewsToJson(value: List<ReviewEntity>): String =
        json.encodeToString(ListSerializer(ReviewEntity.serializer()), value)

    @TypeConverter
    fun jsonToReviews(value: String): List<ReviewEntity> =
        runCatching { json.decodeFromString(ListSerializer(ReviewEntity.serializer()), value) }
            .getOrDefault(emptyList())

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
