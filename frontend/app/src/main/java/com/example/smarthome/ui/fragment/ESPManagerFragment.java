package com.example.smarthome.ui.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import com.example.smarthome.model.response.Esp32Device;
import com.example.smarthome.ui.adapter.ESPAdapter;
import com.example.smarthome.viewmodel.ESPViewModel;

import java.util.ArrayList;
import java.util.List;

public class ESPManagerFragment extends Fragment {

    private static final String ARG_HOME_NAME = "home_name";
    private static final String ARG_HOME_ID = "home_id";

    private String mHomeId;
    private String mHomeName;
    private String authToken;

    private final Handler pollingHandler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;
    private String pendingDeviceId;
    private ProgressDialog progressDialog;

    private ESPViewModel espViewModel;
    private RecyclerView recyclerView;
    private ESPAdapter adapter;
    private final List<Esp32Device> listDevices = new ArrayList<>();

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

        espViewModel = new ViewModelProvider(this).get(ESPViewModel.class);

        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        authToken = prefs.getString("authToken", "");

        TextView textTitle = view.findViewById(R.id.text_title_manager);
        textTitle.setText(mHomeName != null ? "Bộ quản lý thiết bị của " + mHomeName : "Bộ quản lý thiết bị");

        // Thiết lập RecyclerView
        recyclerView = view.findViewById(R.id.recycler_devices);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Khởi tạo Adapter với list trống ban đầu
        adapter = new ESPAdapter(listDevices, device -> {
            if (!"provisioned".equals(device.getStatus())) {
                // Nếu click vào thiết bị chưa cấu hình, có thể mở lại trang hướng dẫn
                Toast.makeText(getContext(), "Thiết bị chưa được cấu hình Wi-Fi", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(adapter);

        // Đăng ký các Observer
        observeProvisionResult();
        observeESPStatus();
        observeEspDevicesList();

        // Tải danh sách lần đầu
//        if (!authToken.isEmpty()) {
//            espViewModel.fetchAllEspDevices(authToken, mHomeId);
//        }

        ImageButton btnAddDevice = view.findViewById(R.id.btn_add_device);
        btnAddDevice.setOnClickListener(v -> showAddEspDialog());
    }

    private void observeProvisionResult() {
        espViewModel.getProvisionResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess()) {
                String idCuaESP = response.getEspDeviceId();
                String tokenClaim = response.getClaimToken();
                Log.d("ESP_DEBUG", "ID: " + idCuaESP + " | Token: " + tokenClaim);
                // 1. Kích hoạt Polling ngay lập tức
                startPolling(response.getEspDeviceId());

                // 2. Chuyển sang Fragment hướng dẫn
                ESPConfigFragment configFragment = ESPConfigFragment.newInstance(
                        response.getClaimToken(),
                        response.getEspDeviceId()
                );

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, configFragment)
                        .addToBackStack(null)
                        .commit();
            } else if (response != null) {
                Toast.makeText(getContext(), response.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void observeESPStatus() {
        espViewModel.getStatusResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess() && response.getData() != null) {
                String status = response.getData().getStatus();

                if ("provisioned".equals(status)) {
                    stopPolling();
                    pendingDeviceId = null;
                    Toast.makeText(getContext(), "Thiết bị đã kết nối thành công!", Toast.LENGTH_LONG).show();

                    // Cập nhật lại danh sách thiết bị trên màn hình
                    espViewModel.fetchAllEspDevices(authToken, mHomeId);
                } else {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.setMessage("Đang đợi thiết bị trực tuyến...\nVui lòng đảm bảo bạn đã quay về WiFi nhà.");
                    }
                }
            }
        });
    }

    private void observeEspDevicesList() {
        espViewModel.getEspDevicesResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess() && response.getData() != null) {
                // Sử dụng phương thức updateData của Adapter để làm mới danh sách
                adapter.updateData(response.getData());
            } else if (response != null) {
                Toast.makeText(getContext(), "Không thể tải danh sách: " + response.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddEspDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        final EditText input = new EditText(getContext());
        input.setHint("Ví dụ: ESP32 Phòng Khách");

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
                        espViewModel.provisionEsp32(authToken, mHomeId, espName);
                    } else {
                        Toast.makeText(getContext(), "Tên không được để trống", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void startPolling(String deviceId) {
        pendingDeviceId = deviceId;
        showLoadingDialog("Đang khởi tạo cấu hình...");

        // Xóa các callback cũ nếu có để tránh chạy song song nhiều polling
        pollingHandler.removeCallbacks(pollingRunnable);

        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (pendingDeviceId != null) {
                    // Gọi hàm check status trong ViewModel
                    espViewModel.checkESPStatus(authToken, mHomeId, pendingDeviceId);
                    pollingHandler.postDelayed(this, 3000); // Lặp lại sau 3s
                }
            }
        };
        pollingHandler.post(pollingRunnable);
    }

    private void stopPolling() {
        pendingDeviceId = null;
        if (pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showLoadingDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPolling();
    }
}