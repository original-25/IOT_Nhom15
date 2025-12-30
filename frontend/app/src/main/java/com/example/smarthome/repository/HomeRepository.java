package com.example.smarthome.repository;

import androidx.lifecycle.MutableLiveData;
import com.example.smarthome.model.request.HomeRequest;
import com.example.smarthome.model.response.HomeResponse;
import com.example.smarthome.network.ApiService;
import com.example.smarthome.network.RetrofitClient;
import com.google.gson.Gson;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeRepository {
    private ApiService apiService;
    private Gson gson;

    public HomeRepository() {
        this.apiService = RetrofitClient.getApiService();
        this.gson = new Gson();
    }

    public void createHome(String token, HomeRequest request, MutableLiveData<HomeResponse<HomeResponse.HomeData>> result) {
        apiService.createHome("Bearer " + token, request).enqueue(new Callback<HomeResponse<HomeResponse.HomeData>>() {
            @Override
            public void onResponse(Call<HomeResponse<HomeResponse.HomeData>> call, Response<HomeResponse<HomeResponse.HomeData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(response.body());
                } else {
                    handleError(response, result);
                }
            }

            @Override
            public void onFailure(Call<HomeResponse<HomeResponse.HomeData>> call, Throwable t) {
                HomeResponse<HomeResponse.HomeData> error = new HomeResponse<>();
                error.setSuccess(false);
                error.setMessage("Lỗi kết nối mạng: " + t.getMessage());
                result.postValue(error);
            }
        });
    }

    public void getMyHomes(String token, MutableLiveData<HomeResponse<List<HomeResponse.HomeData>>> result) {
        apiService.getAllHomes("Bearer " + token).enqueue(new Callback<HomeResponse<List<HomeResponse.HomeData>>>() {
            @Override
            public void onResponse(Call<HomeResponse<List<HomeResponse.HomeData>>> call, Response<HomeResponse<List<HomeResponse.HomeData>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(response.body());
                } else {
                    handleError(response, result);
                }
            }

            @Override
            public void onFailure(Call<HomeResponse<List<HomeResponse.HomeData>>> call, Throwable t) {
                HomeResponse<List<HomeResponse.HomeData>> failure = new HomeResponse<>();
                failure.setSuccess(false);
                failure.setMessage("Lỗi kết nối mạng");
                result.postValue(failure);
            }
        });
    }

    public void getHomeDetail(String token, String homeId, MutableLiveData<HomeResponse<HomeResponse.HomeDetailData>> result) {
        apiService.getHomeDetail("Bearer " + token, homeId).enqueue(new Callback<HomeResponse<HomeResponse.HomeDetailData>>() {
            @Override
            public void onResponse(Call<HomeResponse<HomeResponse.HomeDetailData>> call, Response<HomeResponse<HomeResponse.HomeDetailData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(response.body());
                } else {
                    // Sử dụng hàm handleError dùng chung của bạn
                    handleError(response, result);
                }
            }

            @Override
            public void onFailure(Call<HomeResponse<HomeResponse.HomeDetailData>> call, Throwable t) {
                HomeResponse<HomeResponse.HomeDetailData> error = new HomeResponse<>();
                error.setSuccess(false);
                error.setMessage("Lỗi kết nối mạng: " + t.getMessage());
                result.postValue(error);
            }
        });
    }

    public void removeMember(String token, String homeId, String userId, MutableLiveData<HomeResponse<Void>> result) {
        apiService.removeMember("Bearer " + token, homeId, userId).enqueue(new Callback<HomeResponse<Void>>() {
            @Override
            public void onResponse(Call<HomeResponse<Void>> call, Response<HomeResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(response.body());
                } else {
                    // Sử dụng hàm handleError dùng chung để parse lỗi từ Backend (như CANNOT_REMOVE_OWNER)
                    handleError(response, result);
                }
            }

            @Override
            public void onFailure(Call<HomeResponse<Void>> call, Throwable t) {
                HomeResponse<Void> error = new HomeResponse<>();
                error.setSuccess(false);
                error.setMessage("Lỗi kết nối: " + t.getMessage());
                result.postValue(error);
            }
        });
    }

    public void updateHomeName(String token, String homeId, String newName, MutableLiveData<HomeResponse<HomeResponse.HomeData>> result) {
        HomeRequest request = new HomeRequest(newName);
        apiService.updateHomeName("Bearer " + token, homeId, request).enqueue(new Callback<HomeResponse<HomeResponse.HomeData>>() {
            @Override
            public void onResponse(Call<HomeResponse<HomeResponse.HomeData>> call, Response<HomeResponse<HomeResponse.HomeData>> response) {
                if (response.isSuccessful()) {
                    result.postValue(response.body());
                } else {
                    handleError(response, result);
                }
            }

            @Override
            public void onFailure(Call<HomeResponse<HomeResponse.HomeData>> call, Throwable t) {
                HomeResponse<HomeResponse.HomeData> error = new HomeResponse<>();
                error.setSuccess(false);
                error.setMessage("Lỗi kết nối");
                result.postValue(error);
            }
        });
    }

    private <T> void handleError(Response<HomeResponse<T>> response, MutableLiveData<HomeResponse<T>> result) {
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                // Ép kiểu động dựa trên kiểu T được truyền vào
                HomeResponse<T> errorRes = gson.fromJson(errorBody, HomeResponse.class);
                if (errorRes == null) {
                    errorRes = new HomeResponse<>();
                    errorRes.setMessage("Lỗi hệ thống: " + response.code());
                }
                errorRes.setSuccess(false);
                result.postValue(errorRes);
            }
        } catch (Exception e) {
            HomeResponse<T> fallback = new HomeResponse<>();
            fallback.setSuccess(false);
            fallback.setMessage("Lỗi không xác định: " + response.code());
            result.postValue(fallback);
        }
    }
}