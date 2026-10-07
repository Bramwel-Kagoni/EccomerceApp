"""Order + payment orchestration. Views stay thin; rules live here."""
from __future__ import annotations

import logging
from datetime import timedelta

from django.conf import settings
from django.db import transaction
from django.utils import timezone

from . import mpesa
from .models import Order, OrderItem, Payment
from .pricing import build_quote

logger = logging.getLogger(__name__)

REUSE_PENDING_WITHIN = timedelta(seconds=60)
QUERY_AFTER = timedelta(seconds=8)
QUERY_INTERVAL = timedelta(seconds=5)
GIVE_UP_AFTER = timedelta(minutes=3)

RESULT_MESSAGES = {
    "0": "Payment received. Thank you!",
    "1": "Your M-Pesa balance is insufficient for this payment.",
    "1001": "M-Pesa is busy with another transaction on this number. Try again shortly.",
    "1019": "The payment request expired. Please try again.",
    "1025": "M-Pesa could not send the prompt. Please try again.",
    "1032": "You cancelled the M-Pesa prompt.",
    "1037": "We couldn't reach your phone. Make sure it is on and try again.",
    "2001": "The M-Pesa PIN entered was wrong.",
}


class PaymentNotAllowed(Exception):
    pass


def create_order(*, client_id: str, data: dict) -> Order:
    quote = build_quote(data["items"], data["delivery_method"], data.get("promo_code", ""))
    phone = mpesa.normalize_phone(data["phone"])
    with transaction.atomic():
        order = Order.objects.create(
            client_id=client_id,
            customer_name=data["customer_name"].strip(),
            phone=phone,
            email=data.get("email", "").strip(),
            address=data["address"].strip(),
            city=data["city"].strip(),
            notes=data.get("notes", "").strip(),
            delivery_method=data["delivery_method"],
            subtotal=quote.subtotal,
            delivery_fee=quote.delivery_fee,
            discount=quote.discount,
            total=quote.total,
            promo_code=quote.promo_code,
        )
        OrderItem.objects.bulk_create(
            OrderItem(
                order=order,
                product_id=i.product_id,
                title=i.title,
                thumbnail=i.thumbnail,
                unit_price=i.unit_price,
                quantity=i.quantity,
            )
            for i in quote.items
        )
    return order


def amount_to_charge(order: Order) -> int:
    if settings.MPESA_ENVIRONMENT != "production" and settings.MPESA_TEST_CHARGE_AMOUNT > 0:
        return settings.MPESA_TEST_CHARGE_AMOUNT
    return order.total


def initiate_payment(order: Order, raw_phone: str) -> tuple[Payment, str]:
    """Sends the STK push. Returns (payment, customer_message)."""
    if order.status == Order.Status.PAID:
        raise PaymentNotAllowed("This order has already been paid.")
    if order.status == Order.Status.CANCELLED:
        raise PaymentNotAllowed("This order was cancelled.")

    phone = mpesa.normalize_phone(raw_phone)
    amount = amount_to_charge(order)
    if amount < 1:
        raise PaymentNotAllowed("Nothing to pay for this order.")
    if amount > settings.MPESA_MAX_AMOUNT:
        raise PaymentNotAllowed(
            f"M-Pesa allows at most KSh {settings.MPESA_MAX_AMOUNT:,} per transaction. "
            "Please split your order."
        )

    recent = order.payments.filter(
        status=Payment.Status.PENDING,
        phone=phone,
        created_at__gte=timezone.now() - REUSE_PENDING_WITHIN,
    ).first()
    if recent:
        return recent, "A payment prompt was already sent. Check your phone."

    result = mpesa.stk_push(
        phone=phone,
        amount=amount,
        account_reference=order.order_number.replace("-", ""),
        description="Order payment",
    )
    payment = Payment.objects.create(
        order=order,
        phone=phone,
        amount=amount,
        merchant_request_id=result.merchant_request_id,
        checkout_request_id=result.checkout_request_id,
    )
    if order.status == Order.Status.PAYMENT_FAILED:
        order.status = Order.Status.PENDING_PAYMENT
        order.save(update_fields=["status"])
    return payment, result.customer_message


