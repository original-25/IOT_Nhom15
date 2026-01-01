package com.example.smarthome.model.response;

import com.google.gson.annotations.SerializedName;

public class Esp32ProvisionResponse implements java.io.Serializable {
    private boolean success;
    private String message;
    private String espDeviceId;
    private String claimToken;
    private int expiresIn;

    // BỔ SUNG: Hai trường này để chứa thông tin Wi-Fi người dùng nhập
    private String ssid;
    private String pass;

    @SerializedName("mqtt")
    private MqttConfig mqtt;

    public static class MqttConfig implements java.io.Serializable {
        private String host;
        private int port;
        private String username;
        private String baseTopic;

        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getUsername() { return username; }
        public String getBaseTopic() { return baseTopic; }
    }

    // --- Getters & Setters mới cho SSID và Password ---
    public String getSsid() { return ssid; }
    public void setSsid(String ssid) { this.ssid = ssid; }

    public String getPass() { return pass; }
    public void setPass(String pass) { this.pass = pass; }

    // --- Getters cũ ---
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getEspDeviceId() { return espDeviceId; }
    public String getClaimToken() { return claimToken; }
    public int getExpiresIn() { return expiresIn; }
    public MqttConfig getMqtt() { return mqtt; }

    // --- Setters cũ ---
    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setEspDeviceId(String espDeviceId) { this.espDeviceId = espDeviceId; }
    public void setClaimToken(String claimToken) { this.claimToken = claimToken; }
    public void setMqtt(MqttConfig mqtt) { this.mqtt = mqtt; }
}