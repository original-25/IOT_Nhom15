package com.example.smarthome.model.data;

import com.google.gson.annotations.SerializedName;

public class User {
    private String id;
    private String email;

    @SerializedName("username") // Ánh xạ chính xác key "username" từ JSON của Backend
    private String name;

    private String role;

    public User() {}

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    // Hàm này giờ sẽ trả về giá trị của "username" từ Server
    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRole(String role) {
        this.role = role;
    }
}