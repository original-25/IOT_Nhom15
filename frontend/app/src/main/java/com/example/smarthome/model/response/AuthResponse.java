package com.example.smarthome.model.response;

import com.example.smarthome.model.data.User;
import com.google.gson.annotations.SerializedName;

public class AuthResponse {

    // Backend trả về 'success', 'message', 'data', 'errorCode'
    private boolean success;
    private String message;
    private String errorCode;

    private Data data;

    // --- Getters cho AuthResponse ---
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Data getData() {
        return data;
    }

    // Các phương thức tiện ích để truy cập dữ liệu sâu hơn
    public User getUser() {
        return (data != null) ? data.getUser() : null;
    }

    public String getAccessToken() {
        return (data != null) ? data.getAccessToken() : null;
    }

    // Token xác thực cho luồng Đặt lại Mật khẩu (MỚI)
    public String getResetToken() {
        return (data != null) ? data.getResetToken() : null;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public void setData(Data data) {
        this.data = data;
    }


    // ===========================================
    // LỚP DATA NỘI BỘ
    // ===========================================
    public static class Data {

        @SerializedName("accessToken")
        private String accessToken;

        @SerializedName("refreshToken")
        private String refreshToken;

        @SerializedName("verifyToken")
        private String verifyToken;

        @SerializedName("resetToken") // <-- BỔ SUNG TRƯỜNG NÀY (Từ Backend verifyResetOtp)
        private String resetToken;

        private User user; // Khớp với đối tượng user

        // Getter cho lớp Data
        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public User getUser() {
            return user;
        }

        public String getVerifyToken() {
            return verifyToken;
        }

        public String getResetToken() {
            return resetToken;
        }
    }
}