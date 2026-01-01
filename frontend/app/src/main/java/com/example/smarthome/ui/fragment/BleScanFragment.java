package com.example.smarthome.ui.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthome.R;
import com.example.smarthome.model.response.Esp32ProvisionResponse;
import com.example.smarthome.ui.adapter.BleDeviceAdapter;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BleScanFragment extends Fragment {
    private Esp32ProvisionResponse mProvisionData;
    private BluetoothAdapter bluetoothAdapter;
    private final List<BluetoothDevice> deviceList = new ArrayList<>();
    private final Map<String, String> deviceUuids = new HashMap<>();

    private BleDeviceAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isScanning = false;

    public static BleScanFragment newInstance(Esp32ProvisionResponse response) {
        BleScanFragment fragment = new BleScanFragment();
        Bundle args = new Bundle();
        args.putSerializable("data", response);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mProvisionData = (Esp32ProvisionResponse) getArguments().getSerializable("data");
        }
        BluetoothManager bluetoothManager = (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_ble_scan, container, false);

        progressBar = v.findViewById(R.id.progress_scan);
        tvStatus = v.findViewById(R.id.tv_scan_status);
        RecyclerView rv = v.findViewById(R.id.rv_ble_devices);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BleDeviceAdapter(deviceList, device -> {
            stopScan();
            String selectedUuid = deviceUuids.get(device.getAddress());
            goToConfig(device, selectedUuid);
        });
        rv.setAdapter(adapter);

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        startScan(); // Bắt đầu quét ngay khi view đã sẵn sàng
    }

    @SuppressLint("MissingPermission")
    private void startScan() {
        if (isScanning) return;

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(getContext(), "Vui lòng bật Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra quyền runtime một lần nữa cho chắc chắn
        if (!hasLocationPermission()) {
            Toast.makeText(getContext(), "Thiếu quyền truy cập vị trí/Bluetooth để quét", Toast.LENGTH_SHORT).show();
            return;
        }

        deviceList.clear();
        deviceUuids.clear();
        adapter.notifyDataSetChanged();

        isScanning = true;
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("Đang tìm kiếm thiết bị SmartHome...");

        bluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);

        // Tự động dừng sau 10 giây để tiết kiệm pin
        handler.postDelayed(this::stopScan, 10000);
    }

    @SuppressLint("MissingPermission")
    private void stopScan() {
        if (!isScanning) return;

        isScanning = false;
        if (bluetoothAdapter != null && bluetoothAdapter.getBluetoothLeScanner() != null) {
            bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
        }

        handler.post(() -> {
            if (isAdded()) {
                progressBar.setVisibility(View.GONE);
                if (deviceList.isEmpty()) {
                    tvStatus.setText("Không tìm thấy thiết bị nào. Hãy đảm bảo ESP32 đang ở chế độ Config.");
                } else {
                    tvStatus.setText("Chọn thiết bị để tiếp tục:");
                }
            }
        });
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device != null && device.getName() != null && device.getName().contains("ESP32")) {
                if (!deviceUuids.containsKey(device.getAddress())) {
                    List<ParcelUuid> uuids = result.getScanRecord().getServiceUuids();

                    // Lấy UUID động từ firmware quảng bá, nếu không thấy dùng mặc định của bạn
                    String serviceUuidStr = "12345678-1234-1234-1234-123456789abc";
                    if (uuids != null && !uuids.isEmpty()) {
                        serviceUuidStr = uuids.get(0).getUuid().toString();
                    }

                    deviceUuids.put(device.getAddress(), serviceUuidStr);
                    deviceList.add(device);

                    handler.post(() -> adapter.notifyDataSetChanged());
                }
            }
        }
    };

    private void goToConfig(BluetoothDevice device, String serviceUuid) {
        ESPConfigFragment configFragment = ESPConfigFragment.newInstance(mProvisionData, device, serviceUuid);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, configFragment)
                .addToBackStack(null)
                .commit();
    }

    private boolean hasLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        }
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopScan(); // Đảm bảo dừng quét khi rời khỏi fragment
        handler.removeCallbacksAndMessages(null);
    }
}