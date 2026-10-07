from django.urls import path

from . import views

urlpatterns = [
    path("health/", views.health, name="health"),
    path("orders/quote/", views.quote, name="order-quote"),
    path("orders/", views.orders, name="orders"),
    path("orders/<uuid:order_id>/", views.order_detail, name="order-detail"),
    path("orders/<uuid:order_id>/pay/", views.pay_order, name="order-pay"),
    path("payments/<str:checkout_request_id>/", views.payment_status, name="payment-status"),
    path("mpesa/callback/<str:token>/", views.mpesa_callback, name="mpesa-callback"),
    path("products/<int:product_id>/reviews/", views.product_reviews, name="product-reviews"),
    path("reviews/", views.my_reviews, name="my-reviews"),
]
