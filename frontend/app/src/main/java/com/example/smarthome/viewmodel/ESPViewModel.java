package com.example.smarthome.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smarthome.model.response.Device;
import com.example.smarthome.model.response.Esp32Device;
import com.example.smarthome.model.response.Esp32ProvisionResponse;
import com.example.smarthome.model.response.HomeResponse;
import com.example.smarthome.repository.ESPRepository;

import java.util.List;

public class ESPViewModel extends ViewModel {
    private final ESPRepository repository;

    // LiveData quan sát kết quả lấy claimToken
    private final MutableLiveData<Esp32ProvisionResponse> provisionResult = new MutableLiveData<>();

    // LiveData quan sát trạng thái thiết bị (Polling)
    private final MutableLiveData<HomeResponse<HomeResponse.DeviceStatus>> statusResult = new MutableLiveData<>();

    public ESPViewModel() {
        this.repository = new ESPRepository();
    }

    public LiveData<Esp32ProvisionResponse> getProvisionResult() {
        return provisionResult;
    }

    public LiveData<HomeResponse<HomeResponse.DeviceStatus>> getStatusResult() {
        return statusResult;
    }

    private final MutableLiveData<HomeResponse<List<Esp32Device>>> espDevicesResult = new MutableLiveData<>();

    public LiveData<HomeResponse<List<Esp32Device>>> getEspDevicesResult() {
        return espDevicesResult;
    }

    private final MutableLiveData<HomeResponse<Esp32Device>> detailResult = new MutableLiveData<>();

    public LiveData<HomeResponse<Esp32Device>> getDetailResult() {
        return detailResult;
    }

    public void fetchEsp32Detail(String token, String homeId, String deviceId) {
        repository.getEsp32Detail(token, homeId, deviceId, detailResult);
    }

    public void provisionEsp32(String token, String homeId, String name) {
        repository.provisionEsp32(token, homeId, name, provisionResult);
    }

    public void checkESPStatus(String token, String homeId, String deviceId) {
        repository.getEsp32Status(token, homeId, deviceId, statusResult);
    }

    public void fetchAllEspDevices(String token, String homeId) {
        repository.getAllEsp32InHome(token, homeId, espDevicesResult);
    }

    public void resetProvisionResult() {
        provisionResult.setValue(null);
    }

    // Trong ESPViewModel.java
    public void resetDetailResult() {
        detailResult.setValue(null);
    }

    private final MutableLiveData<HomeResponse<List<Device>>> subDevicesResult = new MutableLiveData<>();

    public LiveData<HomeResponse<List<Device>>> getSubDevicesResult() {
        return subDevicesResult;
    }

    public void fetchDevicesByEsp(String token, String homeId, String espId) {
        repository.getDevicesByEsp(token, homeId, espId, subDevicesResult);
    }

    private final MutableLiveData<HomeResponse<Esp32Device>> updateResult = new MutableLiveData<>();

    public LiveData<HomeResponse<Esp32Device>> getUpdateResult() {
        return updateResult;
    }

    public void updateEspDevice(String token, String homeId, String deviceId, String newName) {
        repository.updateEsp32(token, homeId, deviceId, newName, updateResult);
    }

    // Đừng quên hàm reset để tránh lặp lại thông báo thành công
    public void resetUpdateResult() {
        updateResult.setValue(null);
    }
}