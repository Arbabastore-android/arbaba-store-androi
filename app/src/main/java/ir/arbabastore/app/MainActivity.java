package ir.arbabastore.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.MimeTypeMap;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String BASE_URL = "https://www.arbabastore.ir/";
    private static final String CATEGORY_URL = BASE_URL + "shop/";
    private static final String SEARCH_URL = BASE_URL + "?s=&post_type=product";
    private static final String CART_URL = BASE_URL + "cart/";
    private static final String ACCOUNT_URL = BASE_URL + "my-account/";
    private static final int FILE_CHOOSER_REQUEST = 1201;
    private static final int WEB_PERMISSION_REQUEST = 1202;
    private static final int GEO_PERMISSION_REQUEST = 1203;
    private static final int DOWNLOAD_PERMISSION_REQUEST = 1204;

    private FrameLayout root;
    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout offlineView;
    private LinearLayout bottomNavigation;
    private View splashView;
    private ValueCallback<Uri[]> filePathCallback;
    private PermissionRequest pendingWebPermission;
    private GeolocationPermissions.Callback pendingGeoCallback;
    private String pendingGeoOrigin;
    private PendingDownload pendingDownload;
    private float touchStartY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getColor(R.color.navy));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(0);
        }

        buildInterface();
        configureWebView();

        if (savedInstanceState == null) {
            loadUrl(getIntentUrlOrHome(getIntent()));
        } else {
            webView.restoreState(savedInstanceState);
        }
        updateNetworkState();
    }

    private void buildInterface() {
        root = new FrameLayout(this);
        root.setBackgroundColor(getColor(R.color.surface));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        root.addView(page, matchParent());

        page.addView(buildToolbar(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(getColorStateList(R.color.gold));
        progressBar.setProgressBackgroundTintList(getColorStateList(R.color.surface_soft));
        page.addView(progressBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));

        FrameLayout content = new FrameLayout(this);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        page.addView(content, contentParams);

        webView = new WebView(this);
        webView.setBackgroundColor(getColor(R.color.surface));
        content.addView(webView, matchParent());

        offlineView = buildOfflineView();
        content.addView(offlineView, matchParent());

        bottomNavigation = buildBottomNavigation();
        page.addView(bottomNavigation, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));

        splashView = buildSplashView();
        root.addView(splashView, matchParent());
        setContentView(root);
    }

    private View buildToolbar() {
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(12), 0, dp(8), 0);
        toolbar.setBackgroundColor(getColor(R.color.navy));

        TextView title = new TextView(this);
        title.setText("ARBABA  STORE");
        title.setTextColor(getColor(R.color.gold));
        title.setTextSize(17);
        title.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        title.setTypeface(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1f));

        ImageButton refresh = toolbarButton(R.drawable.ic_refresh, getString(R.string.refresh));
        refresh.setOnClickListener(v -> webView.reload());
        toolbar.addView(refresh, new LinearLayout.LayoutParams(dp(48), dp(48)));

        ImageButton share = toolbarButton(R.drawable.ic_share, getString(R.string.share));
        share.setOnClickListener(v -> shareCurrentPage());
        toolbar.addView(share, new LinearLayout.LayoutParams(dp(48), dp(48)));
        return toolbar;
    }

    private ImageButton toolbarButton(int drawable, String description) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(drawable);
        button.setContentDescription(description);
        button.setBackground(new ColorDrawable(Color.TRANSPARENT));
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        return button;
    }

    private LinearLayout buildBottomNavigation() {
        LinearLayout navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        navigation.setGravity(Gravity.CENTER);
        navigation.setBackgroundColor(getColor(R.color.surface));
        navigation.setElevation(dp(8));
        navigation.addView(navItem(R.drawable.ic_home, R.string.home, BASE_URL), weighted());
        navigation.addView(navItem(R.drawable.ic_categories, R.string.categories, CATEGORY_URL), weighted());
        navigation.addView(navItem(R.drawable.ic_search, R.string.search, SEARCH_URL), weighted());
        navigation.addView(navItem(R.drawable.ic_cart, R.string.cart, CART_URL), weighted());
        navigation.addView(navItem(R.drawable.ic_account, R.string.account, ACCOUNT_URL), weighted());
        return navigation;
    }

    private LinearLayout navItem(int iconRes, int labelRes, String destination) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(2), dp(6), dp(2), dp(4));
        item.setTag(destination);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        item.addView(icon, new LinearLayout.LayoutParams(dp(26), dp(26)));

        TextView label = new TextView(this);
        label.setText(labelRes);
        label.setTextColor(getColor(R.color.text_secondary));
        label.setTextSize(10);
        label.setGravity(Gravity.CENTER);
        item.addView(label, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(22)));

        item.setOnClickListener(v -> loadUrl(destination));
        return item;
    }

    private LinearLayout buildOfflineView() {
        LinearLayout holder = new LinearLayout(this);
        holder.setOrientation(LinearLayout.VERTICAL);
        holder.setGravity(Gravity.CENTER);
        holder.setPadding(dp(28), dp(28), dp(28), dp(28));
        holder.setBackgroundColor(getColor(R.color.surface_soft));
        holder.setVisibility(View.GONE);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(24), dp(28), dp(24), dp(28));
        panel.setBackgroundResource(R.drawable.offline_panel);
        holder.addView(panel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView symbol = new TextView(this);
        symbol.setText("!");
        symbol.setGravity(Gravity.CENTER);
        symbol.setTextColor(getColor(R.color.gold));
        symbol.setTextSize(34);
        symbol.setTypeface(null, android.graphics.Typeface.BOLD);
        panel.addView(symbol, new LinearLayout.LayoutParams(dp(52), dp(52)));

        TextView title = new TextView(this);
        title.setText(R.string.offline_title);
        title.setTextColor(getColor(R.color.text_primary));
        title.setTextSize(19);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        panel.addView(title, margins(-1, -2, 0, 12, 0, 0));

        TextView message = new TextView(this);
        message.setText(R.string.offline_message);
        message.setTextColor(getColor(R.color.text_secondary));
        message.setTextSize(14);
        message.setGravity(Gravity.CENTER);
        panel.addView(message, margins(-1, -2, 0, 0, 0, 18));

        Button retry = new Button(this);
        retry.setText(R.string.retry);
        retry.setTextColor(getColor(R.color.navy));
        retry.setTextSize(15);
        retry.setAllCaps(false);
        retry.setBackgroundResource(R.drawable.button_gold);
        retry.setOnClickListener(v -> {
            updateNetworkState();
            if (isOnline()) webView.reload();
        });
        panel.addView(retry, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return holder;
    }

    private View buildSplashView() {
        LinearLayout splash = new LinearLayout(this);
        splash.setOrientation(LinearLayout.VERTICAL);
        splash.setGravity(Gravity.CENTER);
        splash.setBackgroundColor(getColor(R.color.surface));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.arbaba_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        splash.addView(logo, new LinearLayout.LayoutParams(dp(250), dp(310)));

        ProgressBar spinner = new ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
        spinner.setIndeterminateTintList(getColorStateList(R.color.gold));
        splash.addView(spinner, new LinearLayout.LayoutParams(dp(32), dp(32)));
        splash.setElevation(dp(30));
        return splash;
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new StoreWebViewClient());
        webView.setWebChromeClient(new StoreWebChromeClient());
        webView.setDownloadListener(buildDownloadListener());
        webView.setOnLongClickListener(v -> false);
        WebView.setWebContentsDebuggingEnabled(false);

        webView.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN && webView.getScrollY() == 0) {
                touchStartY = event.getY();
            } else if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                touchStartY = Float.MAX_VALUE;
            }
            if (event.getAction() == android.view.MotionEvent.ACTION_UP
                    && webView.getScrollY() == 0
                    && event.getY() - touchStartY > dp(160)) {
                webView.reload();
                Toast.makeText(this, getString(R.string.refresh), Toast.LENGTH_SHORT).show();
            }
            return false;
        });
    }

    private class StoreWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return handleUri(request.getUrl());
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return handleUri(Uri.parse(url));
        }

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(10);
            offlineView.setVisibility(View.GONE);
            updateSelectedNavigation(url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            CookieManager.getInstance().flush();
            progressBar.setVisibility(View.GONE);
            hideSplash();
            updateSelectedNavigation(url);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) {
                progressBar.setVisibility(View.GONE);
                if (!isOnline()) offlineView.setVisibility(View.VISIBLE);
                hideSplash();
            }
        }
    }

    private class StoreWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);
            progressBar.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback,
                                         FileChooserParams fileChooserParams) {
            if (filePathCallback != null) filePathCallback.onReceiveValue(null);
            filePathCallback = callback;
            Intent chooser = fileChooserParams.createIntent();
            chooser.addCategory(Intent.CATEGORY_OPENABLE);
            chooser.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            try {
                startActivityForResult(chooser, FILE_CHOOSER_REQUEST);
                return true;
            } catch (ActivityNotFoundException exception) {
                filePathCallback = null;
                Toast.makeText(MainActivity.this, "برنامه‌ای برای انتخاب فایل پیدا نشد.", Toast.LENGTH_LONG).show();
                return false;
            }
        }

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            List<String> permissions = new ArrayList<>();
            for (String resource : request.getResources()) {
                if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                    permissions.add(Manifest.permission.CAMERA);
                } else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                    permissions.add(Manifest.permission.RECORD_AUDIO);
                }
            }
            if (permissions.isEmpty() || hasPermissions(permissions)) {
                request.grant(request.getResources());
            } else {
                pendingWebPermission = request;
                requestPermissions(permissions.toArray(new String[0]), WEB_PERMISSION_REQUEST);
            }
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin,
                                                        GeolocationPermissions.Callback callback) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                callback.invoke(origin, true, false);
            } else {
                pendingGeoOrigin = origin;
                pendingGeoCallback = callback;
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, GEO_PERMISSION_REQUEST);
            }
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture,
                                      android.os.Message resultMsg) {
            WebView popup = new WebView(MainActivity.this);
            popup.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView popupView, WebResourceRequest request) {
                    loadUrl(request.getUrl().toString());
                    return true;
                }
            });
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(popup);
            resultMsg.sendToTarget();
            return true;
        }
    }

    private boolean handleUri(Uri uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if ("http".equals(scheme) || "https".equals(scheme)) return false;

        try {
            Intent intent;
            if ("intent".equals(scheme)) {
                intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    String fallback = intent.getStringExtra("browser_fallback_url");
                    if (fallback != null) webView.loadUrl(fallback);
                }
            } else {
                intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        } catch (ActivityNotFoundException | URISyntaxException exception) {
            Toast.makeText(this, "برنامه مناسب برای باز کردن این لینک نصب نیست.", Toast.LENGTH_LONG).show();
        }
        return true;
    }

    private DownloadListener buildDownloadListener() {
        return (url, userAgent, contentDisposition, mimeType, contentLength) -> {
            PendingDownload download = new PendingDownload(url, userAgent, contentDisposition, mimeType);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                    && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                pendingDownload = download;
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        DOWNLOAD_PERMISSION_REQUEST);
            } else {
                enqueueDownload(download);
            }
        };
    }

    private void enqueueDownload(PendingDownload download) {
        try {
            String filename = URLUtil.guessFileName(
                    download.url, download.contentDisposition, download.mimeType);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(download.url));
            String resolvedMime = download.mimeType == null ? MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(download.url))
                    : download.mimeType;
            if (resolvedMime != null) request.setMimeType(resolvedMime);
            request.addRequestHeader("User-Agent", download.userAgent);
            String cookie = CookieManager.getInstance().getCookie(download.url);
            if (cookie != null) request.addRequestHeader("Cookie", cookie);
            request.setTitle(filename);
            request.setDescription("Arbaba Store");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            manager.enqueue(request);
            Toast.makeText(this, "دانلود آغاز شد.", Toast.LENGTH_SHORT).show();
        } catch (Exception exception) {
            Toast.makeText(this, "دانلود فایل انجام نشد.", Toast.LENGTH_LONG).show();
        }
    }

    private void shareCurrentPage() {
        String currentUrl = webView.getUrl() == null ? BASE_URL : webView.getUrl();
        String title = webView.getTitle() == null ? getString(R.string.app_name) : webView.getTitle();
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        shareIntent.putExtra(Intent.EXTRA_TEXT, title + "\n" + currentUrl);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
    }

    private void loadUrl(String url) {
        if (!isOnline()) {
            offlineView.setVisibility(View.VISIBLE);
            hideSplash();
            return;
        }
        offlineView.setVisibility(View.GONE);
        webView.loadUrl(url);
    }

    private void updateSelectedNavigation(String url) {
        if (bottomNavigation == null || url == null) return;
        for (int i = 0; i < bottomNavigation.getChildCount(); i++) {
            LinearLayout item = (LinearLayout) bottomNavigation.getChildAt(i);
            TextView label = (TextView) item.getChildAt(1);
            String destination = String.valueOf(item.getTag());
            boolean selected = (BASE_URL.equals(destination) && isHomeUrl(url))
                    || (!BASE_URL.equals(destination) && url.startsWith(destination));
            label.setTextColor(getColor(selected ? R.color.gold_dark : R.color.text_secondary));
            item.setBackgroundColor(getColor(selected ? R.color.surface_soft : R.color.surface));
        }
    }

    private boolean isHomeUrl(String url) {
        String normalized = url.replace("https://arbabastore.ir", "https://www.arbabastore.ir");
        return BASE_URL.equals(normalized) || (BASE_URL + "index.php").equals(normalized);
    }

    private void updateNetworkState() {
        boolean online = isOnline();
        offlineView.setVisibility(online ? View.GONE : View.VISIBLE);
        if (!online) hideSplash();
    }

    private boolean isOnline() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = manager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void hideSplash() {
        if (splashView == null || splashView.getVisibility() != View.VISIBLE) return;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            splashView.animate().alpha(0f).setDuration(220).withEndAction(() -> {
                splashView.setVisibility(View.GONE);
                splashView.setAlpha(1f);
            }).start();
        }, 300);
    }

    private String getIntentUrlOrHome(Intent intent) {
        Uri data = intent == null ? null : intent.getData();
        return data == null ? BASE_URL : data.toString();
    }

    private boolean hasPermissions(List<String> permissions) {
        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Uri data = intent.getData();
        if (data != null) loadUrl(data.toString());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST || filePathCallback == null) return;
        Uri[] results = null;
        if (resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                results = new Uri[count];
                for (int i = 0; i < count; i++) results[i] = data.getClipData().getItemAt(i).getUri();
            } else if (data.getData() != null) {
                results = new Uri[]{data.getData()};
            }
        }
        filePathCallback.onReceiveValue(results);
        filePathCallback = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = true;
        for (int result : grantResults) granted &= result == PackageManager.PERMISSION_GRANTED;
        if (requestCode == WEB_PERMISSION_REQUEST && pendingWebPermission != null) {
            if (granted) pendingWebPermission.grant(pendingWebPermission.getResources());
            else pendingWebPermission.deny();
            pendingWebPermission = null;
        } else if (requestCode == GEO_PERMISSION_REQUEST && pendingGeoCallback != null) {
            pendingGeoCallback.invoke(pendingGeoOrigin, granted, false);
            pendingGeoCallback = null;
            pendingGeoOrigin = null;
        } else if (requestCode == DOWNLOAD_PERMISSION_REQUEST && pendingDownload != null) {
            if (granted) enqueueDownload(pendingDownload);
            else Toast.makeText(this, "برای ذخیره فایل، مجوز حافظه لازم است.", Toast.LENGTH_LONG).show();
            pendingDownload = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (filePathCallback != null) filePathCallback.onReceiveValue(null);
        webView.stopLoading();
        webView.setWebChromeClient(null);
        webView.setWebViewClient(null);
        webView.clearHistory();
        ((ViewGroup) webView.getParent()).removeView(webView);
        webView.destroy();
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private FrameLayout.LayoutParams matchParent() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
    }

    private LinearLayout.LayoutParams margins(int width, int height, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private static class PendingDownload {
        final String url;
        final String userAgent;
        final String contentDisposition;
        final String mimeType;

        PendingDownload(String url, String userAgent, String contentDisposition, String mimeType) {
            this.url = url;
            this.userAgent = userAgent;
            this.contentDisposition = contentDisposition;
            this.mimeType = mimeType;
        }
    }
}
