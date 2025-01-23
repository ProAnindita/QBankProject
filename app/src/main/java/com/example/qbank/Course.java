package com.example.qbank;

public class Course {
    private String courseName;
    private String courseCode;
    private String semester;

    // Default constructor for Firebase
    public Course() {}

    // Constructor with parameters
    public Course(String courseName, String courseCode, String semester) {
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.semester = semester;
    }

    // Getters and setters
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
}
