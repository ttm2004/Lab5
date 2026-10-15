package com.trantrongmanh.lab5.network;

import android.util.Log;
import android.util.Xml;

import com.trantrongmanh.lab5.model.NewsItem;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight RSS 2.0 parser using Android's built-in XmlPullParser.
 * Runs on a background thread (AsyncTask / Thread / ExecutorService).
 */
public class RssParser {

    private static final String TAG = "RssParser";

    // Matches the first <img src="..."> inside a string
    private static final Pattern IMG_PATTERN =
            Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    /**
     * Parse an RSS feed InputStream and return a list of NewsItems.
     */
    public List<NewsItem> parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readFeed(parser);
        } finally {
            in.close();
        }
    }

    private List<NewsItem> readFeed(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        List<NewsItem> items = new ArrayList<>();
        parser.require(XmlPullParser.START_TAG, null, "rss");

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            if ("channel".equals(parser.getName())) {
                items.addAll(readChannel(parser));
            }
        }
        return items;
    }

    private List<NewsItem> readChannel(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        List<NewsItem> items = new ArrayList<>();

        while (!(parser.next() == XmlPullParser.END_TAG && "channel".equals(parser.getName()))) {
            if (parser.getEventType() == XmlPullParser.START_TAG && "item".equals(parser.getName())) {
                NewsItem item = readItem(parser);
                if (item != null) items.add(item);
            }
        }
        return items;
    }

    private NewsItem readItem(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        NewsItem item = new NewsItem();

        while (!(parser.next() == XmlPullParser.END_TAG && "item".equals(parser.getName()))) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;

            String name = parser.getName();
            switch (name) {
                case "title":
                    item.setTitle(readText(parser));
                    break;
                case "description":
                    String desc = readText(parser);
                    item.setDescription(desc);
                    // Try to extract first image from HTML description
                    if (item.getImageUrl() == null) {
                        item.setImageUrl(extractImageUrl(desc));
                    }
                    break;
                case "link":
                    item.setLink(readText(parser));
                    break;
                case "pubDate":
                    item.setPubDate(readText(parser));
                    break;
                case "enclosure":
                    // <enclosure url="..." type="image/jpeg" />
                    String url = parser.getAttributeValue(null, "url");
                    String type = parser.getAttributeValue(null, "type");
                    if (url != null && type != null && type.startsWith("image")) {
                        item.setImageUrl(url);
                    }
                    skip(parser);
                    break;
                case "media:content":
                    String mUrl = parser.getAttributeValue(null, "url");
                    if (mUrl != null) item.setImageUrl(mUrl);
                    skip(parser);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        return (item.getTitle() != null) ? item : null;
    }

    private String readText(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result.trim();
    }

    private void skip(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:   depth--; break;
                case XmlPullParser.START_TAG: depth++; break;
            }
        }
    }

    private String extractImageUrl(String html) {
        if (html == null) return null;
        Matcher m = IMG_PATTERN.matcher(html);
        return m.find() ? m.group(1) : null;
    }
}
