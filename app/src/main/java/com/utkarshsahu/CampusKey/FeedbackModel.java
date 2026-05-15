package com.utkarshsahu.CampusKey;

import android.text.format.DateUtils;

public class FeedbackModel {
    private String username;
    private int rating;
    private String comment;
    private long timestamp;

    public FeedbackModel() {
        // Required for Firebase
    }

    public FeedbackModel(String username, int rating, String comment, long timestamp) {
        this.username = username;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    public String getUsername() { return username; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public long getTimestamp() { return timestamp; }

    public String getRelativeDate() {
        return (String) DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
    }
}
