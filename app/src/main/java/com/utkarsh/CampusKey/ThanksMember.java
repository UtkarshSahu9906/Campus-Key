package com.utkarsh.CampusKey;

public class ThanksMember {
    private String name;
    private String imageUrl;
    private String socialUrl;

    public ThanksMember(String name, String imageUrl, String socialUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.socialUrl = socialUrl;
    }

    // Getters
    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public String getSocialUrl() { return socialUrl; }
}