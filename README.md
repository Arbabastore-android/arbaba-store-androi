# Arbaba Store Android

Android storefront for `https://www.arbabastore.ir`, built as a secure WebView shell that keeps the website as the single source of truth for products, customers, cart, checkout, payment, and orders.

## Included

- Branded splash screen and launcher identity
- Persistent WooCommerce login and cart cookies
- Native home, shop, search, cart, and account navigation
- In-app checkout and payment redirects
- File upload and authenticated downloads
- External handling for phone, email, SMS, maps, and messenger links
- Deep links for `arbabastore.ir`
- Offline state, retry, progress indicator, refresh gesture, and page sharing
- Camera, microphone, and geolocation permission bridging when requested by the website

## Build

```bash
./gradlew assembleDebug
```

The debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`.
Android Studio can also open the project directly and install it on a connected Android phone.

## Website routes

Navigation routes are constants at the top of `MainActivity.java`. The current defaults follow WooCommerce conventions:

- `/` home
- `/shop/` categories/products
- `/?s=&post_type=product` search
- `/cart/` cart
- `/my-account/` account
