package com.example.smarthome.ui.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.smarthome.R;
import com.example.smarthome.model.response.Esp32Device;
import com.example.smarthome.viewmodel.ESPViewModel;
import com.google.gson.Gson;

import org.jspecify.annotations.NonNull;

public class ESPDetailFragment extends Fragment {
    private Esp32Device device;
    private ESPViewModel espViewModel;
    private String authToken;
    private String homeId;

    // Khai báo các TextView để cập nhật UI sau khi sửa tên
    private TextView tvName, tvStatus, tvId, tvTopic, tvCreated, tvClaimed, tvUpdated;

    public static ESPDetailFragment newInstance(Esp32Device device) {
        ESPDetailFragment fragment = new ESPDetailFragment();
        Bundle args = new Bundle();
        args.putString("device_json", new Gson().toJson(device));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_esp_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Lấy Token và HomeId từ SharedPreferences hoặc mHomeId (tùy cách bạn lưu)
        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        authToken = prefs.getString("authToken", "");

        // 2. Khởi tạo ViewModel (Dùng chung với Activity để đồng bộ dữ liệu)
        espViewModel = new ViewModelProvider(requireActivity()).get(ESPViewModel.class);

        // 3. Ánh xạ View
        tvName = view.findViewById(R.id.tv_detail_name);
        tvStatus = view.findViewById(R.id.tv_detail_status);
        tvId = view.findViewById(R.id.tv_detail_id);
        tvTopic = view.findViewById(R.id.tv_detail_topic);
        tvCreated = view.findViewById(R.id.tv_detail_created);
        tvClaimed = view.findViewById(R.id.tv_detail_claimed);
        tvUpdated = view.findViewById(R.id.tv_detail_updated);
        Button btnUpdate = view.findViewById(R.id.btn_detail_update);
        Button btnDelete = view.findViewById(R.id.btn_detail_delete);

        // 4. Lấy dữ liệu device từ Arguments
        if (getArguments() != null) {
            String json = getArguments().getString("device_json");
            this.device = new Gson().fromJson(json, Esp32Device.class);
            this.homeId = device.getHome(); // Lấy homeId từ chính object device
            updateUI(device);
        }

        // 5. Thiết lập sự kiện nút bấm
        btnUpdate.setOnClickListener(v -> showUpdateDeviceDialog(device));
        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog(device));

        // 6. Đăng ký quan sát kết quả
        observeViewModel();
    }

    private void updateUI(Esp32Device device) {
        tvName.setText(device.getName());
        tvId.setText(device.getId());
        tvTopic.setText(device.getMqttBaseTopic());
        tvStatus.setText("Trạng thái: " + device.getStatus());

        if ("online".equals(device.getStatus()) || "provisioned".equals(device.getStatus())) {
            tvStatus.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            tvStatus.setTextColor(Color.parseColor("#F44336"));
        }

        tvCreated.setText(formatDate(device.getCreatedAt()));
        tvClaimed.setText(formatDate(device.getClaimedAt()));
        tvUpdated.setText(formatDate(device.getUpdatedAt()));
    }

    private void observeViewModel() {
        // Quan sát kết quả Cập nhật
        espViewModel.getUpdateResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null) {
                if (response.isSuccess()) {
                    Toast.makeText(getContext(), "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    this.device = response.getData(); // Cập nhật object cục bộ
                    updateUI(device); // Cập nhật lại giao diện
                } else {
                    Toast.makeText(getContext(), response.getMessage(), Toast.LENGTH_SHORT).show();
                }
                espViewModel.resetUpdateResult(); // Quan trọng: Reset để tránh lặp lại Toast
            }
        });

        espViewModel.getDeleteEspResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null) {
                if (response.isSuccess()) {
                    Toast.makeText(getContext(), "Đã xóa bộ điều khiển thành công", Toast.LENGTH_SHORT).show();
                    // Quay lại màn hình trước hoặc làm mới danh sách
                    requireActivity().getSupportFragmentManager().popBackStack();
                } else {
                    Toast.makeText(getContext(), response.getMessage(), Toast.LENGTH_SHORT).show();
                }
                espViewModel.resetDeleteEspResult();
            }
        });
    }

    private void showUpdateDeviceDialog(Esp32Device device) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        final EditText input = new EditText(requireContext());
        input.setText(device.getName());
        input.setSelection(input.getText().length()); // Đưa con trỏ xuống cuối

        FrameLayout container = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 60; params.rightMargin = 60;
        input.setLayoutParams(params);
        container.addView(input);

        builder.setTitle("Cập nhật tên thiết bị")
                .setView(container)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        // GỌI API THỰC TẾ
                        espViewModel.updateEspDevice(authToken, homeId, device.getId(), newName);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDeleteConfirmDialog(Esp32Device device) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa " + device.getName() + " không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    espViewModel.deleteEsp32(authToken, homeId, device.getId());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "Chưa cập nhật";
        try {
            return isoDate.replace("T", " ").substring(0, 16);
        } catch (Exception e) {
            return isoDate;
        }
    }
}