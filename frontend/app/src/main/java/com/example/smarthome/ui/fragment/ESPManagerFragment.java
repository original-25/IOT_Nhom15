package com.example.smarthome.ui.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

    private ESPViewModel espViewModel;
    private RecyclerView recyclerView;
    private ESPAdapter adapter;
    private final List<Esp32Device> listDevices = new ArrayList<>();
    private final Handler pollingHandler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;
    private String pendingDeviceId;
    private ProgressDialog progressDialog;

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
        textTitle.setText(mHomeName != null ? "Quản lý thiết bị: " + mHomeName : "Quản lý thiết bị");

        // Thiết lập RecyclerView
        recyclerView = view.findViewById(R.id.recycler_devices);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Trong onViewCreated
        adapter = new ESPAdapter(listDevices, new ESPAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Esp32Device device) {
                // Logic khi click vào cả card
            }

            @Override
            public void onDetailsClick(Esp32Device device) {
                // Gọi API lấy detail và chuyển sang ESPDetailFragment
                espViewModel.fetchEsp32Detail(authToken, mHomeId, device.getId());
            }

            @Override
            public void onManageOtherDevicesClick(Esp32Device device) {
                // Chuyển sang DeviceManagerFragment
                DeviceManagerFragment fragment = DeviceManagerFragment.newInstance(mHomeId, device.getId());

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
        recyclerView.setAdapter(adapter);

        // Đăng ký Observers
        observeProvisionResult();
        observeEspDevicesList();
        observeDeviceDetail();

        // Tải danh sách thiết bị hiện có
        refreshDeviceList();

        ImageButton btnAddDevice = view.findViewById(R.id.btn_add_device);
        btnAddDevice.setOnClickListener(v -> showAddEspDialog());

        // LẮNG NGHE KẾT QUẢ TỪ CONFIG FRAGMENT
        getParentFragmentManager().setFragmentResultListener("ble_config_result", getViewLifecycleOwner(), (requestKey, bundle) -> {
            String deviceId = bundle.getString("pending_device_id");
            if (deviceId != null) {
                startPollingStatus(deviceId);
            }
        });

        // QUAN SÁT STATUS RESULT TỪ VIEWMODEL
        observeESPStatus();
    }

    private void observeDeviceDetail() {
        espViewModel.getDetailResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess() && response.getData() != null) {

                // 1. QUAN TRỌNG: Reset giá trị LiveData về null ngay lập tức
                espViewModel.resetDetailResult();

                // 2. Chuyển sang màn hình chi tiết
                ESPDetailFragment detailFragment = ESPDetailFragment.newInstance(response.getData());

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, detailFragment)
                        .addToBackStack(null)
                        .commit();
            } else if (response != null && !response.isSuccess()) {
                Toast.makeText(getContext(), "Lỗi: " + response.getMessage(), Toast.LENGTH_SHORT).show();
                espViewModel.resetDetailResult(); // Reset cả khi lỗi
            }
        });
    }
    // Hàm hiển thị Dialog cập nhật tên thiết bị
    private void showUpdateDeviceDialog(Esp32Device device) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        final EditText input = new EditText(requireContext());
        input.setText(device.getName()); // Hiện tên cũ

        FrameLayout container = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 60; params.rightMargin = 60;
        input.setLayoutParams(params);
        container.addView(input);

        builder.setTitle("Cập nhật thông tin")
                .setView(container)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        // Gọi ViewModel xử lý Update (Bạn cần có hàm này trong ViewModel)
                        // espViewModel.updateEspDevice(authToken, mHomeId, device.getId(), newName);
                        Toast.makeText(getContext(), "Đang cập nhật...", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // Hàm hiển thị Dialog xác nhận xóa thiết bị
    private void showDeleteConfirmDialog(Esp32Device device) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa thiết bị")
                .setMessage("Bạn có chắc chắn muốn xóa " + device.getName() + "?\nHành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // Gọi ViewModel xử lý Delete (Bạn cần có hàm này trong ViewModel)
                    // espViewModel.deleteEspDevice(authToken, mHomeId, device.getId());
                    Toast.makeText(getContext(), "Đang xóa thiết bị...", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    // Luồng xử lý sau khi Server tạo thiết bị thành công
    private void observeProvisionResult() {
        espViewModel.getProvisionResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess()) {
                BleScanFragment scanFragment = BleScanFragment.newInstance(response);

                // QUAN TRỌNG: Reset giá trị ngay sau khi lấy được dữ liệu thành công
                // Điều này ngăn chặn việc quay lại fragment này khi nhấn nút Back
                espViewModel.resetProvisionResult();

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, scanFragment)
                        .addToBackStack(null)
                        .commit();
            } else if (response != null) {
                Toast.makeText(getContext(), response.getMessage(), Toast.LENGTH_SHORT).show();
                // Reset cả khi lỗi để không hiện lại Toast khi xoay màn hình
                espViewModel.resetProvisionResult();
            }
        });
    }

    private void observeEspDevicesList() {
        espViewModel.getEspDevicesResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess() && response.getData() != null) {
                adapter.updateData(response.getData());
            }
        });
    }

    private void showAddEspDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        final EditText input = new EditText(getContext());
        input.setHint("Tên thiết bị (VD: ESP32 Tầng 1)");

        FrameLayout container = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 60;
        params.rightMargin = 60;
        input.setLayoutParams(params);
        container.addView(input);

        builder.setTitle("Thêm thiết bị mới")
                .setView(container)
                .setPositiveButton("Tiếp theo", (dialog, which) -> {
                    String espName = input.getText().toString().trim();
                    if (!espName.isEmpty()) {
                        // Gọi API tạo thiết bị trên Server trước
                        espViewModel.provisionEsp32(authToken, mHomeId, espName);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void startPollingStatus(String deviceId) {
        this.pendingDeviceId = deviceId;
        showLoadingDialog("Đang đợi thiết bị trực tuyến...");

        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (pendingDeviceId != null) {
                    // Gọi hàm check status trong ViewModel
                    espViewModel.checkESPStatus(authToken, mHomeId, pendingDeviceId);
                    // Lặp lại sau mỗi 3 giây
                    pollingHandler.postDelayed(this, 3000);
                }
            }
        };
        pollingHandler.post(pollingRunnable);
    }

    private void observeESPStatus() {
        espViewModel.getStatusResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess() && response.getData() != null) {
                String status = response.getData().getStatus();

                // Nếu thiết bị đã chuyển sang trạng thái provisioned (Online)
                if ("provisioned".equals(status)) {
                    stopPolling();
                    Toast.makeText(getContext(), "Thiết bị đã trực tuyến!", Toast.LENGTH_SHORT).show();
                    // Làm mới danh sách để thiết bị hiện ra
                    refreshDeviceList();
                }
            }
        });
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
        progressDialog.show();
    }



    private void refreshDeviceList() {
        if (!authToken.isEmpty()) {
            espViewModel.fetchAllEspDevices(authToken, mHomeId);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Cập nhật lại danh sách mỗi khi quay lại màn hình này (ví dụ sau khi cấu hình xong)
        refreshDeviceList();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPolling(); // Quan trọng: Ngừng polling khi thoát Fragment để tránh rò rỉ bộ nhớ
    }
}