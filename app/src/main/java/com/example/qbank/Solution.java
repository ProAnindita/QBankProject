package com.example.qbank;

public class Solution {
    private String imageUrl;
    private String uploaderEmail;
    private String timestamp;
    private String courseId;
    private String courseName;
    private String courseSemester;
    private double averageRating; // Add averageRating property

    private String solutionId; // Add solutionId if not already present

    // Default constructor for Firebase
    public Solution() {}

    // Constructor to initialize all fields
    public Solution(String imageUrl, String uploaderEmail, String timestamp, String courseId, String courseName, String courseSemester, double averageRating, String solutionId) {
        this.imageUrl = imageUrl;
        this.uploaderEmail = uploaderEmail;
        this.timestamp = timestamp;
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseSemester = courseSemester;
        this.averageRating = averageRating;
        this.solutionId = solutionId;
    }

    // Getters and Setters
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

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseSemester() {
        return courseSemester;
    }

    public void setCourseSemester(String courseSemester) {
        this.courseSemester = courseSemester;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public String getSolutionId() {
        return solutionId;
    }

    public void setSolutionId(String solutionId) {
        this.solutionId = solutionId;
    }
}
