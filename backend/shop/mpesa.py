"""
Minimal Safaricom Daraja client: OAuth token, STK push (Lipa Na M-Pesa Online)
and STK push status query. Credentials stay on the server.
"""
from __future__ import annotations

import base64
import logging
import re
from dataclasses import dataclass
from datetime import datetime
from zoneinfo import ZoneInfo

import requests
from django.conf import settings
from django.core.cache import cache

logger = logging.getLogger(__name__)

SANDBOX_URL = "https://sandbox.safaricom.co.ke"
PRODUCTION_URL = "https://api.safaricom.co.ke"
TOKEN_CACHE_KEY = "mpesa-access-token"
STILL_PROCESSING_CODE = "500.001.1001"


class MpesaError(Exception):
    def __init__(self, message: str, *, retryable: bool = False):
        super().__init__(message)
        self.retryable = retryable


@dataclass
class StkPushResponse:
    merchant_request_id: str
    checkout_request_id: str
    customer_message: str


@dataclass
class StkQueryResponse:
    processing: bool
    result_code: str = ""
    result_desc: str = ""


def normalize_phone(raw: str) -> str:
    """0712345678 / +254712345678 / 712345678 -> 254712345678. Raises ValueError."""
    digits = re.sub(r"\D", "", raw or "")
    if digits.startswith("254") and len(digits) == 12:
        normalized = digits
    elif digits.startswith("0") and len(digits) == 10:
        normalized = "254" + digits[1:]
    elif len(digits) == 9 and digits[0] in "17":
        normalized = "254" + digits
    else:
        raise ValueError("Enter a valid Safaricom number, e.g. 0712 345 678.")
    if normalized[3] not in "17":
        raise ValueError("Enter a valid Safaricom number, e.g. 0712 345 678.")
    return normalized


def _base_url() -> str:
    return PRODUCTION_URL if settings.MPESA_ENVIRONMENT == "production" else SANDBOX_URL


def _timestamp() -> str:
    return datetime.now(ZoneInfo("Africa/Nairobi")).strftime("%Y%m%d%H%M%S")


def _password(timestamp: str) -> str:
    raw = f"{settings.MPESA_SHORTCODE}{settings.MPESA_PASSKEY}{timestamp}"
    return base64.b64encode(raw.encode()).decode()


def callback_url() -> str:
    return f"{settings.MPESA_CALLBACK_BASE_URL.rstrip('/')}/api/mpesa/callback/{settings.MPESA_CALLBACK_TOKEN}/"


def _ensure_configured() -> None:
    missing = [
        name
        for name in ("MPESA_CONSUMER_KEY", "MPESA_CONSUMER_SECRET", "MPESA_PASSKEY", "MPESA_SHORTCODE")
        if not getattr(settings, name)
    ]
    if missing:
        raise MpesaError("M-Pesa is not configured on the server: missing " + ", ".join(missing))


def get_access_token() -> str:
    token = cache.get(TOKEN_CACHE_KEY)
    if token:
        return token
    _ensure_configured()
    try:
        response = requests.get(
            f"{_base_url()}/oauth/v1/generate",
            params={"grant_type": "client_credentials"},
            auth=(settings.MPESA_CONSUMER_KEY, settings.MPESA_CONSUMER_SECRET),
            timeout=settings.MPESA_TIMEOUT_SECONDS,
        )
    except requests.RequestException as exc:
        raise MpesaError("Could not reach M-Pesa. Please try again.", retryable=True) from exc
    if not response.ok:
        logger.error("Daraja OAuth failed: %s %s", response.status_code, response.text[:300])
        raise MpesaError("M-Pesa authentication failed. Check the server credentials.")
    data = response.json()
    token = data["access_token"]
    expires_in = int(data.get("expires_in", 3599))
    cache.set(TOKEN_CACHE_KEY, token, max(expires_in - 60, 60))
    return token


def _post(path: str, payload: dict) -> requests.Response:
    token = get_access_token()
    try:
        return requests.post(
            f"{_base_url()}{path}",
            json=payload,
            headers={"Authorization": f"Bearer {token}"},
            timeout=settings.MPESA_TIMEOUT_SECONDS,
        )
    except requests.RequestException as exc:
        raise MpesaError("Could not reach M-Pesa. Please try again.", retryable=True) from exc


def stk_push(*, phone: str, amount: int, account_reference: str, description: str) -> StkPushResponse:
    timestamp = _timestamp()
    payload = {
        "BusinessShortCode": settings.MPESA_SHORTCODE,
        "Password": _password(timestamp),
        "Timestamp": timestamp,
        "TransactionType": settings.MPESA_TRANSACTION_TYPE,
        "Amount": int(amount),
        "PartyA": phone,
        "PartyB": settings.MPESA_PARTY_B,
        "PhoneNumber": phone,
        "CallBackURL": callback_url(),
        "AccountReference": account_reference[:12],
        "TransactionDesc": description[:13],
    }
    response = _post("/mpesa/stkpush/v1/processrequest", payload)
    try:
        data = response.json()
    except ValueError:
        data = {}
    if response.ok and str(data.get("ResponseCode")) == "0":
        return StkPushResponse(
            merchant_request_id=data.get("MerchantRequestID", ""),
            checkout_request_id=data["CheckoutRequestID"],
            customer_message=data.get("CustomerMessage", "Check your phone and enter your M-Pesa PIN."),
        )
    logger.error("STK push rejected: %s %s", response.status_code, str(data)[:500])
    message = data.get("errorMessage") or data.get("ResponseDescription") or "M-Pesa rejected the request."
    raise MpesaError(message, retryable=response.status_code >= 500)


def stk_query(checkout_request_id: str) -> StkQueryResponse:
    timestamp = _timestamp()
    payload = {
        "BusinessShortCode": settings.MPESA_SHORTCODE,
        "Password": _password(timestamp),
        "Timestamp": timestamp,
        "CheckoutRequestID": checkout_request_id,
    }
    response = _post("/mpesa/stkpushquery/v1/query", payload)
    try:
        data = response.json()
    except ValueError:
        data = {}
    if data.get("errorCode") == STILL_PROCESSING_CODE:
        return StkQueryResponse(processing=True)
    if "ResultCode" in data:
        return StkQueryResponse(
            processing=False,
            result_code=str(data.get("ResultCode")),
            result_desc=str(data.get("ResultDesc", "")),
        )
    # Rate limits / transient errors: treat as still processing, the app polls again.
    logger.warning("STK query inconclusive: %s %s", response.status_code, str(data)[:300])
    return StkQueryResponse(processing=True)
