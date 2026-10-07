package com.bramwel.eccomerceapp.core.common

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Parses the backend's ISO-8601 timestamps without java.time (minSdk 24). */
object DateFormatter {

    private val fractionRegex = Regex("""\.\d+""")

    fun parseIso(value: String?): Date? {
        if (value.isNullOrBlank()) return null
        val cleaned = value.replace(fractionRegex, "")
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(cleaned)
        } catch (e: ParseException) {
            null
        }
    }

    fun display(value: String?): String {
        val date = parseIso(value) ?: return value.orEmpty()
        return SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(date)
    }

    fun displayDate(value: String?): String {
        val date = parseIso(value) ?: return value.orEmpty()
        return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
    }
}
