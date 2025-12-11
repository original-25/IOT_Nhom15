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
import com.example.smarthome.viewmodel.AuthViewModel;

public class ForgotPasswordFragment extends Fragment {

    private AuthViewModel authViewModel;
    private EditText emailEditText;
    private EditText otpEditText;
    private EditText newPasswordEditText;
    private Button sendOtpButton;
    private Button resetPasswordButton;
    private LinearLayout step1Layout; // Layout cho nhập Email
    private LinearLayout step2Layout; // Layout cho nhập OTP/Mật khẩu mới

    // ... onCreateView (sử dụng fragment_forgot_password.xml) ...
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Ánh xạ Views
        emailEditText = view.findViewById(R.id.edit_text_forgot_email);
        otpEditText = view.findViewById(R.id.edit_text_forgot_otp);
        newPasswordEditText = view.findViewById(R.id.edit_text_new_password);
        sendOtpButton = view.findViewById(R.id.button_forgot_send_otp);
        resetPasswordButton = view.findViewById(R.id.button_reset_password);
        step1Layout = view.findViewById(R.id.layout_forgot_step1);
        step2Layout = view.findViewById(R.id.layout_forgot_step2);

        showStep1();

        sendOtpButton.setOnClickListener(v -> handleSendOtp());
        resetPasswordButton.setOnClickListener(v -> handleResetPassword());

        observeOtpStatus();
        observeResetResult();
    }

    private void showStep1() {
        step1Layout.setVisibility(View.VISIBLE);
        step2Layout.setVisibility(View.GONE);
    }

    private void showStep2() {
        step1Layout.setVisibility(View.GONE);
        step2Layout.setVisibility(View.VISIBLE);
        Toast.makeText(getContext(), "Vui lòng kiểm tra email để nhận OTP.", Toast.LENGTH_LONG).show();
    }

    // --- Logic Gửi OTP ---
    private void handleSendOtp() {
        String email = emailEditText.getText().toString().trim();
        if (email.isEmpty()) return;

        // Cần thêm hàm sendResetPasswordOtp vào AuthViewModel trước khi dùng
        // authViewModel.sendResetPasswordOtp(email);
        Toast.makeText(getContext(), "Tính năng gửi OTP đang được phát triển...", Toast.LENGTH_SHORT).show();
    }

    // --- Logic Đặt lại mật khẩu ---
    private void handleResetPassword() {
        String email = emailEditText.getText().toString().trim();
        String otp = otpEditText.getText().toString().trim();
        String newPassword = newPasswordEditText.getText().toString().trim();

        if (otp.isEmpty() || newPassword.isEmpty()) return;

        // Cần thêm hàm resetPassword vào AuthViewModel trước khi dùng
        // authViewModel.resetPassword(email, otp, newPassword);
        Toast.makeText(getContext(), "Tính năng đặt lại mật khẩu đang được phát triển...", Toast.LENGTH_SHORT).show();
    }

    // --- Quan sát trạng thái gửi OTP ---
    private void observeOtpStatus() {
        // Cần chỉnh sửa sau khi AuthViewModel được hoàn thiện
    }

    // --- Quan sát kết quả Đặt lại mật khẩu ---
    private void observeResetResult() {
        // Cần chỉnh sửa sau khi AuthViewModel được hoàn thiện
    }
}