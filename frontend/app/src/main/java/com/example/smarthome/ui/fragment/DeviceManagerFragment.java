package com.example.smarthome.ui.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
import java.util.List;

public class DeviceManagerFragment extends Fragment {
    private static final String ARG_HOME_ID = "home_id";
    private static final String ARG_ESP_ID = "esp_id";

    private String mHomeId, mEspId, authToken;
    private ESPViewModel espViewModel;
    private DeviceAdapter adapter;
    private List<Device> deviceList = new ArrayList<>();

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

        // Thiết lập RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.rv_sub_devices);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DeviceAdapter(deviceList, device -> {
            // Xử lý khi người dùng nhấn vào một thiết bị con (ví dụ: bật/tắt đèn)
            Toast.makeText(getContext(), "Bạn chọn: " + device.getName(), Toast.LENGTH_SHORT).show();
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setAdapter(adapter);

        // Nút thêm thiết bị mới
        ImageButton btnAdd = view.findViewById(R.id.btn_add_sub_device);
        btnAdd.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Chức năng thêm thiết bị đang phát triển", Toast.LENGTH_SHORT).show();
        });

        // Quan sát kết quả từ API
        espViewModel.getSubDevicesResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess()) {
                deviceList.clear();
                deviceList.addAll(response.getData());
                adapter.notifyDataSetChanged();
            } else if (response != null) {
                Toast.makeText(getContext(), response.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Gọi API lấy dữ liệu
        espViewModel.fetchDevicesByEsp(authToken, mHomeId, mEspId);
    }
}