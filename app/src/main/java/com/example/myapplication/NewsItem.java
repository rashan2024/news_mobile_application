package com.example.myapplication;

public class NewsItem {
    private String title;
    private String description;
    private String timestamp;
    private int imageResource;
    private String category; // NEW: Field to store the news category

    public NewsItem(String title, String description, String timestamp, int imageResource, String category) {
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
        this.imageResource = imageResource;
        this.category = category; // NEW: Assign the category
    }

    // --- Getters for all fields ---
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getImageResource() {
        return imageResource;
    }

    // NEW: Getter for the category
    public String getCategory() {
        return category;
    }
}
