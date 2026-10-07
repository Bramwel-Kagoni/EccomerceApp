"""Review rules: verified purchase only, one review per product per customer,
every new or edited review goes back to moderation."""
from __future__ import annotations

from django.db.models import Avg, Count
from django.utils import timezone

from .models import Order, OrderItem, Review


class ReviewNotAllowed(Exception):
    pass


def paid_item_for(client_id: str, product_id: int) -> OrderItem | None:
    return (
        OrderItem.objects.select_related("order")
        .filter(order__client_id=client_id, order__status=Order.Status.PAID, product_id=product_id)
        .order_by("-order__paid_at")
        .first()
    )


def submit_review(*, client_id: str, product_id: int, rating: int, comment: str, reviewer_name: str) -> Review:
    item = paid_item_for(client_id, product_id)
    if item is None:
        raise ReviewNotAllowed("You can only review products you have bought and paid for.")

    review, _ = Review.objects.update_or_create(
        client_id=client_id,
        product_id=product_id,
        defaults={
            "order": item.order,
            "product_title": item.title,
            "product_thumbnail": item.thumbnail,
            "reviewer_name": reviewer_name.strip(),
            "rating": rating,
            "comment": comment.strip(),
            # Every new or edited review must be approved again.
            "status": Review.Status.PENDING,
            "moderation_note": "",
            "moderated_at": None,
        },
    )
    return review


def product_summary(product_id: int) -> dict:
    approved = Review.objects.filter(product_id=product_id, status=Review.Status.APPROVED)
    stats = approved.aggregate(avg=Avg("rating"), count=Count("id"))
    return {
        "product_id": product_id,
        "average_rating": round(stats["avg"] or 0, 1),
        "count": stats["count"],
        "reviews": list(approved[:50]),
    }


def moderate(queryset, status: str, note: str = "") -> int:
    return queryset.update(status=status, moderation_note=note, moderated_at=timezone.now())
