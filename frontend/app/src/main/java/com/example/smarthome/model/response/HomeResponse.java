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
}