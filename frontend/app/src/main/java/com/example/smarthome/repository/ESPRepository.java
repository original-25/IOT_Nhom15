package com.example.smarthome.repository;

import androidx.lifecycle.MutableLiveData;

import com.example.smarthome.model.response.Device;
import com.example.smarthome.model.response.Esp32Device;
import com.example.smarthome.model.request.ProvisionESPRequest;
import com.example.smarthome.model.response.Esp32ProvisionResponse;
import com.example.smarthome.model.response.HomeResponse;
import com.example.smarthome.network.ApiService;
import com.example.smarthome.network.RetrofitClient;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ESPRepository {
    private final ApiService apiService;
    private final Gson gson;

    public ESPRepository() {
        this.apiService = RetrofitClient.getApiService();
        this.gson = new Gson();
    }

    public void provisionEsp32(String token, String homeId, String deviceName, MutableLiveData<Esp32ProvisionResponse> result) {
        ProvisionESPRequest request = new ProvisionESPRequest(deviceName); // Gửi { "name": name }

        apiService.provisionEsp32("Bearer " + token, homeId, request).enqueue(new Callback<Esp32ProvisionResponse>() {
            @Override
            public void onResponse(Call<Esp32ProvisionResponse> call, Response<Esp32ProvisionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(response.body()); // Trả về success, espDeviceId, claimToken
                } else {
                    handleProvisionError(response, result);
                }
            }

            @Override
            public void onFailure(Call<Esp32ProvisionResponse> call, Throwable t) {
                Esp32ProvisionResponse failure = new Esp32ProvisionResponse();
                failure.setSuccess(false);
                failure.setMessage("Lỗi kết nối Server: " + t.getMessage());
                result.postValue(failure);
            }
        });
    }

    public void getAllEsp32InHome(String token, String homeId, MutableLiveData<HomeResponse<List<Esp32Device>>> result) {
        apiService.getAllEsp32InHome("Bearer " + token, homeId).enqueue(new Callback<HomeResponse<List<Esp32Device>>>() {
            @Override
            public void onResponse(Call<HomeResponse<List<Esp32Device>>> call, Response<HomeResponse<List<Esp32Device>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(response.body());
                } else {
                    handleGenericError(response, result);
                }
            }

            @Override
            public void onFailure(Call<HomeResponse<List<Esp32Device>>> call, Throwable t) {
                HomeResponse<List<Esp32Device>> failure = new HomeResponse<>();
                failure.setSuccess(false);
                failure.setMessage("Lỗi kết nối danh sách thiết bị");
                result.postValue(failure);
            }
        });
    }
    public void getEsp32Status(String token, String homeId, String deviceId, MutableLiveData<HomeResponse<HomeResponse.DeviceStatus>> result) {
        apiService.getEsp32Status("Bearer " + token, homeId, deviceId).enqueue(new Callback<HomeResponse<HomeResponse.DeviceStatus>>() {
            @Override
            public void onResponse(Call<HomeResponse<HomeResponse.DeviceStatus>> call, Response<HomeResponse<HomeResponse.DeviceStatus>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(response.body()); // Trả về id, name, status ("unclaimed" hoặc "provisioned")
                } else {
                    handleGenericError(response, result);
                }
            }

            @Override
            public void onFailure(Call<HomeResponse<HomeResponse.DeviceStatus>> call, Throwable t) {
                HomeResponse<HomeResponse.DeviceStatus> failure = new HomeResponse<>();
                failure.setSuccess(false);
                failure.setMessage("Lỗi kết nối mạng");
                result.postValue(failure);
            }
        });
    }

    public void updateEsp32(String token, String homeId, String deviceId, String newName, MutableLiveData<HomeResponse<Esp32Device>> result) {
        // Tạo request body đơn giản { "name": "newName" }
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", newName);

        apiService.updateEsp32("Bearer " + token, homeId, deviceId, requestBody)
                .enqueue(new Callback<HomeResponse<Esp32Device>>() {
                    @Override
                    public void onResponse(Call<HomeResponse<Esp32Device>> call, Response<HomeResponse<Esp32Device>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            result.postValue(response.body());
                        } else {
                            handleGenericError(response, result);
                        }
                    }

                    @Override
                    public void onFailure(Call<HomeResponse<Esp32Device>> call, Throwable t) {
                        HomeResponse<Esp32Device> failure = new HomeResponse<>();
                        failure.setSuccess(false);
                        failure.setMessage("Lỗi kết nối: " + t.getMessage());
                        result.postValue(failure);
                    }
                });
    }
    public void getEsp32Detail(String token, String homeId, String deviceId, MutableLiveData<HomeResponse<Esp32Device>> result) {
        apiService.getEsp32Details("Bearer " + token, homeId, deviceId).enqueue(new Callback<HomeResponse<Esp32Device>>() {
            @Override
            public void onResponse(Call<HomeResponse<Esp32Device>> call, Response<HomeResponse<Esp32Device>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Trả về success: true và Esp32Device object trong field data
                    result.postValue(response.body());
                } else {
                    // Sử dụng hàm handleGenericError có sẵn của bạn để parse lỗi từ Backend
                    handleGenericError(response, result);
                }
            }

            @Override
            public void onFailure(Call<HomeResponse<Esp32Device>> call, Throwable t) {
                HomeResponse<Esp32Device> failure = new HomeResponse<>();
                failure.setSuccess(false);
                failure.setMessage("Lỗi kết nối Server: " + t.getMessage());
                result.postValue(failure);
            }
        });
    }

    public void getDevicesByEsp(String token, String homeId, String espId, MutableLiveData<HomeResponse<List<Device>>> result) {
        apiService.getDevicesByEsp("Bearer " + token, homeId, espId).enqueue(new Callback<HomeResponse<List<Device>>>() {
            @Override
            public void onResponse(Call<HomeResponse<List<Device>>> call, Response<HomeResponse<List<Device>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(response.body());
                } else {
                    handleGenericError(response, result);
                }
            }

            @Override
            public void onFailure(Call<HomeResponse<List<Device>>> call, Throwable t) {
                HomeResponse<List<Device>> failure = new HomeResponse<>();
                failure.setSuccess(false);
                failure.setMessage("Lỗi kết nối: " + t.getMessage());
                result.postValue(failure);
            }
        });
    }

    // Xử lý lỗi riêng cho Provision vì class response không bọc trong HomeResponse.Data
    private void handleProvisionError(Response<Esp32ProvisionResponse> response, MutableLiveData<Esp32ProvisionResponse> result) {
        try {
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();
                Esp32ProvisionResponse errorRes = gson.fromJson(errorJson, Esp32ProvisionResponse.class);
                if (errorRes == null) errorRes = new Esp32ProvisionResponse();
                errorRes.setSuccess(false);
                result.postValue(errorRes);
            }
        } catch (Exception e) {
            Esp32ProvisionResponse fallback = new Esp32ProvisionResponse();
            fallback.setSuccess(false);
            fallback.setMessage("Lỗi không xác định: " + response.code());
            result.postValue(fallback);
        }
    }

    // Xử lý lỗi dùng chung cho các API có cấu trúc HomeResponse chuẩn
    private <T> void handleGenericError(Response<HomeResponse<T>> response, MutableLiveData<HomeResponse<T>> result) {
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                HomeResponse<T> errorRes = gson.fromJson(errorBody, HomeResponse.class);
                errorRes.setSuccess(false);
                result.postValue(errorRes);
            }
        } catch (Exception e) {
            HomeResponse<T> fallback = new HomeResponse<>();
            fallback.setSuccess(false);
            fallback.setMessage("Lỗi hệ thống: " + response.code());
            result.postValue(fallback);
        }
    }
}