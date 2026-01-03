package com.example.smarthome.model.response;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class Device {
    @SerializedName("_id")
    private String id;

    private String name;
    private String type;

    // Trạng thái kết nối (online, provisioning, error)
    private String status;

    // Trạng thái điều khiển thực tế (ví dụ: { "status": "on" })
    private Map<String, Object> lastState;

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public Map<String, Object> getLastState() { return lastState; }

    // Setters (nếu cần)
    public void setStatus(String status) { this.status = status; }
    public void setLastState(Map<String, Object> lastState) { this.lastState = lastState; }
}