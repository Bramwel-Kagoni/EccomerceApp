from django.db import migrations

PROMOS = [
    {"code": "WELCOME10", "kind": "percent", "value": 10, "max_discount": 1000, "min_subtotal": 0,
     "description": "10% off your order (up to KSh 1,000)"},
    {"code": "SAVE500", "kind": "flat", "value": 500, "max_discount": None, "min_subtotal": 5000,
     "description": "KSh 500 off orders above KSh 5,000"},
]


def seed(apps, schema_editor):
    PromoCode = apps.get_model("shop", "PromoCode")
    for promo in PROMOS:
        PromoCode.objects.update_or_create(code=promo["code"], defaults=promo)


def unseed(apps, schema_editor):
    apps.get_model("shop", "PromoCode").objects.filter(code__in=[p["code"] for p in PROMOS]).delete()


class Migration(migrations.Migration):
    dependencies = [("shop", "0001_initial")]
    operations = [migrations.RunPython(seed, unseed)]
