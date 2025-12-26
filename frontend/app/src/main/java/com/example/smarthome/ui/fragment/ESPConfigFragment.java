package com.example.smarthome.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.example.smarthome.viewmodel.ESPViewModel;
import okhttp3.*;
import java.io.IOException;

public class ESPConfigFragment extends Fragment {

    private String mClaimToken, mEspDeviceId;
    private EditText editSSID, editPassword;
    private Button btnSend;
    private ProgressBar progressBar;
    private ESPViewModel espViewModel;

    private Handler pollingHandler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;
    private int pollingCount = 0;
    private static final int MAX_POLLING_ATTEMPTS = 20;

    public static ESPConfigFragment newInstance(String claimToken, String espDeviceId) {
        ESPConfigFragment fragment = new ESPConfigFragment();
        Bundle args = new Bundle();
        args.putString("claim_token", claimToken);
        args.putString("device_id", espDeviceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mClaimToken = getArguments().getString("claim_token");
            mEspDeviceId = getArguments().getString("device_id");
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

            sendDataToEspLocal(ssid, pass);
        });
    }

    private void sendDataToEspLocal(String ssid, String pass) {
        progressBar.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);

        // Tạo Body gửi cho ESP32 (Tùy thuộc vào code ESP32 của bạn nhận gì)
        // Thông thường gửi qua Query Parameters hoặc JSON Body
        OkHttpClient client = new OkHttpClient();

        // Giả sử ESP32 nhận qua endpoint /config
        String url = "http://192.168.4.1/config?ssid=" + ssid + "&pass=" + pass + "&token=" + mClaimToken + "&id=" + mEspDeviceId;

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSend.setEnabled(true);
                    Toast.makeText(getContext(), "Không thể kết nối tới ESP. Hãy kiểm tra Wi-Fi thiết bị!", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Gửi Wi-Fi thành công! Đang quay lại màn hình quản lý.", Toast.LENGTH_SHORT).show();

                        getParentFragmentManager().popBackStack();
                    });
                }
            }
        });
    }

}