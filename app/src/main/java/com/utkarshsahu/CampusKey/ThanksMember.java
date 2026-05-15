package com.utkarshsahu.CampusKey;

public class ThanksMember {
    private String name;
    private String imageUrl;
    private String socialUrl;
    private String tagText;
    private long count;

    public ThanksMember(String name, String imageUrl, String socialUrl, String tagText, long count) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.socialUrl = socialUrl;
        this.tagText = tagText;
        this.count = count;
    }

    // Getters
    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public String getSocialUrl() { return socialUrl; }
    public String getTagText() { return tagText; }
    public long getCount() { return count; }
}