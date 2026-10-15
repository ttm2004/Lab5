package com.trantrongmanh.lab5.model;

/**
 * Represents a single RSS feed item.
 */
public class NewsItem {

    private String title;
    private String description;
    private String link;
    private String pubDate;
    private String imageUrl;
    private String source;

    public NewsItem() {}

    public NewsItem(String title, String description, String link, String pubDate) {
        this.title = title;
        this.description = description;
        this.link = link;
        this.pubDate = pubDate;
    }

    // Getters & Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public String getPubDate() { return pubDate; }
    public void setPubDate(String pubDate) { this.pubDate = pubDate; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    /**
     * Returns a cleaned plain-text snippet from the HTML description.
     */
    public String getSnippet() {
        if (description == null) return "";
        String plain = description.replaceAll("<[^>]+>", "").trim();
        return plain.length() > 120 ? plain.substring(0, 120) + "…" : plain;
    }
}
