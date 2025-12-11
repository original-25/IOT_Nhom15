package com.example.smarthome.model.request;

public class ResetPasswordRequest {
    private String email;
    private String otp;
    private String newPassword;
    public ResetPasswordRequest(String email, String otp, String newPassword) {
        this.email = email; this.otp = otp;
        this.newPassword = newPassword;
    }
}
