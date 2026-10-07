package com.bramwel.eccomerceapp.core.common

object Constants {
    /** Shown on receipts and share texts. Keep in sync with @string/app_name. */
    const val STORE_NAME = "Soko"

    /** DummyJSON prices are in USD. Keep equal to the backend's USD_TO_KES_RATE. */
    const val USD_TO_KES_RATE = "129"

    /** Only used for the cart hint; the backend quote is authoritative at checkout. */
    const val FREE_DELIVERY_THRESHOLD = 5_000L

    /** Replace with your real support line (international format, no +). */
    const val SUPPORT_WHATSAPP_NUMBER = "254700000000"
    const val SUPPORT_EMAIL = "support@soko.shop"

    const val PAYMENT_POLL_INTERVAL_MS = 3_000L
    const val PAYMENT_TIMEOUT_MS = 120_000L

    const val MAX_QUANTITY_PER_ITEM = 20
}
