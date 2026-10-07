# Soko — Android e-commerce app with M-Pesa checkout

Kotlin · Jetpack Compose · MVVM + Clean Architecture · Hilt · Room · Retrofit · DataStore · Type-safe Navigation
Backend: Django + Django REST Framework (orders, promo codes, M-Pesa Daraja STK push)

```
Splash → (first launch) Onboarding → Home
Home / Categories / Search → Product list → Product details
  → Wishlist ♥ / Add to cart → Cart → Checkout (delivery + promo, server quote)
  → M-Pesa STK push (enter PIN on phone) → Receipt (share as PDF) → My orders
Profile: photo upload, details (pre-fill checkout), orders, settings (theme)
```

## 1. Run the backend (needed for checkout & payment)

```bash
cd backend
python -m venv venv && venv\Scripts\activate      # Windows  (macOS/Linux: source venv/bin/activate)
pip install -r requirements.txt
copy .env.example .env                              # then fill in the M-Pesa values
python manage.py migrate
python manage.py createsuperuser                    # for /admin (orders, payments, promo codes)
python manage.py runserver 0.0.0.0:8000
python manage.py test shop                          # 17 tests, M-Pesa is mocked
```

### M-Pesa (Daraja sandbox)

1. Create an app on https://developer.safaricom.co.ke with **Lipa Na M-Pesa Sandbox** enabled.
2. Put its **Consumer Key** and **Consumer Secret** in `backend/.env`.
   The sandbox shortcode `174379` and passkey are already in `.env.example`.
3. Safaricom sends the payment result to a **public HTTPS** URL. Expose your server with
   `ngrok http 8000` and set `MPESA_CALLBACK_BASE_URL=https://<id>.ngrok-free.app`
   (and a random `MPESA_CALLBACK_TOKEN`). Without it the backend still confirms payments by
   polling Daraja's STK query API, but you won't get the M-Pesa receipt code (e.g. `SIK7RT61SV`).
4. `MPESA_TEST_CHARGE_AMOUNT=1` charges KSh 1 in sandbox instead of the full total. Set `0` to charge real totals.

Credentials live **only** on the server. The app never sees the consumer key, secret or passkey.

## 2. Run the app

Add to `local.properties` (not committed):

```properties
# Emulator → your computer's localhost
backend.baseUrl=http://10.0.2.2:8000/
# Real phone on the same Wi-Fi: http://<your-PC-LAN-IP>:8000/  or your ngrok https URL
```

Open the project in Android Studio, sync Gradle, run. Products come from the free
DummyJSON catalog and are cached in Room, so browsing works offline after the first load.

Promo codes seeded by the backend: `WELCOME10` (10% off, max KSh 1,000) and `SAVE500` (KSh 500 off above KSh 5,000).
Manage them in Django admin.

## Architecture

```
com.bramwel.eccomerceapp
├── MainActivity.kt / MyApplication.kt / MainViewModel.kt / EcommerceApp.kt (app shell + bottom bar)
├── navigation/            AppRoute (type-safe), AppNavHost, TopLevelDestination, AppBottomBar
├── core/
│   ├── common/            AppResult, AppError, Money (USD→KES), MpesaPhone, DateFormatter, Constants
│   ├── network/           safeApiCall, NetworkMonitor, ClientIdInterceptor, qualifiers
│   ├── database/          AppDatabase, Converters
│   ├── datastore/         UserPreferences (profile, theme, onboarding, recent searches)
│   ├── di/                NetworkModule, DatabaseModule, DispatchersModule
│   └── ui/                theme + shared components (ProductCard, AppButton, QuantitySelector…)
└── features/
    ├── splash, onboarding
    ├── home               feed: banners, categories, flash deals + countdown, top rated
    ├── products           catalog data (DummyJSON + Room), list/filter/sort, details, categories
    ├── search             debounced search + recent searches
    ├── wishlist, cart     Room-backed, stock-aware quantities
    ├── checkout           delivery form, server quote, PlaceOrder + PayWithMpesa use cases
    ├── orders             backend orders + offline cache, receipt screen, PDF receipt generator
    ├── profile            photo picker (copied to app storage), edit details
    └── settings           theme, search history, support
```

Every screen is split into a `XxxScreen` (wires the ViewModel) and a stateless `XxxContent`
with `@Preview`s for each state, so you can tweak the UI in Android Studio's preview pane.

### Key rules

- Money is whole shillings (`Long`). The backend re-prices every order from the catalog;
  the total the app shows at checkout is the backend's quote.
- `Constants.USD_TO_KES_RATE` (app) must equal `USD_TO_KES_RATE` (backend `.env`).
- There is no login yet: each install gets an anonymous `X-Client-Id` that scopes its orders.
  When you add authentication, replace `ClientIdInterceptor` with a token interceptor and
  scope orders by user on the backend.
- Replace `Constants.SUPPORT_WHATSAPP_NUMBER` / `SUPPORT_EMAIL` with your real contacts.
