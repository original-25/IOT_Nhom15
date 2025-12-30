package com.example.smarthome.model.response;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class HomeResponse<T> {
    private boolean success;
    private String message;
    private String errorCode;
    private T data;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    // Dùng cho danh sách các ngôi nhà (My Homes)
    public static class HomeData {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("role")
        private String role;

        public String getId() { return id; }
        public String getName() { return name; }
        public String getRole() { return role; }
    }

    // Dùng cho việc kiểm tra trạng thái ESP32 (Polling)
    public static class DeviceStatus {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("status")
        private String status;

        public String getId() { return id; }
        public String getName() { return name; }
        public String getStatus() { return status; }
    }

    public static class HomeDetailData {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("members")
        private List<MemberData> members;

        public String getId() { return id; }
        public String getName() { return name; }
        public List<MemberData> getMembers() { return members; }
    }

    // Chi tiết từng thành viên trong mảng members
    public static class MemberData {
        @SerializedName("userId")
        private String userId;

        @SerializedName("role")
        private String role;

        @SerializedName("email")
        private String email;

        public String getUserId() { return userId; }
        public String getRole() { return role; }
        public String getEmail() { return email; }
    }
}