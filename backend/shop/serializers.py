from rest_framework import serializers

from . import mpesa
from .models import Order, OrderItem, Payment


class CartItemInputSerializer(serializers.Serializer):
    product_id = serializers.IntegerField(min_value=1)
    quantity = serializers.IntegerField(min_value=1, max_value=20)


class QuoteInputSerializer(serializers.Serializer):
    items = CartItemInputSerializer(many=True, allow_empty=False)
    delivery_method = serializers.ChoiceField(choices=Order.DeliveryMethod.choices, default="standard")
    promo_code = serializers.CharField(max_length=32, required=False, allow_blank=True, default="")


class CreateOrderInputSerializer(QuoteInputSerializer):
    customer_name = serializers.CharField(max_length=120)
    phone = serializers.CharField(max_length=20)
    email = serializers.EmailField(required=False, allow_blank=True, default="")
    address = serializers.CharField(max_length=255)
    city = serializers.CharField(max_length=80)
    notes = serializers.CharField(max_length=255, required=False, allow_blank=True, default="")

    def validate_phone(self, value):
        try:
            return mpesa.normalize_phone(value)
        except ValueError as exc:
            raise serializers.ValidationError(str(exc))


class PayInputSerializer(serializers.Serializer):
    phone = serializers.CharField(max_length=20)

    def validate_phone(self, value):
        try:
            return mpesa.normalize_phone(value)
        except ValueError as exc:
            raise serializers.ValidationError(str(exc))


class QuoteItemSerializer(serializers.Serializer):
    product_id = serializers.IntegerField()
    title = serializers.CharField()
    thumbnail = serializers.CharField()
    unit_price = serializers.IntegerField()
    quantity = serializers.IntegerField()
    line_total = serializers.IntegerField()


class QuoteSerializer(serializers.Serializer):
    items = QuoteItemSerializer(many=True)
    subtotal = serializers.IntegerField()
    delivery_fee = serializers.IntegerField()
    discount = serializers.IntegerField()
    total = serializers.IntegerField()
    promo_code = serializers.CharField()
    promo_valid = serializers.BooleanField()
    promo_message = serializers.CharField()
    free_delivery_threshold = serializers.SerializerMethodField()

    def get_free_delivery_threshold(self, _):
        from django.conf import settings

        return settings.FREE_DELIVERY_THRESHOLD


class OrderItemSerializer(serializers.ModelSerializer):
    line_total = serializers.IntegerField(read_only=True)

    class Meta:
        model = OrderItem
        fields = ["product_id", "title", "thumbnail", "unit_price", "quantity", "line_total"]


class PaymentSerializer(serializers.ModelSerializer):
    class Meta:
        model = Payment
        fields = [
            "checkout_request_id",
            "status",
            "phone",
            "amount",
            "result_code",
            "result_desc",
            "mpesa_receipt_number",
            "transaction_date",
            "created_at",
        ]


class OrderSerializer(serializers.ModelSerializer):
    items = OrderItemSerializer(many=True, read_only=True)
    payment = serializers.SerializerMethodField()

    class Meta:
        model = Order
        fields = [
            "id",
            "order_number",
            "status",
            "customer_name",
            "phone",
            "email",
            "address",
            "city",
            "notes",
            "delivery_method",
            "subtotal",
            "delivery_fee",
            "discount",
            "total",
            "promo_code",
            "created_at",
            "paid_at",
            "items",
            "payment",
        ]

    def get_payment(self, order):
        # Prefer the successful payment, otherwise the latest attempt.
        payment = order.payments.filter(status=Payment.Status.SUCCESS).first() or order.latest_payment
        return PaymentSerializer(payment).data if payment else None


class ReviewInputSerializer(serializers.Serializer):
    product_id = serializers.IntegerField(min_value=1)
    rating = serializers.IntegerField(min_value=1, max_value=5)
    comment = serializers.CharField(min_length=10, max_length=1000, trim_whitespace=True)
    reviewer_name = serializers.CharField(min_length=2, max_length=80, trim_whitespace=True)


class PublicReviewSerializer(serializers.Serializer):
    id = serializers.IntegerField()
    product_id = serializers.IntegerField()
    rating = serializers.IntegerField()
    comment = serializers.CharField()
    reviewer_name = serializers.CharField(source="public_name")
    created_at = serializers.DateTimeField()
    verified_purchase = serializers.SerializerMethodField()

    def get_verified_purchase(self, _):
        return True


class MyReviewSerializer(serializers.Serializer):
    id = serializers.IntegerField()
    product_id = serializers.IntegerField()
    product_title = serializers.CharField()
    product_thumbnail = serializers.CharField()
    rating = serializers.IntegerField()
    comment = serializers.CharField()
    reviewer_name = serializers.CharField()
    status = serializers.CharField()
    moderation_note = serializers.CharField()
    created_at = serializers.DateTimeField()
    updated_at = serializers.DateTimeField()


class ProductReviewsSerializer(serializers.Serializer):
    product_id = serializers.IntegerField()
    average_rating = serializers.FloatField()
    count = serializers.IntegerField()
    reviews = PublicReviewSerializer(many=True)
