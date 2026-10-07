"""
Django settings for the e-commerce backend (orders + M-Pesa STK push).

All secrets come from environment variables / a local `.env` file.
Never commit `.env` — copy `.env.example` and fill it in.
"""
import os
from pathlib import Path

from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent.parent
load_dotenv(BASE_DIR / ".env")


def env(key: str, default: str = "") -> str:
    return os.environ.get(key, default)


def env_bool(key: str, default: bool = False) -> bool:
    return env(key, str(default)).strip().lower() in {"1", "true", "yes", "on"}


def env_int(key: str, default: int) -> int:
    try:
        return int(env(key, str(default)))
    except ValueError:
        return default


SECRET_KEY = env("DJANGO_SECRET_KEY", "dev-only-insecure-key-change-me")
DEBUG = env_bool("DJANGO_DEBUG", True)
ALLOWED_HOSTS = [h.strip() for h in env("DJANGO_ALLOWED_HOSTS", "*").split(",") if h.strip()]
CSRF_TRUSTED_ORIGINS = [o.strip() for o in env("DJANGO_CSRF_TRUSTED_ORIGINS", "").split(",") if o.strip()]

INSTALLED_APPS = [
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.staticfiles",
    "rest_framework",
    "shop",
]

MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
    "django.contrib.messages.middleware.MessageMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
]

ROOT_URLCONF = "config.urls"

TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [],
        "APP_DIRS": True,
        "OPTIONS": {
            "context_processors": [
                "django.template.context_processors.request",
                "django.contrib.auth.context_processors.auth",
                "django.contrib.messages.context_processors.messages",
            ],
        },
    },
]

WSGI_APPLICATION = "config.wsgi.application"

DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.sqlite3",
        "NAME": BASE_DIR / "db.sqlite3",
    }
}

AUTH_PASSWORD_VALIDATORS = [
    {"NAME": "django.contrib.auth.password_validation.UserAttributeSimilarityValidator"},
    {"NAME": "django.contrib.auth.password_validation.MinimumLengthValidator"},
    {"NAME": "django.contrib.auth.password_validation.CommonPasswordValidator"},
    {"NAME": "django.contrib.auth.password_validation.NumericPasswordValidator"},
]

LANGUAGE_CODE = "en-us"
TIME_ZONE = "Africa/Nairobi"
USE_I18N = True
USE_TZ = True

STATIC_URL = "static/"
STATIC_ROOT = BASE_DIR / "staticfiles"
DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

CACHES = {"default": {"BACKEND": "django.core.cache.backends.locmem.LocMemCache"}}

REST_FRAMEWORK = {
    # The app has no login yet; orders are scoped by the X-Client-Id header.
    "DEFAULT_AUTHENTICATION_CLASSES": [],
    "DEFAULT_PERMISSION_CLASSES": ["rest_framework.permissions.AllowAny"],
    "UNAUTHENTICATED_USER": None,
    "DEFAULT_RENDERER_CLASSES": ["rest_framework.renderers.JSONRenderer"],
    "DEFAULT_THROTTLE_CLASSES": ["rest_framework.throttling.AnonRateThrottle"],
    "DEFAULT_THROTTLE_RATES": {"anon": "120/min"},
}

LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "handlers": {"console": {"class": "logging.StreamHandler"}},
    "loggers": {"shop": {"handlers": ["console"], "level": "INFO"}},
}

# ---------------------------------------------------------------------------
# Shop / pricing rules (single source of truth — the app shows these via /quote)
# ---------------------------------------------------------------------------
CATALOG_BASE_URL = env("CATALOG_BASE_URL", "https://dummyjson.com")
# DummyJSON prices are USD; the app and backend convert with the same rate.
USD_TO_KES_RATE = env("USD_TO_KES_RATE", "129")
VERIFY_PRICES_WITH_CATALOG = env_bool("VERIFY_PRICES_WITH_CATALOG", True)
STANDARD_DELIVERY_FEE = env_int("STANDARD_DELIVERY_FEE", 200)
EXPRESS_DELIVERY_FEE = env_int("EXPRESS_DELIVERY_FEE", 450)
FREE_DELIVERY_THRESHOLD = env_int("FREE_DELIVERY_THRESHOLD", 5000)

# ---------------------------------------------------------------------------
# M-Pesa Daraja
# ---------------------------------------------------------------------------
MPESA_ENVIRONMENT = env("MPESA_ENVIRONMENT", "sandbox")  # sandbox | production
MPESA_CONSUMER_KEY = env("MPESA_CONSUMER_KEY")
MPESA_CONSUMER_SECRET = env("MPESA_CONSUMER_SECRET")
MPESA_SHORTCODE = env("MPESA_SHORTCODE", "174379")
MPESA_PASSKEY = env("MPESA_PASSKEY")
# CustomerPayBillOnline (paybill) or CustomerBuyGoodsOnline (till)
MPESA_TRANSACTION_TYPE = env("MPESA_TRANSACTION_TYPE", "CustomerPayBillOnline")
# For tills PartyB is the till number; for paybills it is the shortcode.
MPESA_PARTY_B = env("MPESA_PARTY_B", MPESA_SHORTCODE)
# Public HTTPS base URL Safaricom can reach (e.g. an ngrok URL).
MPESA_CALLBACK_BASE_URL = env("MPESA_CALLBACK_BASE_URL", "https://example.com")
# Random secret embedded in the callback path, since Daraja callbacks are unsigned.
MPESA_CALLBACK_TOKEN = env("MPESA_CALLBACK_TOKEN", "change-me-callback-token")
# Sandbox helper: charge this amount instead of the real total (0 = charge real total).
MPESA_TEST_CHARGE_AMOUNT = env_int("MPESA_TEST_CHARGE_AMOUNT", 0)
MPESA_MAX_AMOUNT = env_int("MPESA_MAX_AMOUNT", 250000)
MPESA_TIMEOUT_SECONDS = env_int("MPESA_TIMEOUT_SECONDS", 30)
