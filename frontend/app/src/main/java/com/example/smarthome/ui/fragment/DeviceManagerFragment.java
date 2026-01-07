package com.example.smarthome.ui.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
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

    // Lưu trữ ID đang xử lý để cập nhật đúng item khi nhận Logs hoặc Trạng thái
    private String currentProcessingDeviceId = "";
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;

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

        // Ánh xạ View mới
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);

        // Thiết lập màu sắc cho vòng xoay loading (tùy chọn)
        swipeRefreshLayout.setColorSchemeResources(R.color.purple_500, android.R.color.holo_blue_bright);

        // Sự kiện khi người dùng vuốt xuống
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshList(); // Gọi hàm lấy lại danh sách thiết bị
        });

        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        authToken = prefs.getString("authToken", "");

        espViewModel = new ViewModelProvider(this).get(ESPViewModel.class);

        // 1. Cấu hình RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.rv_sub_devices);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new DeviceAdapter(deviceList, new DeviceAdapter.OnDeviceClickListener() {
            @Override
            public void onDeviceClick(Device device) { }

            @Override
            public void onPowerChange(Device device, boolean isOn) {
                currentProcessingDeviceId = device.getId();
                Map<String, Object> tempState = new HashMap<>();
                tempState.put("value", isOn ? 1 : 0);
                adapter.updateSingleDeviceState(device.getId(), tempState);
                espViewModel.controlDevice(authToken, mHomeId, device.getId(), isOn);
            }

            @Override
            public void onUpdateSubDevice(Device device) {
                showUpdateDeviceDialog(device);
            }

            @Override
            public void onAdvancedSettings(Device device) {
                // Chuyển sang ScheduleManagerFragment
                ScheduleManagerFragment scheduleFragment = ScheduleManagerFragment.newInstance(
                        mHomeId,
                        device.getId(),
                        device.getName()
                );

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, scheduleFragment) // R.id.fragment_container là ID của nơi chứa Fragment trong Activity của bạn
                        .addToBackStack(null) // Cho phép nhấn nút Back để quay lại danh sách thiết bị
                        .commit();
            }

            @Override
            public void onDeleteSubDevice(Device device) {
                showDeleteConfirmDialog(device);
            }

            @Override
            public void onShowStatistics(Device device) {
                DeviceChartFragment chartFragment = DeviceChartFragment.newInstance(mHomeId, device.getId());

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, chartFragment) // Thay thế bằng ID container của bạn
                        .addToBackStack(null) // Để nhấn nút Back quay lại được danh sách
                        .commit();
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
        // Lắng nghe danh sách thiết bị con từ ESP
        espViewModel.getSubDevicesResult().observe(getViewLifecycleOwner(), response -> {
            // TẮT HIỆU ỨNG XOAY KHI CÓ KẾT QUẢ (Dù thành công hay thất bại)
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            if (response != null && response.isSuccess()) {
                List<Device> data = response.getData();
                if (data != null) {
                    deviceList.clear();
                    deviceList.addAll(data);
                    adapter.notifyDataSetChanged();

                    // MỚI: Sau khi có danh sách, quét tìm sensor để lấy log mới nhất
                    for (Device d : data) {
                        if ("sensor".equalsIgnoreCase(d.getType())) {
                            // Gọi lấy 1 log gần nhất cho mỗi sensor
                            espViewModel.fetchLatestLogs(authToken, mHomeId, d.getId());
                        }
                    }
                }
            }
        });

        espViewModel.getDeviceLogsResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess() && response.getData() != null) {
                if (!response.getData().isEmpty()) {
                    Map<String, Object> latestLog = response.getData().get(0);

                    // Xử lý lấy ID từ trường "device" (có thể là Map hoặc String)
                    String logDeviceId = "";
                    Object deviceObj = latestLog.get("device");

                    if (deviceObj instanceof Map) {
                        // Nếu GSON parse $oid thành một Map
                        logDeviceId = String.valueOf(((Map<?, ?>) deviceObj).get("$oid"));
                    } else {
                        logDeviceId = String.valueOf(deviceObj);
                    }

                    if (logDeviceId != null && !logDeviceId.isEmpty()) {
                        adapter.updateSensorFromLog(logDeviceId, latestLog);
                    }
                }
                espViewModel.resetDeviceLogsResult();
            }
        });

        // Lắng nghe kết quả điều khiển (Bật/Tắt)
        espViewModel.getCommandResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null) {
                if (response.isSuccess()) {
                    espViewModel.fetchDeviceState(authToken, mHomeId, currentProcessingDeviceId);
                } else {
                    Toast.makeText(getContext(), "Lỗi: " + response.getMessage(), Toast.LENGTH_SHORT).show();
                }
                espViewModel.resetCommandResult();
            }
        });

        // Lắng nghe trạng thái thiết bị đơn lẻ (công tắc)
        espViewModel.getStateResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess()) {
                if (response.getData() != null && response.getData().getLastState() != null) {
                    adapter.updateSingleDeviceState(currentProcessingDeviceId, response.getData().getLastState());
                }
                espViewModel.resetStateResult();
            }
        });

        // Lắng nghe kết quả tạo/xóa/cập nhật (Tái sử dụng refreshList)
        espViewModel.getCreateDeviceResult().observe(getViewLifecycleOwner(), r -> handleGenericResponse(r, "Đang khởi tạo..."));
        espViewModel.getUpdateSubDeviceResult().observe(getViewLifecycleOwner(), r -> handleGenericResponse(r, "Đã cập nhật"));
        espViewModel.getDeleteSubDeviceResult().observe(getViewLifecycleOwner(), r -> handleGenericResponse(r, "Đã xóa"));
    }

    private void handleGenericResponse(com.example.smarthome.model.response.HomeResponse<?> response, String successMsg) {
        if (response != null) {
            if (response.isSuccess()) {
                Toast.makeText(getContext(), successMsg, Toast.LENGTH_SHORT).show();
                refreshList();
            } else {
                Toast.makeText(getContext(), "Lỗi: " + response.getMessage(), Toast.LENGTH_SHORT).show();
            }
            // Reset các LiveData tương ứng trong ViewModel nếu cần
        }
    }

    private void showAddDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_sub_device, null);

        EditText editName = dialogView.findViewById(R.id.edit_sub_device_name);
        Spinner spinnerType = dialogView.findViewById(R.id.spinner_device_type);
        EditText editPin1 = dialogView.findViewById(R.id.edit_pin_1);
        EditText editPin2 = dialogView.findViewById(R.id.edit_pin_2);

        // MỚI: Thêm "sensor" vào danh sách loại thiết bị
        String[] types = {"light", "fan", "sensor"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, types);
        spinnerType.setAdapter(spinnerAdapter);

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = types[position];
                if ("fan".equals(selected)) {
                    editPin2.setVisibility(View.VISIBLE);
                    editPin1.setHint("PIN 1 (Speed)");
                } else if ("sensor".equals(selected)) {
                    editPin2.setVisibility(View.GONE);
                    editPin1.setHint("Chân PIN (DHT Data)");
                } else {
                    editPin2.setVisibility(View.GONE);
                    editPin1.setHint("Chân PIN (Relay)");
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
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

    private void showUpdateDeviceDialog(Device device) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        // Sử dụng chung layout với dialog thêm thiết bị
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_sub_device, null);

        EditText editName = dialogView.findViewById(R.id.edit_sub_device_name);
        Spinner spinnerType = dialogView.findViewById(R.id.spinner_device_type);
        EditText editPin1 = dialogView.findViewById(R.id.edit_pin_1);
        EditText editPin2 = dialogView.findViewById(R.id.edit_pin_2);

        // 1. Điền thông tin cũ
        editName.setText(device.getName());
        spinnerType.setVisibility(View.GONE); // Thường không nên cho đổi loại thiết bị (Light sang Fan) khi update

        // 2. Trích xuất PIN cũ từ trường config (Mixed/Object)
        Map<String, Object> pinMap = null;
        if (device.getConfig() instanceof Map) {
            pinMap = (Map<String, Object>) device.getConfig();
        }

        // 3. Logic hiển thị theo loại (Type)
        String type = device.getType().toLowerCase();

        if ("fan".equals(type)) {
            editPin1.setVisibility(View.VISIBLE);
            editPin2.setVisibility(View.VISIBLE);
            editPin1.setHint("PIN 1 (Speed)");
            editPin2.setHint("PIN 2 (Direction)");

            if (pinMap != null) {
                if (pinMap.get("pin") != null) editPin1.setText(formatPinValue(pinMap.get("pin")));
                if (pinMap.get("pin2") != null) editPin2.setText(formatPinValue(pinMap.get("pin2")));
            }
        } else {
            // Light hoặc Sensor
            editPin1.setVisibility(View.VISIBLE);
            editPin2.setVisibility(View.GONE); // Ẩn pin2 cho Light/Sensor
            editPin1.setHint(type.equals("sensor") ? "Chân PIN (DHT Data)" : "Chân PIN (Relay)");

            if (pinMap != null && pinMap.get("pin") != null) {
                editPin1.setText(formatPinValue(pinMap.get("pin")));
            }
        }

        builder.setTitle("Cập nhật thiết bị")
                .setView(dialogView)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    String newName = editName.getText().toString().trim();
                    String p1 = editPin1.getText().toString().trim();
                    String p2 = editPin2.getText().toString().trim();

                    if (newName.isEmpty() || p1.isEmpty()) {
                        Toast.makeText(getContext(), "Vui lòng nhập đủ tên và mã PIN", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Đóng gói cấu hình PIN mới
                    Map<String, Object> newPinConfig = new HashMap<>();
                    try {
                        newPinConfig.put("pin", Integer.parseInt(p1));
                        if ("fan".equals(type) && !p2.isEmpty()) {
                            newPinConfig.put("pin2", Integer.parseInt(p2));
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "PIN phải là số", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Gửi yêu cầu cập nhật lên ViewModel
                    // Đảm bảo ViewModel của bạn có hàm nhận (token, homeId, deviceId, name, config)
                    espViewModel.updateSubDevice(authToken, mHomeId, device.getId(), newName, newPinConfig);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // Hàm bổ trợ để chuyển Double thành String số nguyên (ví dụ 5.0 -> "5")
    String formatPinValue(Object value) {
        if (value instanceof Number) {
            // Ép kiểu về số nguyên để loại bỏ phần thập phân .0
            return String.valueOf(((Number) value).intValue());
        }
        return value != null ? value.toString() : "";
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
    private void refreshList() {
        if (authToken != null && mHomeId != null && mEspId != null) {
            espViewModel.fetchDevicesByEsp(authToken, mHomeId, mEspId);
        }
    }
}