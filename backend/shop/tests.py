from datetime import timedelta
from unittest import mock

from django.core.cache import cache
from django.test import TestCase, override_settings
from django.utils import timezone
from rest_framework.test import APIClient

from . import mpesa
from .models import Order, Payment
from .pricing import usd_to_kes

CLIENT = "test-client-0001"
CATALOG = {
    1: {"id": 1, "title": "Mascara", "price": 9.99, "thumbnail": "https://img/1.png", "stock": 10},
    2: {"id": 2, "title": "Laptop", "price": 1999.99, "thumbnail": "https://img/2.png", "stock": 2},
    3: {"id": 3, "title": "Gone", "price": 5, "thumbnail": "", "stock": 0},
}


def fake_catalog(product_id):
    return CATALOG[product_id]


ORDER_BODY = {
    "customer_name": "Kagoni",
    "phone": "0712345678",
    "address": "Moi Avenue",
    "city": "Nairobi",
    "delivery_method": "standard",
    "items": [{"product_id": 1, "quantity": 2}],
}


@override_settings(MPESA_CALLBACK_TOKEN="secret-token", MPESA_TEST_CHARGE_AMOUNT=0, USD_TO_KES_RATE="129")
@mock.patch("shop.pricing.fetch_catalog_product", side_effect=fake_catalog)
class ShopApiTests(TestCase):
    def setUp(self):
        cache.clear()
        self.api = APIClient()
        self.api.credentials(HTTP_X_CLIENT_ID=CLIENT)

    def create_order(self, **overrides):
        response = self.api.post("/api/orders/", {**ORDER_BODY, **overrides}, format="json")
        self.assertEqual(response.status_code, 201, response.content)
        return response.json()

    # ---- pricing -------------------------------------------------------
    def test_usd_conversion_rounds_half_up(self, _):
        self.assertEqual(usd_to_kes(9.99), 1289)  # 1288.71
        self.assertEqual(usd_to_kes(0.5), 65)  # 64.5 -> 65, not banker's rounding

    def test_quote_adds_standard_delivery_below_threshold(self, _):
        r = self.api.post("/api/orders/quote/", {"items": [{"product_id": 1, "quantity": 1}]}, format="json")
        self.assertEqual(r.status_code, 200)
        self.assertEqual(r.json()["subtotal"], 1289)
        self.assertEqual(r.json()["delivery_fee"], 200)
        self.assertEqual(r.json()["total"], 1489)

    def test_quote_free_delivery_and_promo(self, _):
        r = self.api.post(
            "/api/orders/quote/",
            {"items": [{"product_id": 1, "quantity": 5}], "promo_code": "welcome10"},
            format="json",
        )
        body = r.json()
        self.assertEqual(body["subtotal"], 6445)
        self.assertEqual(body["delivery_fee"], 0)
        self.assertEqual(body["discount"], 644)
        self.assertTrue(body["promo_valid"])
        self.assertEqual(body["total"], 6445 - 644)

    def test_invalid_promo_is_reported_not_applied(self, _):
        r = self.api.post("/api/orders/quote/", {"items": [{"product_id": 1, "quantity": 1}],
                                                 "promo_code": "NOPE"}, format="json")
        self.assertFalse(r.json()["promo_valid"])
        self.assertEqual(r.json()["discount"], 0)

    def test_out_of_stock_rejected(self, _):
        r = self.api.post("/api/orders/quote/", {"items": [{"product_id": 3, "quantity": 1}]}, format="json")
        self.assertEqual(r.status_code, 400)
        self.assertIn("out of stock", r.json()["detail"])

    # ---- orders --------------------------------------------------------
    def test_create_order_uses_server_prices_and_normalizes_phone(self, _):
        order = self.create_order(items=[{"product_id": 1, "quantity": 2, "unit_price": 1}])
        self.assertEqual(order["subtotal"], 2578)
        self.assertEqual(order["phone"], "254712345678")
        self.assertEqual(order["status"], "pending_payment")
        self.assertEqual(order["items"][0]["line_total"], 2578)

    def test_orders_scoped_by_client(self, _):
        order = self.create_order()
        other = APIClient()
        other.credentials(HTTP_X_CLIENT_ID="another-client-99")
        self.assertEqual(other.get(f"/api/orders/{order['id']}/").status_code, 404)
        self.assertEqual(len(self.api.get("/api/orders/").json()), 1)

    def test_missing_client_id_rejected(self, _):
        r = APIClient().get("/api/orders/")
        self.assertEqual(r.status_code, 400)

    def test_bad_phone_rejected(self, _):
        r = self.api.post("/api/orders/", {**ORDER_BODY, "phone": "12345"}, format="json")
        self.assertEqual(r.status_code, 400)
        self.assertIn("Safaricom", r.json()["detail"])

    # ---- payments ------------------------------------------------------
    def pay(self, order):
        with mock.patch("shop.mpesa.stk_push", return_value=mpesa.StkPushResponse("m-1", "ws_CO_1", "Enter PIN")):
            return self.api.post(f"/api/orders/{order['id']}/pay/", {"phone": "0712345678"}, format="json")

    def test_pay_then_success_callback_marks_order_paid(self, _):
        order = self.create_order()
        r = self.pay(order)
        self.assertEqual(r.status_code, 202)
        self.assertEqual(r.json()["checkout_request_id"], "ws_CO_1")

        callback = {"Body": {"stkCallback": {
            "MerchantRequestID": "m-1", "CheckoutRequestID": "ws_CO_1", "ResultCode": 0,
            "ResultDesc": "The service request is processed successfully.",
            "CallbackMetadata": {"Item": [
                {"Name": "Amount", "Value": order["total"]},
                {"Name": "MpesaReceiptNumber", "Value": "NLJ7RT61SV"},
                {"Name": "TransactionDate", "Value": 20260926102115},
                {"Name": "PhoneNumber", "Value": 254712345678},
            ]}}}}
        r = APIClient().post("/api/mpesa/callback/secret-token/", callback, format="json")
        self.assertEqual(r.json()["ResultCode"], 0)

        status_body = self.api.get("/api/payments/ws_CO_1/").json()
        self.assertEqual(status_body["status"], "success")
        self.assertEqual(status_body["mpesa_receipt_number"], "NLJ7RT61SV")
        self.assertEqual(status_body["order"]["status"], "paid")
        self.assertEqual(status_body["order"]["payment"]["mpesa_receipt_number"], "NLJ7RT61SV")

        # paying again is refused
        self.assertEqual(self.pay(order).status_code, 409)

    def test_callback_with_wrong_token_ignored(self, _):
        order = self.create_order()
        self.pay(order)
        r = APIClient().post("/api/mpesa/callback/wrong/", {"Body": {"stkCallback": {
            "CheckoutRequestID": "ws_CO_1", "ResultCode": 0}}}, format="json")
        self.assertEqual(r.status_code, 404)
        self.assertEqual(Payment.objects.get().status, "pending")

    def test_cancelled_callback(self, _):
        order = self.create_order()
        self.pay(order)
        APIClient().post("/api/mpesa/callback/secret-token/", {"Body": {"stkCallback": {
            "CheckoutRequestID": "ws_CO_1", "ResultCode": 1032, "ResultDesc": "Request cancelled by user"}}},
                         format="json")
        payment = Payment.objects.get()
        self.assertEqual(payment.status, "cancelled")
        self.assertEqual(payment.order.status, Order.Status.PAYMENT_FAILED)

    def test_underpaid_callback_is_not_success(self, _):
        order = self.create_order()
        self.pay(order)
        APIClient().post("/api/mpesa/callback/secret-token/", {"Body": {"stkCallback": {
            "CheckoutRequestID": "ws_CO_1", "ResultCode": 0,
            "CallbackMetadata": {"Item": [{"Name": "Amount", "Value": 1}]}}}}, format="json")
        self.assertEqual(Payment.objects.get().status, "failed")

    def test_status_query_used_when_no_callback(self, _):
        order = self.create_order()
        self.pay(order)
        Payment.objects.update(created_at=timezone.now() - timedelta(seconds=30))
        with mock.patch("shop.mpesa.stk_query",
                        return_value=mpesa.StkQueryResponse(processing=False, result_code="0",
                                                            result_desc="ok")) as q:
            body = self.api.get("/api/payments/ws_CO_1/").json()
        q.assert_called_once()
        self.assertEqual(body["status"], "success")
        self.assertEqual(body["order"]["status"], "paid")

    def test_repeat_pay_within_a_minute_reuses_pending_prompt(self, _):
        order = self.create_order()
        self.pay(order)
        with mock.patch("shop.mpesa.stk_push") as push:
            r = self.api.post(f"/api/orders/{order['id']}/pay/", {"phone": "0712345678"}, format="json")
        push.assert_not_called()
        self.assertEqual(r.json()["checkout_request_id"], "ws_CO_1")

    @override_settings(MPESA_TEST_CHARGE_AMOUNT=1, MPESA_ENVIRONMENT="sandbox")
    def test_sandbox_test_amount(self, _):
        order = self.create_order()
        self.assertEqual(self.pay(order).json()["amount"], 1)


