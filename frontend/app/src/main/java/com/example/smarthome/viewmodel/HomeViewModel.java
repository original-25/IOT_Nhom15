package com.example.smarthome.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.smarthome.model.request.HomeRequest;
import com.example.smarthome.model.response.HomeResponse;
import com.example.smarthome.repository.HomeRepository;

import java.util.List;

public class HomeViewModel extends ViewModel {
    private HomeRepository repository = new HomeRepository();

    private MutableLiveData<HomeResponse<HomeResponse.HomeData>> createHomeResult = new MutableLiveData<>();

    private MutableLiveData<HomeResponse<List<HomeResponse.HomeData>>> homesListResult = new MutableLiveData<>();

    private MutableLiveData<HomeResponse<HomeResponse.HomeData>> updateHomeResult = new MutableLiveData<>();

    // Getter cho Create Home
    public LiveData<HomeResponse<HomeResponse.HomeData>> getCreateHomeResult() {
        return createHomeResult;
    }

    // Getter cho Homes List
    public LiveData<HomeResponse<List<HomeResponse.HomeData>>> getHomesListResult() {
        return homesListResult;
    }

    public LiveData<HomeResponse<HomeResponse.HomeData>> getUpdateHomeResult() {
        return updateHomeResult;
    }

    public void updateHomeName(String token, String homeId, String newName) {
        repository.updateHomeName(token, homeId, newName, updateHomeResult);
    }

    // Hàm lấy danh sách nhà
    public void fetchAllHomes(String token) {
        repository.getMyHomes(token, homesListResult);
    }

    // Hàm tạo nhà mới
    public void createHome(String token, String name) {
        repository.createHome(token, new HomeRequest(name), createHomeResult);
    }
}