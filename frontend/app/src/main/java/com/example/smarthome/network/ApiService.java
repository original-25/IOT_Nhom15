package com.example.smarthome.network;

import com.example.smarthome.model.request.LoginRequest;
import com.example.smarthome.model.request.OtpRequest;
import com.example.smarthome.model.request.RegisterRequest;
import com.example.smarthome.model.request.ResetPasswordRequest;
import com.example.smarthome.model.response.AuthResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    // ... getDevices() ...

    // Đăng nhập
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    // Bước 1 Đăng ký & Quên mật khẩu: Gửi email để nhận OTP
    @POST("api/auth/register-send-otp")
    Call<Void> sendRegistrationOtp(@Body OtpRequest request);

    // Bước 2 Đăng ký: Xác thực OTP và tạo tài khoản
    @POST("api/auth/register-complete")
    Call<AuthResponse> register(@Body RegisterRequest request);

    // Quên mật khẩu: Gửi email để nhận OTP đặt lại
    @POST("api/auth/forgot-password-send-otp")
    Call<Void> sendResetPasswordOtp(@Body OtpRequest request);

    // Đặt lại mật khẩu: Gửi OTP và mật khẩu mới
    @POST("api/auth/reset-password")
    Call<AuthResponse> resetPassword(@Body ResetPasswordRequest request);
}
