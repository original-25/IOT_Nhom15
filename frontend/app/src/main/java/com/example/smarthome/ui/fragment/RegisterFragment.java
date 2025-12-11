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

public class RegisterFragment extends Fragment {

    private AuthViewModel authViewModel;
    private EditText emailEditText;
    private EditText nameEditText;
    private EditText passwordEditText;
    private EditText otpEditText;
    private Button sendOtpButton;
    private Button registerButton;
    private LinearLayout step1Layout; // Layout cho nhập Email
    private LinearLayout step2Layout; // Layout cho nhập OTP/Pass

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Cần đảm bảo bạn đã tạo fragment_register.xml
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Ánh xạ các Views (Cần đảm bảo ID trong XML là chính xác)
        emailEditText = view.findViewById(R.id.edit_text_register_email);
        nameEditText = view.findViewById(R.id.edit_text_register_name);
        passwordEditText = view.findViewById(R.id.edit_text_register_password);
        otpEditText = view.findViewById(R.id.edit_text_register_otp);
        sendOtpButton = view.findViewById(R.id.button_send_otp);
        registerButton = view.findViewById(R.id.button_register_complete);

        step1Layout = view.findViewById(R.id.layout_register_step1);
        step2Layout = view.findViewById(R.id.layout_register_step2);

        // Mặc định hiển thị bước 1
        showStep1();

        // Sự kiện gửi OTP
        sendOtpButton.setOnClickListener(v -> handleSendOtp());

        // Sự kiện đăng ký hoàn tất
        registerButton.setOnClickListener(v -> handleRegister());

        observeOtpStatus();
        observeAuthResult();
    }

    // --- Điều khiển giao diện ---
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
        if (email.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập Email.", Toast.LENGTH_SHORT).show();
            return;
        }
        authViewModel.sendRegistrationOtp(email);
    }

    // --- Logic Đăng ký hoàn tất ---
    private void handleRegister() {
        String email = emailEditText.getText().toString().trim();
        String name = nameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String otp = otpEditText.getText().toString().trim();

        if (name.isEmpty() || password.isEmpty() || otp.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin.", Toast.LENGTH_SHORT).show();
            return;
        }

        authViewModel.register(email, name, password, otp);
    }

    // --- Quan sát trạng thái gửi OTP ---
    private void observeOtpStatus() {
        authViewModel.getOtpSentStatus().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                if (success) {
                    showStep2(); // Chuyển sang bước 2 nếu gửi thành công
                } else {
                    Toast.makeText(getContext(), "Gửi OTP thất bại. Email có thể đã được đăng ký.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // --- Quan sát kết quả Đăng ký ---
    private void observeAuthResult() {
        authViewModel.getAuthResult().observe(getViewLifecycleOwner(), authResponse -> {
            if (authResponse != null && authResponse.getToken() != null) {
                // Đăng ký thành công -> Chuyển sang Home
                Toast.makeText(getContext(), "Đăng ký thành công!", Toast.LENGTH_LONG).show();
                ((MainActivity) requireActivity()).replaceFragment(new HomeFragment());
            } else if (authResponse != null) {
                // Lỗi đăng ký (OTP sai, v.v.)
                Toast.makeText(getContext(), "Đăng ký thất bại: " + authResponse.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}