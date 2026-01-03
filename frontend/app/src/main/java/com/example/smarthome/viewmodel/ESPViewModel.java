package com.example.smarthome.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.smarthome.model.request.CreateDeviceRequest;
import com.example.smarthome.model.response.Device;
import com.example.smarthome.model.response.Esp32Device;
import com.example.smarthome.model.response.Esp32ProvisionResponse;
import com.example.smarthome.model.response.HomeResponse;
import com.example.smarthome.repository.ESPRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ESPViewModel extends ViewModel {
    private final ESPRepository repository;

    // 1. LiveData cho luồng Provisioning ESP32
    private final MutableLiveData<Esp32ProvisionResponse> provisionResult = new MutableLiveData<>();
    private final MutableLiveData<HomeResponse<HomeResponse.DeviceStatus>> statusResult = new MutableLiveData<>();

    // 2. LiveData cho danh sách và chi tiết ESP32
    private final MutableLiveData<HomeResponse<List<Esp32Device>>> espDevicesResult = new MutableLiveData<>();
    private final MutableLiveData<HomeResponse<Esp32Device>> detailResult = new MutableLiveData<>();
    private final MutableLiveData<HomeResponse<Esp32Device>> updateResult = new MutableLiveData<>();

    // 3. LiveData cho quản lý thiết bị con (Sub-devices)
    private final MutableLiveData<HomeResponse<List<Device>>> subDevicesResult = new MutableLiveData<>();
    private final MutableLiveData<HomeResponse<Device>> createDeviceResult = new MutableLiveData<>();

    private final MutableLiveData<HomeResponse<Void>> deleteSubDeviceResult = new MutableLiveData<>();

    public ESPViewModel() {
        this.repository = new ESPRepository();
    }

    // --- Getters cho LiveData ---
    public LiveData<Esp32ProvisionResponse> getProvisionResult() { return provisionResult; }
    public LiveData<HomeResponse<HomeResponse.DeviceStatus>> getStatusResult() { return statusResult; }
    public LiveData<HomeResponse<List<Esp32Device>>> getEspDevicesResult() { return espDevicesResult; }
    public LiveData<HomeResponse<Esp32Device>> getDetailResult() { return detailResult; }
    public LiveData<HomeResponse<Esp32Device>> getUpdateResult() { return updateResult; }
    public LiveData<HomeResponse<List<Device>>> getSubDevicesResult() { return subDevicesResult; }
    public LiveData<HomeResponse<Device>> getCreateDeviceResult() { return createDeviceResult; }

    public LiveData<HomeResponse<Void>> getDeleteSubDeviceResult() {
        return deleteSubDeviceResult;
    }

    // --- Các phương thức gọi Repository ---

    // Lấy chi tiết ESP32
    public void fetchEsp32Detail(String token, String homeId, String deviceId) {
        repository.getEsp32Detail(token, homeId, deviceId, detailResult);
    }

    // Khởi tạo (Provision) ESP32 trên Server
    public void provisionEsp32(String token, String homeId, String name) {
        repository.provisionEsp32(token, homeId, name, provisionResult);
    }

    // Kiểm tra trạng thái Online của ESP32 (Polling)
    public void checkESPStatus(String token, String homeId, String deviceId) {
        repository.getEsp32Status(token, homeId, deviceId, statusResult);
    }

    // Lấy tất cả ESP32 trong một nhà
    public void fetchAllEspDevices(String token, String homeId) {
        repository.getAllEsp32InHome(token, homeId, espDevicesResult);
    }

    // Cập nhật tên bộ ESP32
    public void updateEspDevice(String token, String homeId, String deviceId, String newName) {
        repository.updateEsp32(token, homeId, deviceId, newName, updateResult);
    }

    // Lấy danh sách thiết bị con (Sub-devices) thuộc 1 ESP32
    public void fetchDevicesByEsp(String token, String homeId, String espId) {
        repository.getDevicesByEsp(token, homeId, espId, subDevicesResult);
    }

    public void createNewDevice(String token, String homeId, String name, String type, String espId, Map<String, Object> pinConfig) {
        CreateDeviceRequest request = new CreateDeviceRequest(name, type, espId);
        request.setConfig(pinConfig);
        repository.createDevice(token, homeId, request, createDeviceResult);
    }

    // --- Các hàm Reset (Cực kỳ quan trọng để chặn Sticky LiveData) ---

    public void resetProvisionResult() { provisionResult.setValue(null); }

    public void resetDetailResult() { detailResult.setValue(null); }

    public void resetUpdateResult() { updateResult.setValue(null); }

    public void resetCreateDeviceResult() { createDeviceResult.setValue(null); }

    public void deleteSubDevice(String token, String homeId, String deviceId) {
        repository.deleteSubDevice(token, homeId, deviceId, deleteSubDeviceResult);
    }

    // Hàm reset để dọn dẹp trạng thái sau khi xử lý xong
    public void resetDeleteSubDeviceResult() {
        deleteSubDeviceResult.setValue(null);
    }

    // LiveData cho kết quả gửi lệnh điều khiển (Bật/Tắt)
    private final MutableLiveData<HomeResponse<Void>> commandResult = new MutableLiveData<>();

    // LiveData cho trạng thái thực tế của thiết bị (Sáng/Tối)
    private final MutableLiveData<HomeResponse<HomeResponse.DeviceState>> stateResult = new MutableLiveData<>();

    // --- Getters ---
    public LiveData<HomeResponse<Void>> getCommandResult() { return commandResult; }
    public LiveData<HomeResponse<HomeResponse.DeviceState>> getStateResult() { return stateResult; }

    public void controlDevice(String token, String homeId, String deviceId, boolean turnOn) {
        Map<String, Object> payload = new HashMap<>();

        // Theo tài liệu Screenshot 153833:
        payload.put("action", "state");
        payload.put("value", turnOn ? 1 : 0);   // Value là 1 nếu bật, 0 nếu tắt

        repository.sendDeviceCommand(token, homeId, deviceId, payload, commandResult);
    }

    public void fetchDeviceState(String token, String homeId, String deviceId) {
        repository.getDeviceState(token, homeId, deviceId, stateResult);
    }


    // --- Reset Methods (Để dọn dẹp LiveData sau khi xử lý xong) ---
    public void resetCommandResult() { commandResult.setValue(null); }
    public void resetStateResult() { stateResult.setValue(null); }

    // LiveData cho kết quả cập nhật thiết bị con
    private final MutableLiveData<HomeResponse<Device>> updateSubDeviceResult = new MutableLiveData<>();

    public LiveData<HomeResponse<Device>> getUpdateSubDeviceResult() {
        return updateSubDeviceResult;
    }

    // Hàm cập nhật thiết bị
    public void updateSubDevice(String token, String homeId, String deviceId, String newName, Map<String, Object> newConfig) {
        Map<String, Object> updates = new HashMap<>();
        if (newName != null) updates.put("name", newName);
        if (newConfig != null) updates.put("config", newConfig);

        repository.updateSubDevice(token, homeId, deviceId, updates, updateSubDeviceResult);
    }

    public void resetUpdateSubDeviceResult() {
        updateSubDeviceResult.setValue(null);
    }
}