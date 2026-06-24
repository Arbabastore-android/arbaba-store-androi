# ساخت فایل نصب Arbaba Store

1. پوشه پروژه را در Android Studio باز کنید.
2. اجازه دهید Gradle و Android SDK 35 دانلود و همگام‌سازی شوند.
3. از منوی `Build` گزینه `Build APK(s)` را انتخاب کنید.
4. فایل نصب در مسیر زیر ساخته می‌شود:

```text
app/build/outputs/apk/debug/app-debug.apk
```

برای انتشار در بازار، مایکت یا Google Play از مسیر `Build > Generate Signed App Bundle / APK` یک کلید امضای دائمی بسازید و خروجی `AAB` یا `APK` امضاشده تولید کنید.

نام برنامه، آدرس سایت و مسیرهای نوار پایین در ابتدای فایل زیر قابل تنظیم‌اند:

```text
app/src/main/java/ir/arbabastore/app/MainActivity.java
```
