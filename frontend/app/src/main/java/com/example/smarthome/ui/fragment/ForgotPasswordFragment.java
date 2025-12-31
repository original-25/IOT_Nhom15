package com.example.smarthome.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.smarthome.R;
import com.example.smarthome.ui.MainActivity;
import com.example.smarthome.viewmodel.AuthViewModel;

public class ForgotPasswordFragment extends Fragment {

    private AuthViewModel authViewModel;
    private EditText emailEditText;
    private EditText otpEditText;
    private EditText newPasswordEditText;
    private EditText confirmPasswordEditText;

    // Buttons
    private Button sendOtpButton;
    private Button verifyOtpButton;
    private Button resetPasswordCompleteButton;

    // Layouts
    private LinearLayout step1Layout;
    private LinearLayout step2OtpLayout;
    private LinearLayout step3ResetLayout;

    private String savedResetToken; // Trong luồng đặt lại mật khẩu, đây là resetToken

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // 1. Ánh xạ Views
        emailEditText = view.findViewById(R.id.edit_text_forgot_email);
        otpEditText = view.findViewById(R.id.edit_text_forgot_otp);
        newPasswordEditText = view.findViewById(R.id.edit_text_new_password);
        confirmPasswordEditText = view.findViewById(R.id.edit_text_confirm_password);

        sendOtpButton = view.findViewById(R.id.button_forgot_send_otp);
        verifyOtpButton = view.findViewById(R.id.button_forgot_verify_otp);
        resetPasswordCompleteButton = view.findViewById(R.id.button_reset_password_complete);

        step1Layout = view.findViewById(R.id.layout_forgot_step1);
        step2OtpLayout = view.findViewById(R.id.layout_forgot_step2_otp);
        step3ResetLayout = view.findViewById(R.id.layout_forgot_step3_reset);

        showStep1();

        sendOtpButton.setOnClickListener(v -> handleSendOtp());
        verifyOtpButton.setOnClickListener(v -> handleVerifyOtp());
        resetPasswordCompleteButton.setOnClickListener(v -> handleResetPasswordComplete());

        // 4. Quan sát LiveData
        observeForgotPasswordResult();
        observeVerifyOtpResult();
        observeResetPasswordResult();
    }

    // --- Điều khiển giao diện ---
    private void showStep1() {
        step1Layout.setVisibility(View.VISIBLE);
        step2OtpLayout.setVisibility(View.GONE);
        step3ResetLayout.setVisibility(View.GONE);
        if (emailEditText != null) emailEditText.setEnabled(true);
    }

    private void showStep2Otp() {
        step1Layout.setVisibility(View.GONE);
        step2OtpLayout.setVisibility(View.VISIBLE);
        step3ResetLayout.setVisibility(View.GONE);
        Toast.makeText(getContext(), "Vui lòng kiểm tra email để nhận OTP.", Toast.LENGTH_LONG).show();
    }

    private void showStep3Reset() {
        step1Layout.setVisibility(View.GONE);
        step2OtpLayout.setVisibility(View.GONE);
        step3ResetLayout.setVisibility(View.VISIBLE);
        if (emailEditText != null) emailEditText.setEnabled(false); // Khóa Email
        Toast.makeText(getContext(), "Xác thực thành công. Vui lòng đặt lại mật khẩu.", Toast.LENGTH_SHORT).show();
    }

    // ===========================================
    // HÀM XỬ LÝ LOGIC
    // ===========================================

    // --- Logic Gửi OTP (Bước 1) ---
    private void handleSendOtp() {
        String email = emailEditText.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập Email.", Toast.LENGTH_SHORT).show();
            return;
        }
        authViewModel.forgotPassword(email);
    }

    // --- Logic Xác thực OTP (Bước 2) ---
    private void handleVerifyOtp() {
        String email = emailEditText.getText().toString().trim();
        String otp = otpEditText.getText().toString().trim();

        if (otp.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập mã OTP.", Toast.LENGTH_SHORT).show();
            return;
        }
        authViewModel.verifyResetOtp(email, otp);
    }

    // --- Logic Hoàn tất Đặt lại Mật khẩu (Bước 3) ---
    private void handleResetPasswordComplete() {
        String email = emailEditText.getText().toString().trim();
        String newPassword = newPasswordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập mật khẩu mới và xác nhận.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(getContext(), "Mật khẩu xác nhận không khớp.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (savedResetToken == null || savedResetToken.isEmpty()) {
            Toast.makeText(getContext(), "Lỗi phiên xác thực. Vui lòng thử lại quy trình.", Toast.LENGTH_LONG).show();
            showStep1();
            return;
        }

        authViewModel.resetPassword(email, savedResetToken, newPassword);
    }

    // ===========================================
    // HÀM QUAN SÁT LIVEDATA
    // ===========================================

    // --- Quan sát kết quả Gửi OTP (Bước 1) ---
    private void observeForgotPasswordResult() {
        authViewModel.getForgotPasswordResult().observe(getViewLifecycleOwner(), authResponse -> {
            if (authResponse != null) {
                if (authResponse.isSuccess()) {
                    showStep2Otp();
                } else {
                    Toast.makeText(getContext(), authResponse.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // --- Quan sát kết quả Xác thực OTP (Bước 2) ---
    private void observeVerifyOtpResult() {
        authViewModel.getVerifyResetOtpResult().observe(getViewLifecycleOwner(), authResponse -> {
            if (authResponse != null) {
                if (authResponse.isSuccess()) {
                    String resetToken = authResponse.getResetToken();

                    if (resetToken != null) {
                        this.savedResetToken = resetToken;
                        showStep3Reset();
                    } else {
                        Toast.makeText(getContext(), "Lỗi: Không nhận được token xác thực phiên.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getContext(), authResponse.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // --- Quan sát kết quả Đặt lại Mật khẩu (Bước 3) ---
    private void observeResetPasswordResult() {
        authViewModel.getResetPasswordResult().observe(getViewLifecycleOwner(), authResponse -> {
            if (authResponse != null) {
                if (authResponse.isSuccess()) {
                    Toast.makeText(getContext(), "Đặt lại mật khẩu thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).replaceFragment(new LoginFragment());
                    }
                } else {
                    // Lỗi đặt lại mật khẩu (Token không hợp lệ, lỗi server)
                    Toast.makeText(getContext(), "Đặt lại mật khẩu thất bại: " + authResponse.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}