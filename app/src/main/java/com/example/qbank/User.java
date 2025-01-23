package com.example.qbank;

public class User {
    private String name;
    private String email;
    private String batch;
    private String section;

    // Default constructor required for calls to DataSnapshot.getValue(User.class)
    public User() {
    }

    // Parameterized constructor
    public User(String name, String email, String batch, String section) {
        this.name = name;
        this.email = email;
        this.batch = batch;
        this.section = section;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBatch() {
        return batch;
    }

    public void setBatch(String batch) {
        this.batch = batch;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }
}
