package com.example.smarthome.model.response;

public class Esp32ProvisionResponse {
    private boolean success;
    private String message; // Thêm để nhận thông báo lỗi từ server
    private String espDeviceId;
    private String claimToken;
    private int expiresIn;

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getEspDeviceId() { return espDeviceId; }
    public String getClaimToken() { return claimToken; }
    public int getExpiresIn() { return expiresIn; }

    // Setters (Dùng cho hàm handleError trong Repo)
    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
}