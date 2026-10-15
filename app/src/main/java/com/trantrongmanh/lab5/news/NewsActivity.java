package com.trantrongmanh.lab5.news;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.trantrongmanh.lab5.R;
import com.trantrongmanh.lab5.model.NewsItem;
import com.trantrongmanh.lab5.network.RssFetcher;

import java.util.ArrayList;
import java.util.List;

public class NewsActivity extends AppCompatActivity {

    // Tab labels parallel to RssFetcher.RSS_FEEDS
    private static final String[] FEED_NAMES = {"VnExpress", "Tuổi Trẻ", "Dân Trí", "Tất cả"};

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ShimmerFrameLayout shimmerNews;
    private SwipeRefreshLayout swipeRefreshNews;
    private RecyclerView rvNews;
    private TextView tvNoNews;

    private NewsAdapter adapter;
    private RssFetcher rssFetcher;

    // Full dataset keyed by source index; last tab = aggregated
    private final List<List<NewsItem>> feedData = new ArrayList<>();
    private int selectedTab = 3; // default: "All"

    // For context menu long-press
    private NewsItem contextMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        rssFetcher = new RssFetcher();

        // Initialise per-feed lists
        for (int i = 0; i <= RssFetcher.RSS_FEEDS.length; i++) feedData.add(new ArrayList<>());

        bindViews();
        setupToolbar();
        setupTabs();
        setupRecyclerView();
        setupSwipeRefresh();
        loadAllFeeds();
    }

    // ── View binding ───────────────────────────────────────────────────────────

    private void bindViews() {
        toolbar          = findViewById(R.id.toolbar);
        tabLayout        = findViewById(R.id.tabLayout);
        shimmerNews      = findViewById(R.id.shimmerNews);
        swipeRefreshNews = findViewById(R.id.swipeRefreshNews);
        rvNews           = findViewById(R.id.rvNews);
        tvNoNews         = findViewById(R.id.tvNoNews);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.news);
        }
        toolbar.setNavigationOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
    }

    // ── Tabs ───────────────────────────────────────────────────────────────────

    private void setupTabs() {
        for (String name : FEED_NAMES) {
            tabLayout.addTab(tabLayout.newTab().setText(name));
        }
        // Default = last tab ("Tất cả")
        TabLayout.Tab allTab = tabLayout.getTabAt(FEED_NAMES.length - 1);
        if (allTab != null) allTab.select();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                showTabData();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // ── RecyclerView ───────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        adapter = new NewsAdapter(this);
        rvNews.setLayoutManager(new LinearLayoutManager(this));
        rvNews.setAdapter(adapter);

        // Click → open detail
        adapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(this, NewsDetailActivity.class);
            intent.putExtra(NewsDetailActivity.EXTRA_URL,   item.getLink());
            intent.putExtra(NewsDetailActivity.EXTRA_TITLE, item.getTitle());
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // Long-click → register context menu item and open it
        adapter.setOnItemLongClickListener((view, position, item) -> {
            contextMenuItem = item;
            registerForContextMenu(view);
            openContextMenu(view);
            return true;
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshNews.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent);
        swipeRefreshNews.setOnRefreshListener(this::loadAllFeeds);
    }

    // ── Loading ────────────────────────────────────────────────────────────────

    private void loadAllFeeds() {
        setShimmer(true);

        // Load individual feeds
        for (int i = 0; i < RssFetcher.RSS_FEEDS.length; i++) {
            final int idx = i;
            rssFetcher.fetch(RssFetcher.RSS_FEEDS[i], new RssFetcher.Callback() {
                @Override
                public void onSuccess(List<NewsItem> items) {
                    feedData.get(idx).clear();
                    feedData.get(idx).addAll(items);
                    rebuildAllTab();
                    if (selectedTab == idx) showTabData();
                }
                @Override
                public void onError(Exception e) { /* silent per-feed failure */ }
            });
        }

        // Also aggregate in the "All" tab
        rssFetcher.fetchAll(new RssFetcher.Callback() {
            @Override
            public void onSuccess(List<NewsItem> items) {
                feedData.get(FEED_NAMES.length - 1).clear();
                feedData.get(FEED_NAMES.length - 1).addAll(items);
                setShimmer(false);
                swipeRefreshNews.setRefreshing(false);
                if (selectedTab == FEED_NAMES.length - 1) showTabData();
            }
            @Override
            public void onError(Exception e) {
                setShimmer(false);
                swipeRefreshNews.setRefreshing(false);
                showError("Không thể tải tin tức: " + e.getMessage());
            }
        });
    }

    private void rebuildAllTab() {
        List<NewsItem> all = new ArrayList<>();
        for (int i = 0; i < RssFetcher.RSS_FEEDS.length; i++) {
            all.addAll(feedData.get(i));
        }
        feedData.get(FEED_NAMES.length - 1).clear();
        feedData.get(FEED_NAMES.length - 1).addAll(all);
    }

    private void showTabData() {
        List<NewsItem> data = feedData.get(selectedTab);
        if (data.isEmpty()) {
            tvNoNews.setVisibility(View.VISIBLE);
            swipeRefreshNews.setVisibility(View.GONE);
        } else {
            tvNoNews.setVisibility(View.GONE);
            swipeRefreshNews.setVisibility(View.VISIBLE);
            adapter.setItems(data);
        }
    }

    // ── Context Menu ───────────────────────────────────────────────────────────

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.menu_news_context, menu);
        if (contextMenuItem != null) {
            menu.setHeaderTitle(contextMenuItem.getTitle());
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (contextMenuItem == null) return super.onContextItemSelected(item);

        int id = item.getItemId();
        if (id == R.id.ctx_open_browser) {
            openInBrowser(contextMenuItem.getLink());
            return true;

        } else if (id == R.id.ctx_share_news) {
            shareNews(contextMenuItem);
            return true;

        } else if (id == R.id.ctx_copy_link) {
            copyLink(contextMenuItem.getLink());
            return true;
        }
        return super.onContextItemSelected(item);
    }

    // ── Action helpers ─────────────────────────────────────────────────────────

    private void openInBrowser(String url) {
        if (url == null || url.isEmpty()) return;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(Intent.createChooser(intent, getString(R.string.open_browser)));
    }

    private void shareNews(NewsItem item) {
        String text = item.getTitle() + "\n" + item.getLink();
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(share, getString(R.string.menu_share)));
    }

    private void copyLink(String url) {
        if (url == null) return;
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("link", url));
            Snackbar.make(rvNews, "Đã sao chép liên kết", Snackbar.LENGTH_SHORT).show();
        }
    }

    // ── Shimmer ────────────────────────────────────────────────────────────────

    private void setShimmer(boolean show) {
        if (show) {
            shimmerNews.setVisibility(View.VISIBLE);
            shimmerNews.startShimmer();
            swipeRefreshNews.setVisibility(View.GONE);
            tvNoNews.setVisibility(View.GONE);
        } else {
            shimmerNews.stopShimmer();
            shimmerNews.setVisibility(View.GONE);
        }
    }

    private void showError(String msg) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG)
                .setAction(R.string.refresh, v -> loadAllFeeds())
                .show();
        tvNoNews.setVisibility(View.VISIBLE);
        swipeRefreshNews.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
