package com.example.smarthome.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smarthome.R;
import com.example.smarthome.ui.MainActivity;

public class HomeFragment extends Fragment {

    // ... Khởi tạo ViewModel cho thiết bị (HomeViewModel) sau này ...

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Tải layout của màn hình chính (ví dụ: fragment_home.xml)
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ví dụ: Thêm nút Logout tạm thời
        Button logoutButton = view.findViewById(R.id.button_logout);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> handleLogout());
        }

        Toast.makeText(getContext(), "Chào mừng đến với Smart Home!", Toast.LENGTH_LONG).show();

        // ... Logic quan sát danh sách thiết bị từ HomeViewModel ...
    }

    private void handleLogout() {
        // Xóa Token và chuyển về màn hình Login
        // Bạn cần thêm hàm logout vào AuthRepository và AuthViewModel

        Toast.makeText(getContext(), "Đã đăng xuất.", Toast.LENGTH_SHORT).show();
        ((MainActivity) requireActivity()).replaceFragment(new LoginFragment());
    }
}