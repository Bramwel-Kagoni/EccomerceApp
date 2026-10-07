package com.bramwel.eccomerceapp.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MpesaPhoneTest {

    @Test
    fun `normalizes common Kenyan formats`() {
        listOf("0712345678", "+254712345678", "254712345678", "712345678", "0712 345 678").forEach {
            assertEquals(it, "254712345678", MpesaPhone.normalize(it))
        }
        assertEquals("254110345678", MpesaPhone.normalize("0110345678"))
    }

    @Test
    fun `rejects invalid numbers`() {
        listOf("", "12345", "0812345678", "25471234567").forEach { assertNull(it, MpesaPhone.normalize(it)) }
    }

    @Test
    fun `formats for display`() {
        assertEquals("0712 345 678", MpesaPhone.display("254712345678"))
    }
}
