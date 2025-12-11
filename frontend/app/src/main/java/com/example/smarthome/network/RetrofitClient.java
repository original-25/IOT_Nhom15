package com.example.smarthome.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // THAY THẾ bằng địa chỉ IP cục bộ và cổng của Backend Node.js
    private static final String BASE_URL = "http://<IP_CUA_MAY_SERVER_BACKEND>:3000/api/";

    private static Retrofit retrofit;

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    // Cần thêm ConverterFactory (đã thêm dependency Gson ở bước trước)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    // Hàm tiện ích để lấy ApiService (Interface đã tạo)
    public static ApiService getApiService() {
        return getRetrofitInstance().create(ApiService.class);
    }
}
