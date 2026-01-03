package com.example.smarthome.model.response;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class Device {
    @SerializedName("_id")
    private String id;

    private String name;
    private String type;
    private String status;

    // Đổi từ Map<String, Object> sang Object để nhận được cả số và đối tượng
    private Object lastState;
    private Object config;

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public Object getLastState() { return lastState; } // Cập nhật kiểu trả về

    // Setters
    public void setStatus(String status) { this.status = status; }
    public void setLastState(Object lastState) { this.lastState = lastState; } // Cập nhật tham số
    public Object getConfig() { return config; }
    public void setConfig(Object config) { this.config = config; }
}