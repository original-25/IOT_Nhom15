package com.example.smarthome.model.request;

import java.util.HashMap;
import java.util.Map;

public class CreateDeviceRequest {
    private String name;
    private String type;
    private String espId;
    private Map<String, Object> config;   // Đổi thành Map để dễ truyền dữ liệu PIN
    private Map<String, Object> settings;

    public CreateDeviceRequest(String name, String type, String espId) {
        this.name = name;
        this.type = type;
        this.espId = espId;
        this.config = new HashMap<>();
        this.settings = new HashMap<>();
    }

    // Setter để truyền cấu hình PIN từ Fragment/ViewModel
    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }
}