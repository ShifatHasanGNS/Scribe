package com.plasma.scribe;

import java.util.ArrayList;
import java.util.HashMap;

public class DatabaseModel {
    private String name;
    private String email;
    private String bio;
    private String app_rating;
    private ArrayList<String> feedbacks = new ArrayList<>();
    private HashMap<String, String> documents = new HashMap<>();

    public DatabaseModel() {
        name = "Mr. Genius";
        email = "mr.genius@gmail.com";
        bio = "I am a Learner.";
        app_rating = "0";
        feedbacks.add("No feedback yet.");
        documents.put("Title", "No document yet.");
    }

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

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getApp_rating() {
        return app_rating;
    }

    public void setApp_rating(String app_rating) {
        this.app_rating = app_rating;
    }

    public ArrayList<String> getFeedbacks() {
        return feedbacks;
    }

    public void setFeedbacks(ArrayList<String> feedbacks) {
        this.feedbacks = feedbacks;
    }

    public HashMap<String, String> getDocuments() {
        return documents;
    }

    public void setDocuments(HashMap<String, String> documents) {
        this.documents = documents;
    }
}
