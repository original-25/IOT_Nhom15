package com.example.smarthome.model.request;

public class HomeRequest {
    private String name;

    public HomeRequest(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}