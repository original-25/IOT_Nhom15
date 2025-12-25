package com.example.smarthome.ui.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthome.R;
import com.example.smarthome.viewmodel.ESPViewModel;

import java.util.ArrayList;
import java.util.List;

public class ESPManagerFragment extends Fragment {

    private static final String ARG_HOME_NAME = "home_name";
    private static final String ARG_HOME_ID = "home_id";

    private String mHomeId;
    private String mHomeName;
    private String authToken;

    private ESPViewModel espViewModel;
    private RecyclerView recyclerView;
    // private ESPAdapter adapter; // Bạn sẽ cần tạo adapter này sau
    // private final List<ESPDevice> listDevices = new ArrayList<>();

    public static ESPManagerFragment newInstance(String homeId, String homeName) {
        ESPManagerFragment fragment = new ESPManagerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_HOME_ID, homeId);
        args.putString(ARG_HOME_NAME, homeName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mHomeId = getArguments().getString(ARG_HOME_ID);
            mHomeName = getArguments().getString(ARG_HOME_NAME);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_esp_manager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Khởi tạo ViewModel
        espViewModel = new ViewModelProvider(this).get(ESPViewModel.class);

        // 2. Lấy Token từ SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        authToken = prefs.getString("authToken", "");

        // 3. Ánh xạ View và hiển thị tiêu đề
        TextView textTitle = view.findViewById(R.id.text_title_manager);
        textTitle.setText(mHomeName != null ? "Bộ quản lý thiết bị của " + mHomeName : "Bộ quản lý thiết bị");

        // 4. Thiết lập RecyclerView (Tạm thời để logic chờ Adapter)
        recyclerView = view.findViewById(R.id.recycler_devices);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // adapter = new ESPAdapter(listDevices, ...);
        // recyclerView.setAdapter(adapter);

        // 5. Quan sát các kết quả từ ViewModel
        observeProvisionResult();
        // observeDevicesListResult(); // Hàm để quan sát danh sách ESP hiện có

        // 6. Tải danh sách ESP hiện có của nhà này (nếu có API)
        // if (authToken != null && !authToken.isEmpty()) {
        //    espViewModel.fetchDevicesByHome(authToken, mHomeId);
        // }

        // 7. Sự kiện nút Thêm (+)
        ImageButton btnAddDevice = view.findViewById(R.id.btn_add_device);
        btnAddDevice.setOnClickListener(v -> showAddEspDialog());
    }

    // Quan sát kết quả Provision (Tạo thiết bị)
    private void observeProvisionResult() {
//        espViewModel.getProvisionResult().observe(getViewLifecycleOwner(), response -> {
//            if (response != null && response.isSuccess()) {
//                Toast.makeText(getContext(), "Thêm bộ quản lý thành công!", Toast.LENGTH_SHORT).show();
//                // Sau khi thêm thành công, thường ta sẽ gọi lại API lấy danh sách để cập nhật UI
//                // espViewModel.fetchDevicesByHome(authToken, mHomeId);
//            } else if (response != null) {
//                Toast.makeText(getContext(), "Lỗi: " + response.getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        });
    }

    private void showAddEspDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        // Tạo View nhập liệu linh hoạt
        final EditText input = new EditText(getContext());
        input.setHint("Ví dụ: Bộ quản lý thiết bị 1(ESP1)");

        FrameLayout container = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50;
        params.rightMargin = 50;
        input.setLayoutParams(params);
        container.addView(input);

        builder.setTitle("Thêm bộ quản lý mới")
                .setMessage("Nhập tên cho thiết bị ESP32 của bạn")
                .setView(container)
                .setCancelable(false)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String espName = input.getText().toString().trim();
                    if (!espName.isEmpty()) {
                        // Gọi qua ViewModel theo đúng chuẩn MVVM
//                        espViewModel.provisionEsp32(authToken, mHomeId, espName);
                    } else {
                        Toast.makeText(getContext(), "Tên không được để trống", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }
}