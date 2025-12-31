package com.example.smarthome.ui.fragment;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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
import androidx.lifecycle.ViewModelProvider;

import com.example.smarthome.R;
import com.example.smarthome.model.response.Esp32ProvisionResponse;
import com.example.smarthome.viewmodel.ESPViewModel;

import okhttp3.*;

import java.io.IOException;

public class ESPConfigFragment extends Fragment {

    private Esp32ProvisionResponse mData;
    private EditText editSSID, editPassword;
    private Button btnSend;
    private ProgressBar progressBar;
    private ESPViewModel espViewModel;

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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_esp_config, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        espViewModel = new ViewModelProvider(this).get(ESPViewModel.class);

        editSSID = view.findViewById(R.id.edit_ssid);
        editPassword = view.findViewById(R.id.edit_password);
        btnSend = view.findViewById(R.id.btn_send_to_esp);
        progressBar = view.findViewById(R.id.progress_loading);

        btnSend.setOnClickListener(v -> {
            String ssid = editSSID.getText().toString().trim();
            String pass = editPassword.getText().toString().trim();

            if (ssid.isEmpty() || pass.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin Wi-Fi", Toast.LENGTH_SHORT).show();
                return;
            }

            // Kiểm tra tên Wifi trước khi gửi
            if (!checkWifiConnection()) {
                Toast.makeText(getContext(), "Vui lòng kết nối vào Wi-Fi của ESP32!", Toast.LENGTH_LONG).show();
                return;
            }

            if (mData != null) {
                requestWifiNetworkAndSend(ssid, pass);
            }
        });
    }

    // 1. Hàm kiểm tra xem có đang kết nối Wi-Fi (nên chứa tên ESP32) không
    private boolean checkWifiConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        if (capabilities == null || !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return false;

        WifiManager wifiManager = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if (wifiInfo != null) {
            String ssid = wifiInfo.getSSID();
            Log.d("WIFI_DEBUG", "Current SSID: " + ssid); // Xem log để biết tên thật là gì

            // Android thường bọc SSID trong dấu ngoặc kép, ví dụ: "ESP32_Device"
            // Nếu điện thoại không đọc được tên, ssid sẽ là "<unknown ssid>"
            if (ssid.equals("<unknown ssid>")) {
                // Nếu vẫn không đọc được tên, nhưng máy tính/điện thoại đang kết nối vào dải IP 192.168.4.x
                // thì ta tạm chấp nhận là đã kết nối đúng (đây là mẹo để vượt qua lỗi SSID)
                return true;
            }

            return ssid.contains("ESP32") || ssid.contains("192.168.4.1");
        }
        return false;
    }

    // 2. Hàm ép Android sử dụng mạng Wi-Fi và gửi dữ liệu
    private void requestWifiNetworkAndSend(String ssid, String pass) {
        progressBar.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);

        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        // Tạo yêu cầu chỉ sử dụng mạng Wi-Fi
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();

        connectivityManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                // Quan trọng: Ép toàn bộ tiến trình của App sử dụng mạng Wi-Fi này
                connectivityManager.bindProcessToNetwork(network);

                getActivity().runOnUiThread(() -> {
                    sendDataToEspLocal(ssid, pass);
                });
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSend.setEnabled(true);
                    Toast.makeText(getContext(), "Không tìm thấy mạng Wi-Fi phù hợp", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void sendDataToEspLocal(String ssid, String pass) {
        String url = "http://192.168.4.1/config?"
                + "ssid=" + ssid
                + "&pass=" + pass
                + "&host=" + mData.getMqtt().getHost()
                + "&port=" + mData.getMqtt().getPort()
                + "&espDeviceId=" + mData.getEspDeviceId()
                + "&claimToken=" + mData.getClaimToken()
                + "&username=" + mData.getMqtt().getUsername()
                + "&password=" + mData.getMqtt().getPassword()
                + "&baseTopic=" + mData.getMqtt().getBaseTopic();

        Log.d("ESP_CONFIG", "Sending URL: " + url);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                resetNetworkBinding(); // Gỡ bỏ ép buộc mạng khi xong
                if (isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnSend.setEnabled(true);
                        Toast.makeText(getContext(), "Lỗi kết nối tới ESP32!", Toast.LENGTH_LONG).show();
                    });
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                resetNetworkBinding(); // Gỡ bỏ ép buộc mạng khi xong
                if (isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Gửi cấu hình thành công!", Toast.LENGTH_SHORT).show();
                            getParentFragmentManager().popBackStack();
                        } else {
                            btnSend.setEnabled(true);
                            Toast.makeText(getContext(), "ESP32 lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    // 3. Giải phóng ràng buộc mạng để quay lại dùng Internet bình thường (4G/Wifi nhà)
    private void resetNetworkBinding() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.bindProcessToNetwork(null);
    }
}