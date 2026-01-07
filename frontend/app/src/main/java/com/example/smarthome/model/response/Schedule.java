package com.example.smarthome.model.response;

import com.google.gson.annotations.SerializedName;

public class Schedule {
    @SerializedName("_id")
    private String id; // ID của lịch trình

    private String name; // Tên lịch trình (VD: "Bật đèn tối")

    // Backend dùng .populate("device", "name") nên đây là một đối tượng Device
    private Device device;

    private String time; // Định dạng "HH:mm"
    private String action; // "on" hoặc "off"

    @SerializedName("isActive")
    private boolean active; // Trạng thái lịch trình có đang chạy không

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public Device getDevice() { return device; }
    public String getTime() { return time; }
    public String getAction() { return action; }
    public boolean isActive() { return active; }
}