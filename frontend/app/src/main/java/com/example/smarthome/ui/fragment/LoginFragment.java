package com.example.smarthome.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.smarthome.R;
import com.example.smarthome.viewmodel.AuthViewModel;
import com.example.smarthome.ui.MainActivity;

public class LoginFragment extends Fragment {

    private AuthViewModel authViewModel;
    private EditText emailEditText;
    private EditText passwordEditText;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Khởi tạo ViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // 2. Ánh xạ các thành phần giao diện
        emailEditText = view.findViewById(R.id.edit_text_email);
        passwordEditText = view.findViewById(R.id.edit_text_password);
        Button loginButton = view.findViewById(R.id.button_login);
        TextView registerTextView = view.findViewById(R.id.text_register);
        TextView forgotPasswordTextView = view.findViewById(R.id.text_forgot_password);

        loginButton.setOnClickListener(v -> handleLogin());

        // Sự kiện Chuyển sang Đăng ký
        registerTextView.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).replaceFragment(new RegisterFragment());
        });

        // Sự kiện Quên Mật khẩu
        forgotPasswordTextView.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).replaceFragment(new ForgotPasswordFragment());
        });

        observeAuthResult();
    }

    private void handleLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập Email và Mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        authViewModel.login(email, password);
    }

    // --- Hàm Quan sát LiveData ---
    private void observeAuthResult() {
        authViewModel.getAuthResult().observe(getViewLifecycleOwner(), authResponse -> {
            if (authResponse != null) {
                if (authResponse.getAccessToken() != null) {
                    // Đăng nhập thành công -> Chuyển sang HomeFragment
                    Toast.makeText(getContext(), "Đăng nhập thành công! Chuyển sang màn hình chính.", Toast.LENGTH_LONG).show();
                    // Thao tác chuyển Fragment:
                    ((MainActivity) requireActivity()).replaceFragment(new HomeFragment());
                } else {
                    // Đăng nhập thất bại (Lỗi server/sai pass)
                    Toast.makeText(getContext(), "Lỗi: " + authResponse.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}