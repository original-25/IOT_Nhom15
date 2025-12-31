package com.example.smarthome.model.request;

public class RegisterRequest {
    private String verifyToken;
    private String email;
    private String username;
    private String password;

    public RegisterRequest(String verifyToken, String email, String username, String password) {
        this.verifyToken = verifyToken;
        this.email = email;
        this.username = username;
        this.password = password;
    }
}