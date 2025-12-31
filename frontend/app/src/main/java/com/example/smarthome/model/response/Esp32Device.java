package com.example.smarthome.model.response;

import com.google.gson.annotations.SerializedName;

public class Esp32Device {
    @SerializedName("_id") // Backend trả về _id, Android nên map thành id
    private String id;

    private String name;
    private String status; // "unclaimed" hoặc "provisioned"
    private String mqttUsername;
    private String mqttBaseTopic;
    private String home; // ID của ngôi nhà

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public String getMqttUsername() { return mqttUsername; }
    public String getMqttBaseTopic() { return mqttBaseTopic; }
}
