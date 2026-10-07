"""
Server-side pricing. The app never decides what the customer pays:
prices are re-read from the catalog and totals are computed here.
"""
from __future__ import annotations

import logging
from dataclasses import dataclass, field
from decimal import ROUND_HALF_UP, Decimal

import requests
from django.conf import settings
from django.core.cache import cache

from .models import Order, PromoCode

logger = logging.getLogger(__name__)

MAX_QUANTITY_PER_ITEM = 20
CATALOG_CACHE_SECONDS = 10 * 60


class PricingError(Exception):
    """Raised when an order cannot be priced (bad item, catalog down...)."""


@dataclass
class PricedItem:
    product_id: int
    title: str
    thumbnail: str
    unit_price: int
    quantity: int

    @property
    def line_total(self) -> int:
        return self.unit_price * self.quantity


@dataclass
class Quote:
    items: list[PricedItem]
    subtotal: int
    delivery_fee: int
    discount: int
    total: int
    promo_code: str = ""
    promo_valid: bool = False
    promo_message: str = ""
    warnings: list[str] = field(default_factory=list)


def usd_to_kes(usd_price) -> int:
    """Must match the Android app's conversion (HALF_UP to whole shillings)."""
    value = Decimal(str(usd_price)) * Decimal(str(settings.USD_TO_KES_RATE))
    return int(value.quantize(Decimal("1"), rounding=ROUND_HALF_UP))


def fetch_catalog_product(product_id: int) -> dict:
    key = f"catalog-product-{product_id}"
    cached = cache.get(key)
    if cached is not None:
        return cached
    url = f"{settings.CATALOG_BASE_URL.rstrip('/')}/products/{product_id}"
    try:
        response = requests.get(url, timeout=10, params={"select": "id,title,price,thumbnail,stock"})
    except requests.RequestException as exc:
        logger.warning("Catalog unreachable: %s", exc)
        raise PricingError("We couldn't verify product prices right now. Please try again.") from exc
    if response.status_code == 404:
        raise PricingError(f"Product {product_id} is no longer available.")
    if not response.ok:
        raise PricingError("We couldn't verify product prices right now. Please try again.")
    data = response.json()
    cache.set(key, data, CATALOG_CACHE_SECONDS)
    return data


def price_items(raw_items: list[dict]) -> tuple[list[PricedItem], list[str]]:
    if not raw_items:
        raise PricingError("Your cart is empty.")

    merged: dict[int, int] = {}
    for item in raw_items:
        merged[item["product_id"]] = merged.get(item["product_id"], 0) + item["quantity"]

    priced: list[PricedItem] = []
    warnings: list[str] = []
    for product_id, quantity in merged.items():
        if quantity > MAX_QUANTITY_PER_ITEM:
            raise PricingError(f"You can order at most {MAX_QUANTITY_PER_ITEM} of each item.")
        if settings.VERIFY_PRICES_WITH_CATALOG:
            product = fetch_catalog_product(product_id)
            stock = int(product.get("stock") or 0)
            if stock <= 0:
                raise PricingError(f"{product.get('title', 'An item')} is out of stock.")
            if quantity > stock:
                raise PricingError(f"Only {stock} of {product.get('title')} left in stock.")
            priced.append(
                PricedItem(
                    product_id=product_id,
                    title=str(product.get("title", ""))[:200],
                    thumbnail=str(product.get("thumbnail", ""))[:500],
                    unit_price=usd_to_kes(product.get("price", 0)),
                    quantity=quantity,
                )
            )
        else:  # pragma: no cover - only for offline development
            original = next(i for i in raw_items if i["product_id"] == product_id)
            priced.append(
                PricedItem(
                    product_id=product_id,
                    title=original.get("title", f"Product {product_id}"),
                    thumbnail=original.get("thumbnail", ""),
                    unit_price=int(original.get("unit_price", 0)),
                    quantity=quantity,
                )
            )
            warnings.append("Prices were not verified with the catalog.")
    return priced, warnings


def delivery_fee_for(method: str, subtotal: int) -> int:
    if method == Order.DeliveryMethod.EXPRESS:
        return settings.EXPRESS_DELIVERY_FEE
    if subtotal >= settings.FREE_DELIVERY_THRESHOLD:
        return 0
    return settings.STANDARD_DELIVERY_FEE


def apply_promo(code: str, subtotal: int) -> tuple[int, bool, str, str]:
    """Returns (discount, valid, message, normalized_code)."""
    normalized = (code or "").strip().upper()
    if not normalized:
        return 0, False, "", ""
    promo = PromoCode.objects.filter(code=normalized, is_active=True).first()
    if promo is None:
        return 0, False, "This promo code is not valid.", normalized
    if subtotal < promo.min_subtotal:
        return 0, False, f"Spend at least KSh {promo.min_subtotal:,} to use {normalized}.", normalized
    if promo.kind == PromoCode.Kind.PERCENT:
        discount = (subtotal * promo.value) // 100
        if promo.max_discount:
            discount = min(discount, promo.max_discount)
    else:
        discount = promo.value
    discount = min(discount, subtotal)
    return discount, True, promo.description or f"{normalized} applied", normalized


def build_quote(raw_items: list[dict], delivery_method: str, promo_code: str = "") -> Quote:
    items, warnings = price_items(raw_items)
    subtotal = sum(i.line_total for i in items)
    discount, valid, message, normalized = apply_promo(promo_code, subtotal)
    fee = delivery_fee_for(delivery_method, subtotal)
    total = max(subtotal - discount + fee, 0)
    return Quote(
        items=items,
        subtotal=subtotal,
        delivery_fee=fee,
        discount=discount,
        total=total,
        promo_code=normalized if valid else "",
        promo_valid=valid,
        promo_message=message,
        warnings=warnings,
    )
