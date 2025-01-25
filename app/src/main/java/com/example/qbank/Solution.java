package com.example.qbank;

public class Solution {
    private String imageUrl;
    private String uploaderEmail;
    private String timestamp;

    public Solution() {
        // Default constructor for Firebase
    }

    public Solution(String imageUrl, String uploaderEmail, String timestamp) {
        this.imageUrl = imageUrl;
        this.uploaderEmail = uploaderEmail;
        this.timestamp = timestamp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getUploaderEmail() {
        return uploaderEmail;
    }

    public void setUploaderEmail(String uploaderEmail) {
        this.uploaderEmail = uploaderEmail;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
