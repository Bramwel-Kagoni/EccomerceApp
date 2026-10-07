from django.contrib import admin

from .models import Order, OrderItem, Payment, PromoCode


class OrderItemInline(admin.TabularInline):
    model = OrderItem
    extra = 0
    readonly_fields = ["product_id", "title", "unit_price", "quantity"]
    fields = readonly_fields


class PaymentInline(admin.TabularInline):
    model = Payment
    extra = 0
    readonly_fields = ["checkout_request_id", "phone", "amount", "status", "result_desc", "mpesa_receipt_number",
                       "created_at"]
    fields = readonly_fields
    can_delete = False


@admin.register(Order)
class OrderAdmin(admin.ModelAdmin):
    list_display = ["order_number", "customer_name", "phone", "total", "status", "created_at"]
    list_filter = ["status", "delivery_method", "created_at"]
    search_fields = ["order_number", "customer_name", "phone", "payments__mpesa_receipt_number"]
    readonly_fields = ["order_number", "client_id", "subtotal", "delivery_fee", "discount", "total", "created_at",
                       "paid_at"]
    inlines = [OrderItemInline, PaymentInline]


@admin.register(Payment)
class PaymentAdmin(admin.ModelAdmin):
    list_display = ["checkout_request_id", "order", "phone", "amount", "status", "mpesa_receipt_number", "created_at"]
    list_filter = ["status"]
    search_fields = ["checkout_request_id", "mpesa_receipt_number", "phone"]
    readonly_fields = [f.name for f in Payment._meta.fields]


@admin.register(PromoCode)
class PromoCodeAdmin(admin.ModelAdmin):
    list_display = ["code", "kind", "value", "max_discount", "min_subtotal", "is_active"]
    list_editable = ["is_active"]


from .models import Review  # noqa: E402
from .reviews import moderate  # noqa: E402


@admin.register(Review)
class ReviewAdmin(admin.ModelAdmin):
    list_display = ["product_title", "rating", "short_comment", "reviewer_name", "status", "created_at"]
    list_filter = ["status", "rating", "created_at"]
    search_fields = ["product_title", "comment", "reviewer_name", "order__order_number"]
    readonly_fields = ["product_id", "product_title", "client_id", "order", "reviewer_name", "rating", "comment",
                       "created_at", "updated_at", "moderated_at"]
    fields = readonly_fields[:7] + ["status", "moderation_note"] + readonly_fields[7:]
    actions = ["approve", "reject"]
    list_per_page = 50

    @admin.display(description="Comment")
    def short_comment(self, obj):
        return (obj.comment[:60] + "…") if len(obj.comment) > 60 else obj.comment

    @admin.action(description="Approve selected reviews")
    def approve(self, request, queryset):
        count = moderate(queryset, Review.Status.APPROVED)
        self.message_user(request, f"{count} review(s) approved and now visible in the app.")

    @admin.action(description="Reject selected reviews")
    def reject(self, request, queryset):
        count = moderate(queryset, Review.Status.REJECTED, "Your review didn't meet our review guidelines.")
        self.message_user(request, f"{count} review(s) rejected.")

    def save_model(self, request, obj, form, change):
        if "status" in form.changed_data:
            from django.utils import timezone
            obj.moderated_at = timezone.now()
        super().save_model(request, obj, form, change)
