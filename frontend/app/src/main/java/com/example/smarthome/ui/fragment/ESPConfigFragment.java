package com.example.smarthome.ui.fragment;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;

import com.example.smarthome.R;
import com.example.smarthome.model.response.Esp32ProvisionResponse;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ESPConfigFragment extends Fragment {

    // UUID Characteristic dùng chung để ghi dữ liệu
    private final UUID CHAR_UUID = UUID.fromString("87654321-4321-4321-4321-cba987654321");

    private Esp32ProvisionResponse mData;
    private BluetoothDevice targetDevice;
    private UUID dynamicServiceUuid;

    private EditText editSSID, editPassword;
    private Button btnSend;
    private ProgressBar progressBar;
    private BluetoothGatt bluetoothGatt;

    // Sửa newInstance để nhận thêm Device và Service UUID động
    public static ESPConfigFragment newInstance(Esp32ProvisionResponse response, BluetoothDevice device, String serviceUuid) {
        ESPConfigFragment fragment = new ESPConfigFragment();
        Bundle args = new Bundle();
        args.putSerializable("provision_data", response);
        args.putParcelable("selected_device", device);
        args.putString("dynamic_service_uuid", serviceUuid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mData = (Esp32ProvisionResponse) getArguments().getSerializable("provision_data");
            targetDevice = getArguments().getParcelable("selected_device");
            dynamicServiceUuid = UUID.fromString(getArguments().getString("dynamic_service_uuid"));
        }
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

        btnSend.setOnClickListener(v -> {
            String ssid = editSSID.getText().toString().trim();
            String pass = editPassword.getText().toString().trim();

            if (ssid.isEmpty() || pass.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin WiFi", Toast.LENGTH_SHORT).show();
                return;
            }

            // Gán dữ liệu vào mData
            mData.setSsid(ssid);
            mData.setPass(pass);

            // In log để kiểm tra gói tin JSON
            String debugJson = new Gson().toJson(mData);
            Log.d("DEBUG_PAYLOAD", "Gói tin chuẩn bị gửi: " + debugJson);

            // Bắt đầu kết nối trực tiếp vì đã có Device từ màn hình Scan
            connectToDevice();
        });
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice() {
        if (targetDevice == null) {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy thông tin thiết bị!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);

        Toast.makeText(getContext(), "Đang kết nối tới " + targetDevice.getName() + "...", Toast.LENGTH_SHORT).show();

        // Kết nối GATT
        bluetoothGatt = targetDevice.connectGatt(getContext(), false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BLE_CONFIG", "Connected! Requesting MTU 517...");
                gatt.requestMtu(517); // Bắt buộc xin MTU lớn trước khi tìm service
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                handleError("Mất kết nối Bluetooth với ESP32");
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE_CONFIG", "MTU set to " + mtu + ". Discovering services...");
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

        // Trong ESPConfigFragment.java, tại hàm onCharacteristicWrite:
        @Override
        @SuppressLint("MissingPermission")
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            getActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Toast.makeText(getContext(), "Gửi cấu hình thành công! Đang đợi thiết bị kết nối...", Toast.LENGTH_LONG).show();

                    // Gửi kết quả về ESPManagerFragment
                    Bundle result = new Bundle();
                    result.putString("pending_device_id", mData.getEspDeviceId()); // ID của ESP vừa cấu hình
                    getParentFragmentManager().setFragmentResult("ble_config_result", result);

                    // Quay lại màn hình quản lý
                    getParentFragmentManager().popBackStack("manager_fragment", 0);
                } else {
                    btnSend.setEnabled(true);
                    Toast.makeText(getContext(), "Lỗi ghi dữ liệu BLE: " + status, Toast.LENGTH_SHORT).show();
                }
            });
            gatt.disconnect();
        }
    };

    @SuppressLint("MissingPermission")
    private void sendProvisioningData(BluetoothGatt gatt) {
        // Dùng Service UUID động nhận được từ ScanFragment
        BluetoothGattService service = gatt.getService(dynamicServiceUuid);
        if (service == null) {
            handleError("Không tìm thấy Service tương ứng trên ESP32");
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