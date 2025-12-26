package com.example.smarthome.network;

import com.example.smarthome.model.response.Esp32Device;
import com.example.smarthome.model.request.HomeRequest;
import com.example.smarthome.model.request.LoginRequest;
import com.example.smarthome.model.request.ProvisionESPRequest;
import com.example.smarthome.model.request.RegisterRequest;
import com.example.smarthome.model.request.ResetPasswordRequest;
import com.example.smarthome.model.request.SendOtpRequest;
import com.example.smarthome.model.request.VerifyOtpRequest;
import com.example.smarthome.model.response.AuthResponse;
import com.example.smarthome.model.response.Esp32ProvisionResponse;
import com.example.smarthome.model.response.HomeResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    // -----------------------------------------------------Đăng nhập-------------------------------------------
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    // Đăng ký (Bước 1): Gửi email để nhận OTP
    @POST("api/auth/register/sendRegisterReq")
    Call<AuthResponse> sendRegistrationOtp(@Body SendOtpRequest request);

    // 3. Đăng ký (Bước 2): Xác thực OTP
    @POST("api/auth/register/verifyRegisterOtp")
    Call<AuthResponse> verifyRegistrationOtp(@Body VerifyOtpRequest request);

    // 4. Đăng ký (Bước 3): Tạo Tài khoản
    @POST("api/auth/register/createAccount")
    Call<AuthResponse> createAccount(@Body RegisterRequest request);

    // 5. Quên Pass (Bước 1): Yêu cầu đặt lại mật khẩu
    @POST("api/auth/forgot-password")
    Call<AuthResponse> sendForgotPasswordReq(@Body SendOtpRequest request);

    // 6. Quên Pass (Bước 2): Xác thực OTP đặt lại
    @POST("api/auth/verify-otp")
    Call<AuthResponse> verifyResetOtp(@Body VerifyOtpRequest request);

    // 7. Quên Pass (Bước 3): Đặt lại mật khẩu
    @POST("api/auth/reset-password")
    Call<AuthResponse> resetPassword(@Body ResetPasswordRequest request);

    //-------------------------------------------------Home------------------------------------------------------

//    // 1. Chấp nhận lời mời vào nhà
//    @POST("api/home/invitation/accept")
//    Call<AuthResponse> acceptInvitation(@Body Object request); // Thay Object bằng Request model nếu có
//
//    // 2. Từ chối lời mời vào nhà
//    @POST("api/home/invitation/decline")
//    Call<AuthResponse> declineInvitation(@Body Object request);

    // 3. Tạo một ngôi nhà mới
    @POST("api/home")
    Call<HomeResponse<HomeResponse.HomeData>> createHome(
            @Header("Authorization") String token,
            @Body HomeRequest request
    );

    // 4. Lấy danh sách tất cả các nhà của người dùng
    @GET("api/home")
    Call<HomeResponse<List<HomeResponse.HomeData>>> getAllHomes(@Header("Authorization") String token);
//
//    // 5. Lấy thông tin chi tiết của một nhà cụ thể
//    @GET("api/home/{homeId}")
//    Call<AuthResponse> getHomeDetails(@Path("homeId") String homeId);
//
    // 6. Cập nhật tên của một nhà cụ thể
    @PATCH("api/home/{id}") // Thay đổi từ PUT sang PATCH ở đây
    Call<HomeResponse<HomeResponse.HomeData>> updateHomeName(
            @Header("Authorization") String token,
            @Path("id") String homeId,
            @Body HomeRequest request
);
//
//    // 7. Mời một thành viên vào nhà
//    @POST("api/home/{homeId}/invite")
//    Call<AuthResponse> inviteMember(@Path("homeId") String homeId, @Body Object request);
//
//    // 8. Xóa một thành viên khỏi nhà
//    @DELETE("api/home/{homeId}/members/{userId}")
//    Call<AuthResponse> removeMember(
//            @Path("homeId") String homeId,
//            @Path("userId") String userId
//    );
//
//    // 9. Lấy danh sách thành viên của một nhà cụ thể
//    @GET("api/home/{homeId}/members")
//    Call<AuthResponse> getHomeMembers(@Path("homeId") String homeId);

    //----------------------------------------------ESP32 Devices-----------------------------------------------

    // 1. Provision a new ESP32 device (Khởi tạo thiết bị mới vào nhà)
    @POST("api/home/{homeId}/esp32/provision")
    Call<Esp32ProvisionResponse> provisionEsp32(
            @Header("Authorization") String token,
            @Path("homeId") String homeId,
            @Body ProvisionESPRequest request
    );
//
//    // 2. Claim an ESP32 device (Xác nhận quyền sở hữu thiết bị)
//    @POST("api/esp32/claim")
//    Call<HomeResponse<Esp32Device>> claimEsp32(
//            @Header("Authorization") String token,
//            @Body Object request // Thường chứa deviceId hoặc code
//    );
//
//    // 3. Get all ESP32 devices of a home (Lấy danh sách thiết bị trong nhà)
    @GET("api/homes/{homeId}/esp32")
    Call<HomeResponse<List<Esp32Device>>> getAllEsp32InHome(
            @Header("Authorization") String token,
            @Path("homeId") String homeId
    );
//
//    // 4. Get details of a specific ESP32 device (Xem chi tiết 1 thiết bị)
//    @GET("api/home/{homeId}/esp32/{id}")
//    Call<HomeResponse<Esp32Device>> getEsp32Details(
//            @Header("Authorization") String token,
//            @Path("homeId") String homeId,
//            @Path("id") String deviceId
//    );
//
//    // 5. Update a specific ESP32 device (Cập nhật tên/thông tin thiết bị)
//    @PATCH("api/home/{homeId}/esp32/{id}")
//    Call<HomeResponse<Esp32Device>> updateEsp32(
//            @Header("Authorization") String token,
//            @Path("homeId") String homeId,
//            @Path("id") String deviceId,
//            @Body Object request
//    );
//
//    // 6. Delete a specific ESP32 device (Xóa thiết bị)
//    @DELETE("api/home/{homeId}/esp32/{id}")
//    Call<HomeResponse<Void>> deleteEsp32(
//            @Header("Authorization") String token,
//            @Path("homeId") String homeId,
//            @Path("id") String deviceId
//    );
//
//    // 7. Get ESP32 device status (Lấy trạng thái online/offline hiện tại)
    @GET("api/home/{homeId}/esp32/{id}/status")
    Call<HomeResponse<HomeResponse.DeviceStatus>> getEsp32Status(
            @Header("Authorization") String token,
            @Path("homeId") String homeId,
            @Path("id") String deviceId
    );
}
