package com.example.smarthome.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.smarthome.R;
import com.example.smarthome.ui.fragment.LoginFragment; // Cần import LoginFragment

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // [Cần đảm bảo R.id.main là ID của layout chính trong activity_main.xml]
        setContentView(R.layout.activity_main);

        // --- GIỮ LẠI LOGIC XỬ LÝ WINDOW INSETS (Không thay đổi) ---
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ==============================================================
        // BỔ SUNG: LOGIC TẢI FRAGMENT (CHỈ TẢI LẦN ĐẦU)
        // ==============================================================
        if (savedInstanceState == null) {
            // Kiểm tra savedInstanceState == null để đảm bảo Fragment chỉ được thêm một lần
            // (khi Activity bị hủy và tạo lại, Fragment sẽ tự động khôi phục)

            // 1. Khởi tạo FragmentManager
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            // 2. Thêm LoginFragment vào Container
            // R.id.fragment_container: ID của FrameLayout trong activity_main.xml
            fragmentTransaction.add(R.id.fragment_container, new LoginFragment());

            // 3. Thực hiện giao dịch
            fragmentTransaction.commit();
        }
    }

    // Hàm tiện ích để chuyển đổi Fragment đơn giản
    public void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null); // Cho phép nhấn nút Back
        transaction.commit();
    }
}