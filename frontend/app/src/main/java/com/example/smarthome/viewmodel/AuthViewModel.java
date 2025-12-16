package com.example.smarthome.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.annotation.NonNull;

import com.example.smarthome.model.request.LoginRequest;
import com.example.smarthome.model.request.RegisterRequest;
import com.example.smarthome.model.request.ResetPasswordRequest;
import com.example.smarthome.model.request.SendOtpRequest;
import com.example.smarthome.model.request.VerifyOtpRequest;
import com.example.smarthome.model.response.AuthResponse;
import com.example.smarthome.repository.AuthRepository;

public class AuthViewModel extends AndroidViewModel {
    private AuthRepository repository;

    private MutableLiveData<AuthResponse> authResult = new MutableLiveData<>();
    private MutableLiveData<AuthResponse> otpSentResult = new MutableLiveData<>();
    private MutableLiveData<AuthResponse> verifyOtpResult = new MutableLiveData<>();
    private MutableLiveData<AuthResponse> forgotPasswordResult = new MutableLiveData<>();
    private MutableLiveData<AuthResponse> verifyResetOtpResult = new MutableLiveData<>();

    private MutableLiveData<AuthResponse> resetPasswordResult = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        // Khởi tạo Repository cần Context
        repository = new AuthRepository(application.getApplicationContext());
    }

    // --- Public LiveData (View quan sát) ---
    public LiveData<AuthResponse> getAuthResult() {
        return authResult;
    }

    // ĐÃ SỬA GETTER: Trả về AuthResponse
    public LiveData<AuthResponse> getOtpSentResult() {
        return otpSentResult;
    }

    public LiveData<AuthResponse> getVerifyOtpResult() {
        return verifyOtpResult;
    }

    public LiveData<AuthResponse> getForgotPasswordResult() {
        return forgotPasswordResult;
    }

    public MutableLiveData<AuthResponse> getVerifyResetOtpResult() {
        return verifyResetOtpResult;
    }

    public MutableLiveData<AuthResponse> getResetPasswordResult() {
        return resetPasswordResult;
    }

    // --- Hàm Logic (View gọi) ---
    public void login(String email, String password) {
        repository.login(new LoginRequest(email, password), authResult);
    }

    public void sendRegistrationOtp(String email) {
        // ĐÃ SỬA: Thay OtpSentStatus bằng OtpSentResult
        repository.sendRegistrationOtp(new SendOtpRequest(email), otpSentResult);
    }

    public void verifyOtp(String email, String otp) {
        VerifyOtpRequest request = new VerifyOtpRequest(email, otp);
        repository.verifyOtp(request, verifyOtpResult);
    }

    public void register(String email, String name, String password, String verifyToken) {
        RegisterRequest request = new RegisterRequest(verifyToken, email, name, password);
        repository.register(request, authResult);
    }

    public void forgotPassword(String email) {
        SendOtpRequest request = new SendOtpRequest(email);
        repository.forgotPassword(request, forgotPasswordResult);
    }

    public void verifyResetOtp(String email, String otp) {
        VerifyOtpRequest request = new VerifyOtpRequest(email, otp);
        repository.verifyResetOtp(request, verifyResetOtpResult);
    }

    public void resetPassword(String email, String resetToken, String newPassword) {
        ResetPasswordRequest request = new ResetPasswordRequest(email, resetToken, newPassword);
        repository.resetPassword(request, resetPasswordResult);
    }
}
