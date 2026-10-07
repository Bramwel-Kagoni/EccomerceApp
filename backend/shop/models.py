import secrets
import string
import uuid

from django.db import models


def generate_order_number() -> str:
    alphabet = string.ascii_uppercase + string.digits
    return "ORD-" + "".join(secrets.choice(alphabet) for _ in range(8))


class PromoCode(models.Model):
    class Kind(models.TextChoices):
        PERCENT = "percent", "Percent off"
        FLAT = "flat", "Flat amount off"

    code = models.CharField(max_length=32, unique=True)
    kind = models.CharField(max_length=10, choices=Kind.choices, default=Kind.PERCENT)
    value = models.PositiveIntegerField(help_text="Percent (1-100) or KES amount")
    max_discount = models.PositiveIntegerField(null=True, blank=True, help_text="Cap in KES (percent codes)")
    min_subtotal = models.PositiveIntegerField(default=0)
    description = models.CharField(max_length=120, blank=True)
    is_active = models.BooleanField(default=True)

    def save(self, *args, **kwargs):
        self.code = self.code.strip().upper()
        super().save(*args, **kwargs)

    def __str__(self) -> str:
        return self.code


class Order(models.Model):
    class Status(models.TextChoices):
        PENDING_PAYMENT = "pending_payment", "Pending payment"
        PAID = "paid", "Paid"
        PAYMENT_FAILED = "payment_failed", "Payment failed"
        CANCELLED = "cancelled", "Cancelled"

    class DeliveryMethod(models.TextChoices):
        STANDARD = "standard", "Standard"
        EXPRESS = "express", "Express"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    order_number = models.CharField(max_length=20, unique=True, default=generate_order_number, editable=False)
    client_id = models.CharField(max_length=64, db_index=True)
    status = models.CharField(max_length=20, choices=Status.choices, default=Status.PENDING_PAYMENT)

    customer_name = models.CharField(max_length=120)
    phone = models.CharField(max_length=12)
    email = models.EmailField(blank=True)
    address = models.CharField(max_length=255)
    city = models.CharField(max_length=80)
    notes = models.CharField(max_length=255, blank=True)
    delivery_method = models.CharField(max_length=10, choices=DeliveryMethod.choices, default=DeliveryMethod.STANDARD)

    subtotal = models.PositiveIntegerField()
    delivery_fee = models.PositiveIntegerField()
    discount = models.PositiveIntegerField(default=0)
    total = models.PositiveIntegerField()
    promo_code = models.CharField(max_length=32, blank=True)

    created_at = models.DateTimeField(auto_now_add=True)
    paid_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        ordering = ["-created_at"]

    def __str__(self) -> str:
        return f"{self.order_number} ({self.get_status_display()})"

    @property
    def latest_payment(self):
        return self.payments.order_by("-created_at").first()


class OrderItem(models.Model):
    order = models.ForeignKey(Order, related_name="items", on_delete=models.CASCADE)
    product_id = models.PositiveIntegerField()
    title = models.CharField(max_length=200)
    thumbnail = models.URLField(max_length=500, blank=True)
    unit_price = models.PositiveIntegerField()
    quantity = models.PositiveIntegerField()

    @property
    def line_total(self) -> int:
        return self.unit_price * self.quantity

    def __str__(self) -> str:
        return f"{self.quantity} x {self.title}"


class Payment(models.Model):
    class Status(models.TextChoices):
        PENDING = "pending", "Pending"
        SUCCESS = "success", "Success"
        FAILED = "failed", "Failed"
        CANCELLED = "cancelled", "Cancelled by user"
        TIMEOUT = "timeout", "Timed out"

    order = models.ForeignKey(Order, related_name="payments", on_delete=models.CASCADE)
    phone = models.CharField(max_length=12)
    amount = models.PositiveIntegerField(help_text="Amount actually requested from M-Pesa")
    merchant_request_id = models.CharField(max_length=64, blank=True)
    checkout_request_id = models.CharField(max_length=64, unique=True)
    status = models.CharField(max_length=10, choices=Status.choices, default=Status.PENDING)
    result_code = models.CharField(max_length=16, blank=True)
    result_desc = models.CharField(max_length=255, blank=True)
    mpesa_receipt_number = models.CharField(max_length=32, blank=True)
    transaction_date = models.CharField(max_length=20, blank=True)
    raw_callback = models.JSONField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    last_queried_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        ordering = ["-created_at"]

    def __str__(self) -> str:
        return f"{self.checkout_request_id} ({self.status})"


class Review(models.Model):
    """A product review. Only customers who paid for the product may write one,
    and it stays hidden until an admin approves it."""

    class Status(models.TextChoices):
        PENDING = "pending", "Pending approval"
        APPROVED = "approved", "Approved"
        REJECTED = "rejected", "Rejected"

    product_id = models.PositiveIntegerField(db_index=True)
    client_id = models.CharField(max_length=64, db_index=True)
    order = models.ForeignKey(Order, related_name="reviews", on_delete=models.SET_NULL, null=True, blank=True)
    product_title = models.CharField(max_length=200, blank=True)
    product_thumbnail = models.URLField(max_length=500, blank=True)
    reviewer_name = models.CharField(max_length=80)
    rating = models.PositiveSmallIntegerField()
    comment = models.TextField(max_length=1000)
    status = models.CharField(max_length=10, choices=Status.choices, default=Status.PENDING, db_index=True)
    moderation_note = models.CharField(max_length=255, blank=True, help_text="Shown to the customer if rejected")
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    moderated_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        ordering = ["-created_at"]
        constraints = [
            models.UniqueConstraint(fields=["client_id", "product_id"], name="one_review_per_customer_product"),
        ]

    def __str__(self) -> str:
        return f"{self.product_title or self.product_id} – {self.rating}★ ({self.status})"

    @property
    def public_name(self) -> str:
        """'Kagoni Livwege' -> 'Kagoni L.' so full names aren't published."""
        parts = self.reviewer_name.split()
        if not parts:
            return "Customer"
        return parts[0] if len(parts) == 1 else f"{parts[0]} {parts[-1][0].upper()}."