def _status_for_code(code: str) -> str:
    if code == "0":
        return Payment.Status.SUCCESS
    if code == "1032":
        return Payment.Status.CANCELLED
    if code in {"1037", "1019"}:
        return Payment.Status.TIMEOUT
    return Payment.Status.FAILED


@transaction.atomic
def apply_result(payment: Payment, *, result_code: str, result_desc: str, metadata: dict | None = None,
                 raw: dict | None = None) -> Payment:
    payment = Payment.objects.select_for_update().get(pk=payment.pk)
    if payment.status == Payment.Status.SUCCESS:
        return payment  # idempotent: never downgrade a confirmed payment

    metadata = metadata or {}
    status = _status_for_code(str(result_code))
    if status == Payment.Status.SUCCESS and "Amount" in metadata:
        paid = int(float(metadata["Amount"]))
        if paid < payment.amount:
            logger.error("Amount mismatch for %s: paid %s expected %s", payment.checkout_request_id, paid,
                         payment.amount)
            status = Payment.Status.FAILED
            result_desc = "Amount paid does not match the order total."

    payment.status = status
    payment.result_code = str(result_code)
    payment.result_desc = RESULT_MESSAGES.get(str(result_code), result_desc)[:255]
    if metadata.get("MpesaReceiptNumber"):
        payment.mpesa_receipt_number = str(metadata["MpesaReceiptNumber"])
    if metadata.get("TransactionDate"):
        payment.transaction_date = str(metadata["TransactionDate"])
    if raw is not None:
        payment.raw_callback = raw
    payment.save()

    order = Order.objects.select_for_update().get(pk=payment.order_id)
    if status == Payment.Status.SUCCESS:
        order.status = Order.Status.PAID
        order.paid_at = timezone.now()
        order.save(update_fields=["status", "paid_at"])
    elif order.status != Order.Status.PAID:
        order.status = Order.Status.PAYMENT_FAILED
        order.save(update_fields=["status"])
    return payment


def handle_callback(body: dict) -> Payment | None:
    callback = (body or {}).get("Body", {}).get("stkCallback", {})
    checkout_id = callback.get("CheckoutRequestID")
    if not checkout_id:
        return None
    payment = Payment.objects.filter(checkout_request_id=checkout_id).first()
    if payment is None:
        logger.warning("Callback for unknown CheckoutRequestID %s", checkout_id)
        return None
    items = callback.get("CallbackMetadata", {}).get("Item", []) or []
    metadata = {item.get("Name"): item.get("Value") for item in items if "Name" in item}
    return apply_result(
        payment,
        result_code=str(callback.get("ResultCode")),
        result_desc=str(callback.get("ResultDesc", "")),
        metadata=metadata,
        raw=body,
    )


def refresh_payment(payment: Payment) -> Payment:
    """If no callback arrived yet, ask Daraja directly (rate limited)."""
    if payment.status != Payment.Status.PENDING:
        return payment
    now = timezone.now()
    if now - payment.created_at < QUERY_AFTER:
        return payment
    if payment.last_queried_at and now - payment.last_queried_at < QUERY_INTERVAL:
        return payment

    Payment.objects.filter(pk=payment.pk).update(last_queried_at=now)
    try:
        result = mpesa.stk_query(payment.checkout_request_id)
    except mpesa.MpesaError as exc:
        logger.warning("STK query failed: %s", exc)
        result = mpesa.StkQueryResponse(processing=True)

    if not result.processing:
        return apply_result(payment, result_code=result.result_code, result_desc=result.result_desc)
    if now - payment.created_at > GIVE_UP_AFTER:
        return apply_result(payment, result_code="1037", result_desc="Timed out")
    payment.refresh_from_db()
    return payment
