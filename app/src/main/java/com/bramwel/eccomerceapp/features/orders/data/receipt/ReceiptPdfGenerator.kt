package com.bramwel.eccomerceapp.features.orders.data.receipt

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.bramwel.eccomerceapp.core.common.AppError
import com.bramwel.eccomerceapp.core.common.AppResult
import com.bramwel.eccomerceapp.core.common.DateFormatter
import com.bramwel.eccomerceapp.core.common.IoDispatcher
import com.bramwel.eccomerceapp.core.common.MpesaPhone
import com.bramwel.eccomerceapp.core.common.formatKes
import com.bramwel.eccomerceapp.features.orders.domain.model.Order
import com.bramwel.eccomerceapp.features.orders.domain.model.OrderStatus
import com.bramwel.eccomerceapp.features.orders.domain.repository.ReceiptExporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Renders an order receipt to an A4 PDF in the app cache (shared through FileProvider). */
@Singleton
class ReceiptPdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ReceiptExporter {

    override suspend fun export(order: Order, storeName: String): AppResult<String> = try {
        AppResult.Success(generate(order, storeName).absolutePath)
    } catch (e: IOException) {
        AppResult.Error(AppError.Unknown("Couldn't create the receipt PDF."))
    }

    private suspend fun generate(order: Order, storeName: String): File = withContext(ioDispatcher) {
        val document = PdfDocument()
        val writer = PageWriter(document)
        try {
            writer.drawReceipt(order, storeName)
            writer.finish()

            val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
            val file = File(dir, "receipt_${order.orderNumber}.pdf")
            FileOutputStream(file).use { document.writeTo(it) }
            file
        } finally {
            document.close()
        }
    }

    private class PageWriter(private val document: PdfDocument) {
        private val pageWidth = 595
        private val pageHeight = 842
        private val margin = 40f
        private val right = pageWidth - margin
        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private lateinit var canvas: Canvas
        private var y = 0f

        private val brand = Color.rgb(91, 76, 240)
        private val muted = Color.rgb(110, 110, 120)
        private val success = Color.rgb(46, 160, 67)

        private val title = paint(22f, bold = true, color = brand)
        private val heading = paint(13f, bold = true)
        private val body = paint(11f)
        private val bodyMuted = paint(10f, color = muted)
        private val bodyBold = paint(11f, bold = true)
        private val totalPaint = paint(15f, bold = true, color = brand)
        private val line = Paint().apply { color = Color.rgb(225, 225, 232); strokeWidth = 1f }

        init {
            newPage()
        }

        fun drawReceipt(order: Order, storeName: String) {
            text(storeName, margin, title)
            textRight(if (order.status == OrderStatus.PAID) "PAID" else order.status.label.uppercase(),
                paint(12f, bold = true, color = if (order.status == OrderStatus.PAID) success else muted))
            y += 26f
            text("Order receipt", margin, bodyMuted)
            y += 24f

            row("Order number", order.orderNumber)
            row("Order date", DateFormatter.display(order.createdAt))
            order.paidAt?.let { row("Paid on", DateFormatter.display(it)) }
            order.payment?.takeIf { it.mpesaReceiptNumber.isNotBlank() }?.let {
                row("M-Pesa receipt", it.mpesaReceiptNumber)
            }
            order.payment?.let { row("Paid from", MpesaPhone.display(it.phone)) }
            divider()

            text("Deliver to", margin, heading); y += 18f
            text(order.customerName, margin, body); y += 15f
            text("${order.address}, ${order.city}", margin, body); y += 15f
            text(MpesaPhone.display(order.phone), margin, body); y += 15f
            text(order.deliveryMethod.label, margin, bodyMuted); y += 8f
            divider()

            text("Item", margin, heading)
            text("Qty", 360f, heading)
            textRight("Amount", heading)
            y += 20f
            order.items.forEach { item ->
                ensureSpace(34f)
                text(item.title.take(48), margin, body)
                text("${item.quantity}", 364f, body)
                textRight(item.lineTotal.formatKes(), body)
                y += 14f
                text("${item.unitPrice.formatKes()} each", margin, bodyMuted)
                y += 18f
            }
            divider()

            row("Subtotal", order.subtotal.formatKes())
            row("Delivery", if (order.deliveryFee == 0L) "Free" else order.deliveryFee.formatKes())
            if (order.discount > 0) {
                row("Discount${if (order.promoCode.isNotBlank()) " (${order.promoCode})" else ""}",
                    "- " + order.discount.formatKes())
            }
            y += 6f
            ensureSpace(30f)
            text("Total", margin, totalPaint)
            textRight(order.total.formatKes(), totalPaint)
            y += 30f
            text("Payment method: M-Pesa", margin, bodyMuted)
            y += 40f
            ensureSpace(40f)
            text("Thank you for shopping with $storeName!", margin, bodyBold)
            y += 16f
            text("Keep this receipt for returns and warranty claims.", margin, bodyMuted)
        }

        fun finish() {
            page?.let(document::finishPage)
            page = null
        }

        private fun newPage() {
            page?.let(document::finishPage)
            pageNumber += 1
            val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(info).also { canvas = it.canvas }
            y = margin + 10f
        }

        private fun ensureSpace(needed: Float) {
            if (y + needed > pageHeight - margin) newPage()
        }

        private fun row(label: String, value: String) {
            ensureSpace(18f)
            text(label, margin, bodyMuted)
            textRight(value, bodyBold)
            y += 18f
        }

        private fun divider() {
            y += 6f
            ensureSpace(16f)
            canvas.drawLine(margin, y, right, y, line)
            y += 20f
        }

        private fun text(value: String, x: Float, paint: Paint) = canvas.drawText(value, x, y, paint)

        private fun textRight(value: String, paint: Paint) =
            canvas.drawText(value, right - paint.measureText(value), y, paint)

        private fun paint(size: Float, bold: Boolean = false, color: Int = Color.rgb(28, 28, 36)) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                this.color = color
                typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
    }
}
