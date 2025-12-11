package com.example.smarthome.repository;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.lifecycle.MutableLiveData;
import com.example.smarthome.model.data.*;
import com.example.smarthome.model.request.LoginRequest;
import com.example.smarthome.model.request.OtpRequest;
import com.example.smarthome.model.request.RegisterRequest;
import com.example.smarthome.model.request.ResetPasswordRequest;
import com.example.smarthome.model.response.AuthResponse;
import com.example.smarthome.network.ApiService;
import com.example.smarthome.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {
    private ApiService apiService;
    private Context context;
    private static final String PREFS_NAME = "AuthPrefs";
    private static final String TOKEN_KEY = "authToken";

    public AuthRepository(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = RetrofitClient.getApiService();
    }

    // --- Token Management ---
    public void saveToken(String token) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(TOKEN_KEY, token).apply();
    }

    public String getToken() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(TOKEN_KEY, null);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public void login(LoginRequest request, MutableLiveData<AuthResponse> loginResult) {
        apiService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    saveToken(authResponse.getToken()); // LƯU TOKEN
                    loginResult.postValue(authResponse);
                } else {
                    // Tạo một AuthResponse giả để báo lỗi cho ViewModel
                    AuthResponse errorResponse = new AuthResponse();
                    errorResponse.setMessage("Tên đăng nhập hoặc mật khẩu không đúng.");
                    loginResult.postValue(errorResponse);
                }
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                // Xử lý lỗi mạng/server
                AuthResponse errorResponse = new AuthResponse();
                errorResponse.setMessage("Lỗi kết nối server: " + t.getMessage());
                loginResult.postValue(errorResponse);
            }
        });
    }

    public void sendRegistrationOtp(OtpRequest request, MutableLiveData<Boolean> otpStatus) {
        apiService.sendRegistrationOtp(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    otpStatus.postValue(true); // Gửi OTP thành công
                } else {
                    otpStatus.postValue(false); // Gửi OTP thất bại (Email đã tồn tại...)
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                otpStatus.postValue(false);
            }
        });
    }

    public void register(RegisterRequest request, MutableLiveData<AuthResponse> registerResult) {
        apiService.register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    saveToken(authResponse.getToken()); // LƯU TOKEN
                    registerResult.postValue(authResponse);
                } else {
                    AuthResponse errorResponse = new AuthResponse();
                    errorResponse.setMessage("Đăng ký thất bại. Vui lòng kiểm tra lại OTP/thông tin.");
                }
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                // Xử lý lỗi mạng/server
                AuthResponse errorResponse = new AuthResponse();
                errorResponse.setMessage("Lỗi kết nối server trong quá trình đăng ký: " + t.getMessage());
                registerResult.postValue(errorResponse);
            }
        });
    }

    // --- Hàm Gửi OTP Đặt lại mật khẩu (Quên mật khẩu) ---
    public void sendResetPasswordOtp(OtpRequest request, MutableLiveData<Boolean> otpStatus) {
        apiService.sendResetPasswordOtp(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    otpStatus.postValue(true); // Gửi OTP thành công
                } else {
                    otpStatus.postValue(false); // Gửi OTP thất bại (Email không tồn tại...)
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                otpStatus.postValue(false);
            }
        });
    }

    // --- Hàm Đặt lại mật khẩu ---
    public void resetPassword(ResetPasswordRequest request, MutableLiveData<AuthResponse> resetResult) {
        apiService.resetPassword(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Trả về kết quả thành công (không lưu token vì người dùng cần đăng nhập lại)
                    resetResult.postValue(response.body());
                } else {
                    // Xử lý lỗi (OTP sai, mật khẩu không hợp lệ)
                    AuthResponse errorResponse = new AuthResponse();
                    errorResponse.setMessage("Đặt lại mật khẩu thất bại. Vui lòng kiểm tra lại OTP.");
                    resetResult.postValue(errorResponse);
                }
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                // Xử lý lỗi mạng/server
                AuthResponse errorResponse = new AuthResponse();
                errorResponse.setMessage("Lỗi kết nối server trong quá trình đặt lại mật khẩu: " + t.getMessage());
                resetResult.postValue(errorResponse);
            }
        });
    }
}
