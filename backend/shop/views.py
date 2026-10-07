import hmac
import logging
import re

from django.conf import settings
from django.shortcuts import get_object_or_404
from rest_framework import status
from rest_framework.decorators import api_view
from rest_framework.exceptions import ValidationError
from rest_framework.response import Response

from . import mpesa, services
from .models import Order, Payment
from .pricing import PricingError, build_quote
from .serializers import (
    CreateOrderInputSerializer,
    OrderSerializer,
    PayInputSerializer,
    QuoteInputSerializer,
    QuoteSerializer,
)

logger = logging.getLogger(__name__)
CLIENT_ID_PATTERN = re.compile(r"^[A-Za-z0-9-]{8,64}$")


def _client_id(request) -> str:
    client_id = request.headers.get("X-Client-Id", "")
    if not CLIENT_ID_PATTERN.match(client_id):
        raise ValidationError({"detail": "Missing or invalid X-Client-Id header."})
    return client_id


def _first_error(errors) -> str:
    """Flatten DRF validation errors into one human readable message."""
    if isinstance(errors, dict):
        for value in errors.values():
            return _first_error(value)
    if isinstance(errors, list) and errors:
        return _first_error(errors[0])
    return str(errors)


def _bad_request(serializer):
    return Response({"detail": _first_error(serializer.errors), "errors": serializer.errors},
                    status=status.HTTP_400_BAD_REQUEST)


@api_view(["GET"])
def health(request):
    return Response({"status": "ok", "mpesa_environment": settings.MPESA_ENVIRONMENT})


@api_view(["POST"])
def quote(request):
    serializer = QuoteInputSerializer(data=request.data)
    if not serializer.is_valid():
        return _bad_request(serializer)
    data = serializer.validated_data
    try:
        result = build_quote(data["items"], data["delivery_method"], data.get("promo_code", ""))
    except PricingError as exc:
        return Response({"detail": str(exc)}, status=status.HTTP_400_BAD_REQUEST)
    payload = QuoteSerializer(
        {
            **result.__dict__,
            "items": [{**i.__dict__, "line_total": i.line_total} for i in result.items],
        }
    ).data
    return Response(payload)


@api_view(["GET", "POST"])
def orders(request):
    client_id = _client_id(request)
    if request.method == "GET":
        queryset = Order.objects.filter(client_id=client_id).prefetch_related("items", "payments")[:100]
        return Response(OrderSerializer(queryset, many=True).data)

    serializer = CreateOrderInputSerializer(data=request.data)
    if not serializer.is_valid():
        return _bad_request(serializer)
    try:
        order = services.create_order(client_id=client_id, data=serializer.validated_data)
    except (PricingError, ValueError) as exc:
        return Response({"detail": str(exc)}, status=status.HTTP_400_BAD_REQUEST)
    return Response(OrderSerializer(order).data, status=status.HTTP_201_CREATED)


@api_view(["GET"])
def order_detail(request, order_id):
    order = get_object_or_404(Order, pk=order_id, client_id=_client_id(request))
    return Response(OrderSerializer(order).data)


@api_view(["POST"])
def pay_order(request, order_id):
    order = get_object_or_404(Order, pk=order_id, client_id=_client_id(request))
    serializer = PayInputSerializer(data=request.data)
    if not serializer.is_valid():
        return _bad_request(serializer)
    try:
        payment, message = services.initiate_payment(order, serializer.validated_data["phone"])
    except services.PaymentNotAllowed as exc:
        return Response({"detail": str(exc)}, status=status.HTTP_409_CONFLICT)
    except ValueError as exc:
        return Response({"detail": str(exc)}, status=status.HTTP_400_BAD_REQUEST)
    except mpesa.MpesaError as exc:
        code = status.HTTP_503_SERVICE_UNAVAILABLE if exc.retryable else status.HTTP_502_BAD_GATEWAY
        return Response({"detail": str(exc)}, status=code)
    return Response(
        {
            "checkout_request_id": payment.checkout_request_id,
            "status": payment.status,
            "customer_message": message,
            "amount": payment.amount,
            "phone": payment.phone,
        },
        status=status.HTTP_202_ACCEPTED,
    )


@api_view(["GET"])
def payment_status(request, checkout_request_id):
    payment = get_object_or_404(
        Payment.objects.select_related("order"),
        checkout_request_id=checkout_request_id,
        order__client_id=_client_id(request),
    )
    payment = services.refresh_payment(payment)
    return Response(
        {
            "checkout_request_id": payment.checkout_request_id,
            "status": payment.status,
            "result_desc": payment.result_desc,
            "mpesa_receipt_number": payment.mpesa_receipt_number,
            "order": OrderSerializer(payment.order).data,
        }
    )


@api_view(["POST"])
def mpesa_callback(request, token):
    # Daraja callbacks are unsigned: the secret path token is our guard.
    if not hmac.compare_digest(token, settings.MPESA_CALLBACK_TOKEN):
        return Response(status=status.HTTP_404_NOT_FOUND)
    try:
        services.handle_callback(request.data)
    except Exception:  # never make Safaricom retry forever because of our bug
        logger.exception("Failed to process M-Pesa callback")
    return Response({"ResultCode": 0, "ResultDesc": "Accepted"})


# ---------------------------------------------------------------------------
# Reviews
# ---------------------------------------------------------------------------
from . import reviews as review_rules  # noqa: E402
from .models import Review  # noqa: E402
from .serializers import (  # noqa: E402
    MyReviewSerializer,
    ProductReviewsSerializer,
    ReviewInputSerializer,
)


@api_view(["GET"])
def product_reviews(request, product_id):
    """Approved reviews only — safe to show to everyone."""
    return Response(ProductReviewsSerializer(review_rules.product_summary(product_id)).data)


@api_view(["GET", "POST"])
def my_reviews(request):
    client_id = _client_id(request)
    if request.method == "GET":
        queryset = Review.objects.filter(client_id=client_id)[:200]
        return Response(MyReviewSerializer(queryset, many=True).data)

    serializer = ReviewInputSerializer(data=request.data)
    if not serializer.is_valid():
        return _bad_request(serializer)
    try:
        review = review_rules.submit_review(client_id=client_id, **serializer.validated_data)
    except review_rules.ReviewNotAllowed as exc:
        return Response({"detail": str(exc)}, status=status.HTTP_403_FORBIDDEN)
    return Response(MyReviewSerializer(review).data, status=status.HTTP_201_CREATED)
