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
import com.example.smarthome.model.response.AuthResponse;
import com.example.smarthome.ui.fragment.HomeFragment; // Đảm bảo import HomeFragment nếu nó tồn tại

public class RegisterFragment extends Fragment {

    private AuthViewModel authViewModel;
    private EditText emailEditText;
    private EditText nameEditText;
    private EditText passwordEditText;
    private EditText otpEditText;

    private Button sendOtpButton;
    private Button verifyOtpButton;
    private Button registerButton;

    // Layouts
    private LinearLayout step1Layout;
    private LinearLayout step2Layout;
    private LinearLayout step3Layout;

    // TRƯỜNG MỚI: Lưu trữ verifyToken sau khi xác thực OTP thành công
    private String savedVerifyToken;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // --- Ánh xạ Views ---
        emailEditText = view.findViewById(R.id.edit_text_register_email);
        nameEditText = view.findViewById(R.id.edit_text_register_name);
        passwordEditText = view.findViewById(R.id.edit_text_register_password);
        otpEditText = view.findViewById(R.id.edit_text_register_otp);

        sendOtpButton = view.findViewById(R.id.button_send_otp);
        verifyOtpButton = view.findViewById(R.id.button_verify_otp);
        registerButton = view.findViewById(R.id.button_register_complete);

        // --- Ánh xạ Layouts ---
        step1Layout = view.findViewById(R.id.layout_register_step1);
        step2Layout = view.findViewById(R.id.layout_register_step2_otp);
        step3Layout = view.findViewById(R.id.layout_register_step3_info);

        showStep1();

        // --- Sự kiện Listeners ---
        sendOtpButton.setOnClickListener(v -> handleSendOtp());
        verifyOtpButton.setOnClickListener(v -> handleVerifyOtp());

        // Bỏ comment và thêm sự kiện cho nút Đăng ký cuối cùng
        registerButton.setOnClickListener(v -> handleRegister());

        // --- Quan sát LiveData ---
        observeOtpResult();
        observeVerifyOtpResult();
        observeAuthResult();
    }

    // --- Điều khiển giao diện ---
    private void showStep1() {
        step1Layout.setVisibility(View.VISIBLE);
        step2Layout.setVisibility(View.GONE);
        step3Layout.setVisibility(View.GONE);
    }

    private void showStep2Otp() {
        step1Layout.setVisibility(View.GONE);
        step2Layout.setVisibility(View.VISIBLE);
        step3Layout.setVisibility(View.GONE);
        Toast.makeText(getContext(), "Vui lòng kiểm tra email để nhận OTP.", Toast.LENGTH_LONG).show();
    }

    private void showStep3Register() {
        step1Layout.setVisibility(View.GONE);
        step2Layout.setVisibility(View.GONE);
        step3Layout.setVisibility(View.VISIBLE);
        emailEditText.setEnabled(false); // Ngăn sửa email
        Toast.makeText(getContext(), "Xác thực thành công. Vui lòng hoàn tất thông tin cá nhân.", Toast.LENGTH_SHORT).show();
    }

    // --- Logic Gửi OTP (Bước 1) ---
    private void handleSendOtp() {
        String email = emailEditText.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập Email.", Toast.LENGTH_SHORT).show();
            return;
        }
        authViewModel.sendRegistrationOtp(email);
    }

    // --- Logic Xác thực OTP (Bước 2) ---
    private void handleVerifyOtp() {
        String email = emailEditText.getText().toString().trim();
        String otp = otpEditText.getText().toString().trim();

        if (otp.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập mã OTP.", Toast.LENGTH_SHORT).show();
            return;
        }

        authViewModel.verifyOtp(email, otp);
    }


    // --- Logic Đăng ký hoàn tất (Bước 3) ---
    private void handleRegister() {
        String email = emailEditText.getText().toString().trim();
        String name = nameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // 1. Kiểm tra dữ liệu
        if (name.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập đầy đủ Tên người dùng và Mật khẩu.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Kiểm tra Token
        if (savedVerifyToken == null || savedVerifyToken.isEmpty()) {
            Toast.makeText(getContext(), "Lỗi phiên xác thực. Vui lòng thử lại từ bước 1.", Toast.LENGTH_LONG).show();
            showStep1(); // Quay lại bước 1
            return;
        }

        // 3. Gọi ViewModel để tạo tài khoản
        authViewModel.register(email, name, password, savedVerifyToken);
    }

    // --- Quan sát kết quả Gửi OTP (Bước 1) ---
    private void observeOtpResult() {
        authViewModel.getOtpSentResult().observe(getViewLifecycleOwner(), authResponse -> {
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
        authViewModel.getVerifyOtpResult().observe(getViewLifecycleOwner(), authResponse -> {
            if (authResponse != null) {
                if (authResponse.isSuccess()) {
                    // LƯU TRỮ TOKEN: Lưu verifyToken (Backend trả về trong data.verifyToken)
                    if (authResponse.getData() != null && authResponse.getData().getVerifyToken() != null) {
                        this.savedVerifyToken = authResponse.getData().getVerifyToken();
                        showStep3Register();
                    } else {
                        Toast.makeText(getContext(), "Lỗi: Không nhận được token xác thực.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    // Lỗi: OTP sai, hết hạn, v.v.
                    Toast.makeText(getContext(), authResponse.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // --- Quan sát kết quả Đăng ký (Bước 3) ---
    private void observeAuthResult() {
        authViewModel.getAuthResult().observe(getViewLifecycleOwner(), authResponse -> {
            if (authResponse != null) {
                if (authResponse.isSuccess()) {
                    Toast.makeText(getContext(), "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();

                    // Chuyển sang màn hình Đăng nhập (LoginFragment)
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).replaceFragment(new LoginFragment());
                    }
                } else {
                    // Lỗi đăng ký cuối cùng (Token hết hạn, lỗi server, v.v.)
                    Toast.makeText(getContext(), "Đăng ký thất bại: " + authResponse.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}