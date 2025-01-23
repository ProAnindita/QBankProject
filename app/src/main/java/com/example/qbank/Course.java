package com.example.qbank;
public class Course {
    private String courseName;
    private String courseCode;
    private String semester;
    private String imageKey;

    public Course() {}

    public Course(String courseName, String courseCode, String semester, String imageKey) {
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.semester = semester;
        this.imageKey = imageKey;
    }

    public Course(String newCourseName, String newCourseId, String newSemester) {
    }

    // Getters and Setters
    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getImageKey() {
        return imageKey;
    }

    public void setImageKey(String imageKey) {
        this.imageKey = imageKey;
    }
}
