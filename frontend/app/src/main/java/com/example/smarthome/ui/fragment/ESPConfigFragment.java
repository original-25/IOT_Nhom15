package com.example.smarthome.ui.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.smarthome.R;
import com.example.smarthome.model.response.Esp32ProvisionResponse;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ESPConfigFragment extends Fragment {

    // Đảm bảo các UUID này khớp chính xác với code trên ESP32
    private final UUID SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc");
    private final UUID CHAR_UUID = UUID.fromString("87654321-4321-4321-4321-cba987654321");

    private Esp32ProvisionResponse mData;
    private EditText editSSID, editPassword;
    private Button btnSend;
    private ProgressBar progressBar;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice targetDevice;

    public static ESPConfigFragment newInstance(Esp32ProvisionResponse response) {
        ESPConfigFragment fragment = new ESPConfigFragment();
        Bundle args = new Bundle();
        args.putSerializable("provision_data", response);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mData = (Esp32ProvisionResponse) getArguments().getSerializable("provision_data");
        }
        BluetoothManager bluetoothManager = (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_esp_config, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editSSID = view.findViewById(R.id.edit_ssid);
        editPassword = view.findViewById(R.id.edit_password);
        btnSend = view.findViewById(R.id.btn_send_to_esp);
        progressBar = view.findViewById(R.id.progress_loading);

        btnSend.setOnClickListener(v -> startBleProvisioning());
    }

    // --- BƯỚC 1: QUÉT TÌM ESP32 ---
    private void startBleProvisioning() {
        if (!hasPermissions()) {
            Toast.makeText(getContext(), "Vui lòng cấp quyền Bluetooth và Vị trí trong cài đặt", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(getContext(), "Vui lòng bật Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        String ssid = editSSID.getText().toString().trim();
        String pass = editPassword.getText().toString().trim();

        if (ssid.isEmpty() || pass.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập thông tin Wi-Fi", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cập nhật thông tin vào object mData (Yêu cầu đã thêm Setter trong Model)
        mData.setSsid(ssid);
        mData.setPass(pass);

        progressBar.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);
        targetDevice = null;

        Toast.makeText(getContext(), "Đang tìm thiết bị ESP32...", Toast.LENGTH_SHORT).show();

        // Bắt đầu quét BLE với kiểm tra quyền rõ ràng
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            bluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
        }

        // Timeout sau 10 giây
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (targetDevice == null) {
                stopScan();
                handleError("Không tìm thấy thiết bị. Hãy chắc chắn ESP32 đang bật BLE.");
            }
        }, 10000);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            // Lọc thiết bị theo tên hiển thị
            if (device != null && device.getName() != null && device.getName().contains("ESP32")) {
                targetDevice = device;
                stopScan();
                connectToDevice(device);
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void stopScan() {
        if (bluetoothAdapter != null && bluetoothAdapter.getBluetoothLeScanner() != null) {
            bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
        }
    }

    // --- BƯỚC 2: KẾT NỐI GATT ---
    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Đã thấy thiết bị, đang kết nối...", Toast.LENGTH_SHORT).show());
        bluetoothGatt = device.connectGatt(getContext(), false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.requestMtu(517); // Request MTU lớn để gửi chuỗi JSON MQTT Token
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                handleError("Mất kết nối Bluetooth với ESP32");
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt.discoverServices();
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                sendProvisioningData(gatt);
            }
        }

        @Override
        @SuppressLint("MissingPermission")
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            getActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Toast.makeText(getContext(), "Gửi cấu hình thành công!", Toast.LENGTH_LONG).show();
                    getParentFragmentManager().popBackStack();
                } else {
                    btnSend.setEnabled(true);
                    Toast.makeText(getContext(), "Lỗi ghi dữ liệu BLE: " + status, Toast.LENGTH_SHORT).show();
                }
            });
            gatt.disconnect();
        }
    };

    // --- BƯỚC 3: GỬI DỮ LIỆU ---
    @SuppressLint("MissingPermission")
    private void sendProvisioningData(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(SERVICE_UUID);
        if (service == null) {
            handleError("Không tìm thấy Service phù hợp trên ESP32");
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHAR_UUID);
        if (characteristic != null) {
            String jsonPayload = new Gson().toJson(mData);

            characteristic.setValue(jsonPayload.getBytes(StandardCharsets.UTF_8));
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            gatt.writeCharacteristic(characteristic);
        } else {
            handleError("Không tìm thấy Characteristic ghi dữ liệu");
        }
    }

    private void handleError(String message) {
        if (isAdded()) {
            getActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                btnSend.setEnabled(true);
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }
}