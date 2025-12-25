package com.example.smarthome.model.response;

public class Esp32ProvisionResponse {
    private boolean success;
    private String espDeviceId;
    private String claimToken;
    private int expiresIn;

    // Getters và Setters
    public String getEspDeviceId() { return espDeviceId; }
    public String getClaimToken() { return claimToken; }
    public boolean isSuccess() { return success; }
}
