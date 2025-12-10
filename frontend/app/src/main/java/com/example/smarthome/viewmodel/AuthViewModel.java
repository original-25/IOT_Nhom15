package com.example.smarthome.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.annotation.NonNull;

import com.example.smarthome.model.request.LoginRequest;
import com.example.smarthome.model.request.OtpRequest;
import com.example.smarthome.model.request.RegisterRequest;
import com.example.smarthome.model.response.AuthResponse;
import com.example.smarthome.repository.AuthRepository;

public class AuthViewModel extends AndroidViewModel {
    private AuthRepository repository;
    // LiveData cho kết quả Đăng nhập và Đăng ký
    private MutableLiveData<AuthResponse> authResult = new MutableLiveData<>();
    // LiveData cho trạng thái gửi OTP
    private MutableLiveData<Boolean> otpSentStatus = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        // Khởi tạo Repository cần Context
        repository = new AuthRepository(application.getApplicationContext());
    }

    // --- Public LiveData (View quan sát) ---
    public LiveData<AuthResponse> getAuthResult() {
        return authResult;
    }

    public LiveData<Boolean> getOtpSentStatus() {
        return otpSentStatus;
    }

    // --- Hàm Logic (View gọi) ---
    public void login(String email, String password) {
        repository.login(new LoginRequest(email, password), authResult);
    }

    public void sendRegistrationOtp(String email) {
        repository.sendRegistrationOtp(new OtpRequest(email), otpSentStatus);
    }

    public void register(String email, String name, String password, String otp) {
        repository.register(new RegisterRequest(email, name, password, otp), authResult);
    }

    // ... Thêm hàm resetPassword ...

    public boolean isUserLoggedIn() {
        return repository.isLoggedIn();
    }
}
