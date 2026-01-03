package com.example.smarthome.ui.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthome.R;
import com.example.smarthome.model.response.Device;
import com.example.smarthome.ui.adapter.DeviceAdapter;
import com.example.smarthome.viewmodel.ESPViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceManagerFragment extends Fragment {
    private static final String ARG_HOME_ID = "home_id";
    private static final String ARG_ESP_ID = "esp_id";

    private String mHomeId, mEspId, authToken;
    private ESPViewModel espViewModel;
    private DeviceAdapter adapter;
    private List<Device> deviceList = new ArrayList<>();

    // Biến lưu trữ ID thiết bị đang thao tác để cập nhật giao diện cục bộ
    private String currentProcessingDeviceId = "";

    public static DeviceManagerFragment newInstance(String homeId, String espId) {
        DeviceManagerFragment fragment = new DeviceManagerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_HOME_ID, homeId);
        args.putString(ARG_ESP_ID, espId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mHomeId = getArguments().getString(ARG_HOME_ID);
            mEspId = getArguments().getString(ARG_ESP_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_manager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        authToken = prefs.getString("authToken", "");

        espViewModel = new ViewModelProvider(this).get(ESPViewModel.class);

        // 1. Cấu hình RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.rv_sub_devices);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new DeviceAdapter(deviceList, new DeviceAdapter.OnDeviceClickListener() {
            @Override
            public void onDeviceClick(Device device) {
                // Xử lý khi nhấn vào item
            }

            @Override
            public void onPowerChange(Device device, boolean isOn) {
                // Lưu ID để cập nhật đúng item sau khi có phản hồi từ server
                currentProcessingDeviceId = device.getId();
                espViewModel.controlDevice(authToken, mHomeId, device.getId(), isOn);
            }

            @Override
            public void onUpdateSubDevice(Device device) {
                showUpdateDeviceDialog(device);
            }

            @Override
            public void onAdvancedSettings(Device device) {
                Toast.makeText(getContext(), "Tính năng nâng cao: " + device.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteSubDevice(Device device) {
                showDeleteConfirmDialog(device);
            }
        });

        recyclerView.setAdapter(adapter);

        // 2. Nút thêm thiết bị mới
        ImageButton btnAdd = view.findViewById(R.id.btn_add_sub_device);
        btnAdd.setOnClickListener(v -> showAddDeviceDialog());

        // 3. Thiết lập quan sát dữ liệu (Observers)
        observeViewModel();

        // 4. Lấy danh sách ban đầu
        refreshList();
    }

    private void observeViewModel() {
        // Lắng nghe danh sách thiết bị con
        espViewModel.getSubDevicesResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess()) {
                deviceList.clear();
                deviceList.addAll(response.getData());
                adapter.notifyDataSetChanged();
            }
        });

        // Lắng nghe kết quả điều khiển (Bật/Tắt)
        espViewModel.getCommandResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null) {
                if (response.isSuccess()) {
                    // Tối ưu: Lấy lại trạng thái riêng của thiết bị này
                    espViewModel.fetchDeviceState(authToken, mHomeId, currentProcessingDeviceId);
                } else {
                    Toast.makeText(getContext(), "Lỗi: " + response.getMessage(), Toast.LENGTH_SHORT).show();
                    refreshList(); // Lỗi thì tải lại cả danh sách cho chắc chắn
                }
                espViewModel.resetCommandResult();
            }
        });

        // Lắng nghe trạng thái thiết bị đơn lẻ (sau khi send command)
        espViewModel.getStateResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess()) {
                if (response.getData() != null && response.getData().getLastState() != null) {
                    // Cập nhật duy nhất item đó trong Adapter
                    adapter.updateSingleDeviceState(currentProcessingDeviceId, response.getData().getLastState());
                }
                espViewModel.resetStateResult();
            }
        });

        // Lắng nghe kết quả tạo mới thiết bị
        espViewModel.getCreateDeviceResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null) {
                if (response.isSuccess()) {
                    Toast.makeText(getContext(), "Đang khởi tạo thiết bị...", Toast.LENGTH_SHORT).show();
                    refreshList();
                } else {
                    Toast.makeText(getContext(), "Lỗi tạo: " + response.getMessage(), Toast.LENGTH_SHORT).show();
                }
                espViewModel.resetCreateDeviceResult();
            }
        });

        // Lắng nghe kết quả cập nhật (Đổi tên)
        espViewModel.getUpdateSubDeviceResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null) {
                if (response.isSuccess()) {
                    Toast.makeText(getContext(), "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    refreshList();
                } else {
                    Toast.makeText(getContext(), "Lỗi cập nhật: " + response.getMessage(), Toast.LENGTH_SHORT).show();
                }
                espViewModel.resetUpdateSubDeviceResult();
            }
        });

        // Lắng nghe kết quả xóa thiết bị
        espViewModel.getDeleteSubDeviceResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null) {
                if (response.isSuccess()) {
                    Toast.makeText(getContext(), "Đã xóa thiết bị", Toast.LENGTH_SHORT).show();
                    refreshList();
                } else {
                    Toast.makeText(getContext(), "Lỗi xóa: " + response.getMessage(), Toast.LENGTH_SHORT).show();
                }
                espViewModel.resetDeleteSubDeviceResult();
            }
        });
    }

    private void showUpdateDeviceDialog(Device device) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_sub_device, null);

        EditText editName = dialogView.findViewById(R.id.edit_sub_device_name);
        // Ẩn các trường không cần thiết khi chỉ đổi tên
        dialogView.findViewById(R.id.spinner_device_type).setVisibility(View.GONE);
        dialogView.findViewById(R.id.edit_pin_1).setVisibility(View.GONE);
        dialogView.findViewById(R.id.edit_pin_2).setVisibility(View.GONE);

        editName.setText(device.getName());

        builder.setTitle("Đổi tên thiết bị")
                .setView(dialogView)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    String newName = editName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        espViewModel.updateSubDevice(authToken, mHomeId, device.getId(), newName, null);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDeleteConfirmDialog(Device device) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa '" + device.getName() + "'?")
                .setPositiveButton("Xóa", (dialog, which) ->
                        espViewModel.deleteSubDevice(authToken, mHomeId, device.getId()))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showAddDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_sub_device, null);

        EditText editName = dialogView.findViewById(R.id.edit_sub_device_name);
        Spinner spinnerType = dialogView.findViewById(R.id.spinner_device_type);
        EditText editPin1 = dialogView.findViewById(R.id.edit_pin_1);
        EditText editPin2 = dialogView.findViewById(R.id.edit_pin_2);

        String[] types = {"light", "fan"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item_large, types);
        spinnerType.setAdapter(spinnerAdapter);

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if ("fan".equals(types[position])) {
                    editPin2.setVisibility(View.VISIBLE);
                    editPin1.setHint("PIN 1 (Tốc độ)");
                } else {
                    editPin2.setVisibility(View.GONE);
                    editPin1.setHint("Chân PIN (GPIO)");
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        builder.setTitle("Thêm thiết bị mới")
                .setView(dialogView)
                .setPositiveButton("Tạo", (dialog, which) -> {
                    String name = editName.getText().toString().trim();
                    String type = spinnerType.getSelectedItem().toString();
                    String p1 = editPin1.getText().toString().trim();

                    if (name.isEmpty() || p1.isEmpty()) return;

                    Map<String, Object> pinConfig = new HashMap<>();
                    pinConfig.put("pin", Integer.parseInt(p1));
                    if ("fan".equals(type)) {
                        String p2 = editPin2.getText().toString().trim();
                        if (!p2.isEmpty()) pinConfig.put("pin2", Integer.parseInt(p2));
                    }

                    espViewModel.createNewDevice(authToken, mHomeId, name, type, mEspId, pinConfig);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void refreshList() {
        if (authToken != null && mHomeId != null && mEspId != null) {
            espViewModel.fetchDevicesByEsp(authToken, mHomeId, mEspId);
        }
    }
}