class PhoneTests(TestCase):
    def test_normalize(self):
        for raw in ["0712345678", "+254712345678", "254712345678", "712345678", "0712 345 678", "0110345678"]:
            self.assertTrue(mpesa.normalize_phone(raw).startswith("254"), raw)
        for raw in ["", "12345", "0812345678", "25471234567"]:
            with self.assertRaises(ValueError):
                mpesa.normalize_phone(raw)


@override_settings(MPESA_CALLBACK_TOKEN="secret-token", MPESA_TEST_CHARGE_AMOUNT=0)
@mock.patch("shop.pricing.fetch_catalog_product", side_effect=fake_catalog)
class ReviewTests(TestCase):
    def setUp(self):
        cache.clear()
        self.api = APIClient()
        self.api.credentials(HTTP_X_CLIENT_ID=CLIENT)

    def paid_order(self):
        order = self.api.post("/api/orders/", ORDER_BODY, format="json").json()
        Order.objects.filter(pk=order["id"]).update(status=Order.Status.PAID, paid_at=timezone.now())
        return order

    def review(self, **overrides):
        body = {"product_id": 1, "rating": 5, "comment": "Great mascara, lasts all day!",
                "reviewer_name": "Kagoni Livwege", **overrides}
        return self.api.post("/api/reviews/", body, format="json")

    def test_cannot_review_without_paid_order(self, _):
        self.api.post("/api/orders/", ORDER_BODY, format="json")  # unpaid order
        r = self.review()
        self.assertEqual(r.status_code, 403)

    def test_review_is_pending_and_hidden_until_approved(self, _):
        self.paid_order()
        r = self.review()
        self.assertEqual(r.status_code, 201, r.content)
        self.assertEqual(r.json()["status"], "pending")
        self.assertEqual(r.json()["product_title"], "Mascara")

        public = APIClient().get("/api/products/1/reviews/").json()
        self.assertEqual(public["count"], 0)

        from .models import Review
        from .reviews import moderate
        moderate(Review.objects.all(), Review.Status.APPROVED)
        public = APIClient().get("/api/products/1/reviews/").json()
        self.assertEqual(public["count"], 1)
        self.assertEqual(public["average_rating"], 5.0)
        self.assertEqual(public["reviews"][0]["reviewer_name"], "Kagoni L.")
        self.assertTrue(public["reviews"][0]["verified_purchase"])

    def test_editing_sends_review_back_to_moderation(self, _):
        from .models import Review
        self.paid_order()
        self.review()
        Review.objects.update(status=Review.Status.APPROVED)
        r = self.review(rating=3, comment="Changed my mind, it smudges a bit.")
        self.assertEqual(r.json()["status"], "pending")
        self.assertEqual(Review.objects.count(), 1)
        self.assertEqual(APIClient().get("/api/products/1/reviews/").json()["count"], 0)

    def test_validation(self, _):
        self.paid_order()
        self.assertEqual(self.review(rating=6).status_code, 400)
        self.assertEqual(self.review(comment="short").status_code, 400)

    def test_my_reviews_scoped_to_client(self, _):
        self.paid_order()
        self.review()
        self.assertEqual(len(self.api.get("/api/reviews/").json()), 1)
        other = APIClient()
        other.credentials(HTTP_X_CLIENT_ID="another-client-99")
        self.assertEqual(other.get("/api/reviews/").json(), [])
