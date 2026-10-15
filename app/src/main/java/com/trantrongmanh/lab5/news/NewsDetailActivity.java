package com.trantrongmanh.lab5.news;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.trantrongmanh.lab5.R;

public class NewsDetailActivity extends AppCompatActivity {

    public static final String EXTRA_URL   = "extra_url";
    public static final String EXTRA_TITLE = "extra_title";

    private WebView webView;
    private String articleUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail);

        articleUrl = getIntent().getStringExtra(EXTRA_URL);
        String title = getIntent().getStringExtra(EXTRA_TITLE);

        setupToolbar(title);
        setupWebView();
        setupOpenBrowserButton();

        if (articleUrl != null && !articleUrl.isEmpty()) {
            webView.loadUrl(articleUrl);
        }
    }

    private void setupToolbar(String title) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(title != null ? title : getString(R.string.news));
        }
        toolbar.setNavigationOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
    }

    private void setupWebView() {
        webView = findViewById(R.id.webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        // Stay inside the WebView rather than opening the system browser
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
    }

    private void setupOpenBrowserButton() {
        MaterialButton btn = findViewById(R.id.btnOpenBrowser);
        btn.setOnClickListener(v -> {
            if (articleUrl != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(articleUrl));
                startActivity(intent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }
}
