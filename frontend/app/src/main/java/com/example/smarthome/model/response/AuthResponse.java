package com.example.smarthome.model.response;

import com.example.smarthome.model.data.User;

public class AuthResponse {
    private String token;
    private User user;
    private String message;
    private boolean success;

    public AuthResponse() {}

    public String getToken() {
        return token;
    }

    public User getUser() {
        return user;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Nếu Backend có trả về trường success, bạn nên thêm Setter cho nó
    public void setSuccess(boolean success) {
        this.success = success;
    }
}
