from django.contrib import admin
from django.urls import include, path

admin.site.site_header = "Shop Admin"
admin.site.site_title = "Shop Admin"

urlpatterns = [
    path("admin/", admin.site.urls),
    path("api/", include("shop.urls")),
]
