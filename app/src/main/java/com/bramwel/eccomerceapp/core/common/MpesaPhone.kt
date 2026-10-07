package com.bramwel.eccomerceapp.core.common

/** Safaricom number helpers. Mirrors backend `normalize_phone`. */
object MpesaPhone {

    /** Returns 2547XXXXXXXX / 2541XXXXXXXX or null when invalid. */
    fun normalize(raw: String): String? {
        val digits = raw.filter(Char::isDigit)
        val normalized = when {
            digits.startsWith("254") && digits.length == 12 -> digits
            digits.startsWith("0") && digits.length == 10 -> "254" + digits.drop(1)
            digits.length == 9 && (digits[0] == '7' || digits[0] == '1') -> "254$digits"
            else -> return null
        }
        return normalized.takeIf { it[3] == '7' || it[3] == '1' }
    }

    fun isValid(raw: String): Boolean = normalize(raw) != null

    /** 254712345678 -> 0712 345 678 */
    fun display(raw: String): String {
        val normalized = normalize(raw) ?: return raw
        val local = "0" + normalized.drop(3)
        return "${local.take(4)} ${local.substring(4, 7)} ${local.substring(7)}"
    }
}
