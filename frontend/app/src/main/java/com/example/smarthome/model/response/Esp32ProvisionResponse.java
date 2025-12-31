package com.example.smarthome.model.response;

import com.google.gson.annotations.SerializedName;

public class Esp32ProvisionResponse implements java.io.Serializable {
    private boolean success;
    private String message;
    private String espDeviceId;
    private String claimToken;
    private int expiresIn;

    // Thêm đối tượng mqtt để hứng dữ liệu từ backend
    @SerializedName("mqtt")
    private MqttConfig mqtt;

    // --- Inner Class để chứa thông tin cấu hình MQTT ---
    public static class MqttConfig implements java.io.Serializable {
        private String host;
        private int port;
        private String username;
        private String password;
        private String baseTopic;

        // Getters cho MqttConfig
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getBaseTopic() { return baseTopic; }
    }

    // --- Getters cho class chính ---
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getEspDeviceId() { return espDeviceId; }
    public String getClaimToken() { return claimToken; }
    public int getExpiresIn() { return expiresIn; }
    public MqttConfig getMqtt() { return mqtt; }

    // --- Setters (Dùng cho hàm handleError trong Repo hoặc khi cần khởi tạo thủ công) ---
    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setEspDeviceId(String espDeviceId) { this.espDeviceId = espDeviceId; }
    public void setClaimToken(String claimToken) { this.claimToken = claimToken; }
    public void setMqtt(MqttConfig mqtt) { this.mqtt = mqtt; }
}