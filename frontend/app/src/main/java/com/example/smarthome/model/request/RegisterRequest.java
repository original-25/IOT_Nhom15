package com.example.smarthome.model.request;

public class RegisterRequest {
    private String email;
    private String name;
     private String password;
     private String otp;
     public RegisterRequest(String email, String username, String password, String otp) {
         this.email = email; this.name = name;
         this.password = password; this.otp = otp;
     }
}
