package com.example.smarthome.model.response;

import com.google.gson.annotations.SerializedName;

public class Esp32Device {
    @SerializedName("_id")
    private String id;
    private String name;
    private String status;
    private String mqttBaseTopic;
    private String home;

    // Bổ sung các trường mới từ gói tin chi tiết
    private String claimedAt;
    private String createdAt;
    private String updatedAt;

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public String getMqttBaseTopic() { return mqttBaseTopic; }
    public String getHome() { return home; }
    public String getClaimedAt() { return claimedAt; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}