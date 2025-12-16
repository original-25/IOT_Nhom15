package com.example.smarthome.network;

import com.example.smarthome.model.request.LoginRequest;
import com.example.smarthome.model.request.RegisterRequest;
import com.example.smarthome.model.request.ResetPasswordRequest;
import com.example.smarthome.model.request.SendOtpRequest;
import com.example.smarthome.model.request.VerifyOtpRequest;
import com.example.smarthome.model.response.AuthResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    // ... getDevices() ...

    // Đăng nhập
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    // Đăng ký (Bước 1): Gửi email để nhận OTP
    @POST("api/auth/register/sendRegisterReq")
    Call<AuthResponse> sendRegistrationOtp(@Body SendOtpRequest request);

    // 3. Đăng ký (Bước 2): Xác thực OTP
    @POST("api/auth/register/verifyRegisterOtp")
    Call<AuthResponse> verifyRegistrationOtp(@Body VerifyOtpRequest request);

    // 4. Đăng ký (Bước 3): Tạo Tài khoản
    @POST("api/auth/register/createAccount")
    Call<AuthResponse> createAccount(@Body RegisterRequest request);

    // 5. Quên Pass (Bước 1): Yêu cầu đặt lại mật khẩu
    @POST("api/auth/forgot-password")
    Call<AuthResponse> sendForgotPasswordReq(@Body SendOtpRequest request);

    // 6. Quên Pass (Bước 2): Xác thực OTP đặt lại
    @POST("api/auth/verify-otp")
    Call<AuthResponse> verifyResetOtp(@Body VerifyOtpRequest request);

    // 7. Quên Pass (Bước 3): Đặt lại mật khẩu
    @POST("api/auth/reset-password")
    Call<AuthResponse> resetPassword(@Body ResetPasswordRequest request);

//    // 8. Tạm thời bỏ qua refresh-token
}
