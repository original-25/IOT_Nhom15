package com.example.smarthome.model.response;

import com.google.gson.annotations.SerializedName;

public class Device {
    @SerializedName("_id")
    private String id;
    private String name;
    private String type; // Ví dụ: "light", "fan", "switch"

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
}