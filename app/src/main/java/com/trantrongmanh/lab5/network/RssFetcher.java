package com.trantrongmanh.lab5.network;

import android.os.Handler;
import android.os.Looper;

import com.trantrongmanh.lab5.model.NewsItem;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fetches and parses an RSS feed on a background thread,
 * then delivers the result on the main thread via a callback.
 */
public class RssFetcher {

    public interface Callback {
        void onSuccess(List<NewsItem> items);
        void onError(Exception e);
    }

    // Popular Vietnamese weather / news RSS feeds
    public static final String[] RSS_FEEDS = {
            "https://vnexpress.net/rss/thoi-su.rss",
            "https://tuoitre.vn/rss/thoi-su.rss",
            "https://dantri.com.vn/rss/xa-hoi.rss"
    };

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final RssParser parser = new RssParser();

    public void fetch(String feedUrl, Callback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(feedUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "WeatherApp/1.0");
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);
                conn.connect();

                InputStream is = conn.getInputStream();
                List<NewsItem> items = parser.parse(is);

                // Tag source domain
                String source = url.getHost().replace("www.", "");
                for (NewsItem item : items) item.setSource(source);

                conn.disconnect();
                mainHandler.post(() -> callback.onSuccess(items));

            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Fetch all feeds and aggregate results (order not guaranteed).
     */
    public void fetchAll(Callback callback) {
        executor.execute(() -> {
            List<NewsItem> all = new ArrayList<>();
            Exception lastError = null;

            for (String feedUrl : RSS_FEEDS) {
                try {
                    URL url = new URL(feedUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("User-Agent", "WeatherApp/1.0");
                    conn.setConnectTimeout(10_000);
                    conn.setReadTimeout(10_000);
                    conn.connect();

                    InputStream is = conn.getInputStream();
                    List<NewsItem> items = parser.parse(is);
                    String source = url.getHost().replace("www.", "");
                    for (NewsItem item : items) item.setSource(source);
                    all.addAll(items);
                    conn.disconnect();
                } catch (Exception e) {
                    lastError = e;
                }
            }

            final List<NewsItem> result = all;
            final Exception error = lastError;
            mainHandler.post(() -> {
                if (!result.isEmpty()) callback.onSuccess(result);
                else if (error != null) callback.onError(error);
                else callback.onSuccess(result);
            });
        });
    }
}
