package com.example.smarthome.model.request;

public class ResetPasswordRequest {
    private String email;
    private String resetToken;
    private String newPassword;

    public ResetPasswordRequest(String email, String resetToken, String newPassword) {
        this.email = email;
        this.resetToken = resetToken;
        this.newPassword = newPassword;
    }
}