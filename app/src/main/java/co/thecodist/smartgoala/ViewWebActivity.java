package co.thecodist.smartgoala;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ViewWebActivity extends AppCompatActivity {

    public WebView myWebView;
    private String urlToLoad = "https://smartgoala.thecodist.co/";

    private static final int REQUEST_FINE_LOCATION = 1;
    private String geolocationOrigin;
    private GeolocationPermissions.Callback geolocationCallback;

    private View llProgress;
    private TextView tvPercentage;
    private ImageView ivProgressImage;
    private View llNoInternet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_web);

        llNoInternet = findViewById(R.id.llNoInternet);
        llProgress = findViewById(R.id.llProgress);
        tvPercentage = findViewById(R.id.tvPercentage);
        ivProgressImage = findViewById(R.id.ivProgressImage);

        Button btReload = findViewById(R.id.btReload);
        Button btSettings = findViewById(R.id.btSettings);
        btSettings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        btReload.setOnClickListener(v -> reloadWebPage(urlToLoad));

        myWebView = findViewById(R.id.myWebView);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setGeolocationDatabasePath(getFilesDir().getPath());
        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                urlToLoad = url;
                if (Utils.isConnected(ViewWebActivity.this)) {
                    return false;
                } else {
                    setNoInternetDialogVisibility(true);
                    return true;
                }
            }
        });
        myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                // Geolocation permissions coming from this app's Manifest will only be valid for devices with
                // API_VERSION < 23. On API 23 and above, we must check for permissions, and possibly
                // ask for them.
                String perm = Manifest.permission.ACCESS_FINE_LOCATION;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                        ContextCompat.checkSelfPermission(ViewWebActivity.this, perm) == PackageManager.PERMISSION_GRANTED) {
                    // we're on SDK < 23 OR user has already granted permission
                    callback.invoke(origin, true, false);
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(ViewWebActivity.this, perm)) {
                        // ask the user for permission
                        ActivityCompat.requestPermissions(ViewWebActivity.this, new String[]{perm}, REQUEST_FINE_LOCATION);

                        // we will use these when user responds
                        geolocationOrigin = origin;
                        geolocationCallback = callback;
                    }
                }
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (newProgress < 100){
                    if (llProgress.getVisibility() == ProgressBar.GONE) {
                        llProgress.setVisibility(ProgressBar.VISIBLE);

                        Animation progressAnimation = AnimationUtils.loadAnimation(ViewWebActivity.this, R.anim.progress_anim);
                        ivProgressImage.startAnimation(progressAnimation);
                    }
                    tvPercentage.setText(String.format(getString(R.string.label_loading), newProgress));
                }
                if (newProgress == 100) {
                    llProgress.setVisibility(ProgressBar.GONE);
                    ivProgressImage.setAnimation(null);
                }
            }
        });

        reloadWebPage(urlToLoad);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_FINE_LOCATION:
                boolean allow = false;
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // user has allowed this permission
                    allow = true;
                }
                if (geolocationCallback != null) {
                    // call back to web chrome client
                    geolocationCallback.invoke(geolocationOrigin, allow, false);
                }
                break;
        }
    }

    private void reloadWebPage(String url) {
        if (Utils.isConnected(this)) {
            setNoInternetDialogVisibility(false);
            myWebView.loadUrl(url);
        } else {
            setNoInternetDialogVisibility(true);
        }
    }

    private void setNoInternetDialogVisibility(boolean isVisible) {
        llNoInternet.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        myWebView.setVisibility(isVisible ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        if (myWebView.canGoBack()) {
            myWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

}
