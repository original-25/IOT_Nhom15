package com.example.smarthome.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.MutableLiveData;

import com.example.smarthome.model.data.User;
import com.example.smarthome.model.request.LoginRequest;
import com.example.smarthome.model.request.RegisterRequest;
import com.example.smarthome.model.request.ResetPasswordRequest;
import com.example.smarthome.model.request.SendOtpRequest;
import com.example.smarthome.model.request.VerifyOtpRequest;
import com.example.smarthome.model.response.AuthResponse;
import com.example.smarthome.network.ApiService;
import com.example.smarthome.network.RetrofitClient;
import com.google.gson.Gson;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {
    private ApiService apiService;
    private Context context;
    private Gson gson;
    private static final String PREFS_NAME = "AuthPrefs";
    private static final String TOKEN_KEY = "authToken";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_USER_ID = "userId";

    public AuthRepository(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = RetrofitClient.getApiService();
        this.gson = new Gson();
    }

    public void saveUserData(User user, String accessToken) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (user != null) {
            editor.putString(KEY_USERNAME, user.getName());
            editor.putString(KEY_EMAIL, user.getEmail());
            editor.putString(KEY_USER_ID, user.getId());
        }

        if (accessToken != null) {
            editor.putString(TOKEN_KEY, accessToken);
        }

        editor.apply();
    }

    public void login(LoginRequest request, MutableLiveData<AuthResponse> loginResult) {
        apiService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();

                    saveUserData(authResponse.getUser(), authResponse.getAccessToken());
                    loginResult.postValue(authResponse);

                } else if (response.errorBody() != null) {
                    try {
                        String errorBodyString = response.errorBody().string();
                        AuthResponse errorResponse = gson.fromJson(errorBodyString, AuthResponse.class);

                        String newErrorMessage = "Đã xảy ra lỗi không xác định.";

                        if (errorResponse != null) {
                            errorResponse.setSuccess(false);
                            if ("INVALID_CREDENTIALS".equals(errorResponse.getErrorCode())) {
                                newErrorMessage = "Email hoặc mật khẩu không đúng. Đăng ký tài khoản nếu bạn chưa tạo!";
                                errorResponse.setMessage(newErrorMessage);
                            }
                        } else {
                            // Lỗi khi parse body
                            errorResponse = new AuthResponse();
                            newErrorMessage = "Lỗi máy chủ không xác định (Mã: " + response.code() + ")";
                            errorResponse.setSuccess(false);
                            errorResponse.setMessage(newErrorMessage);
                        }

                        loginResult.postValue(errorResponse);

                    } catch (Exception e) {
                        // Xử lý lỗi I/O hoặc JSON Parse
                        AuthResponse fallbackError = new AuthResponse();
                        fallbackError.setMessage("Lỗi: Không thể xử lý phản hồi lỗi từ máy chủ.");
                        fallbackError.setSuccess(false);
                        loginResult.postValue(fallbackError);
                        e.printStackTrace();
                    }
                } else {
                    AuthResponse errorResponse = new AuthResponse();
                    errorResponse.setMessage("Yêu cầu thất bại (Code: " + response.code() + ").");
                    errorResponse.setSuccess(false);
                    loginResult.postValue(errorResponse);
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                AuthResponse errorResponse = new AuthResponse();
                errorResponse.setMessage("Lỗi kết nối server: Vui lòng kiểm tra mạng.");
                errorResponse.setSuccess(false);
                loginResult.postValue(errorResponse);
            }
        });
    }

    public void sendRegistrationOtp(SendOtpRequest request, MutableLiveData<AuthResponse> otpSentResult) {
        apiService.sendRegistrationOtp(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Backend trả về: {success: true, message: "OTP sent"}
                    otpSentResult.postValue(response.body());

                } else if (response.errorBody() != null) {
                    try {
                        String errorBodyString = response.errorBody().string();
                        AuthResponse errorResponse = gson.fromJson(errorBodyString, AuthResponse.class);

                        if (errorResponse != null) {
                            errorResponse.setSuccess(false);

                            // Kiểm tra lỗi Email đã tồn tại (HTTP 409)
                            if ("EMAIL_EXISTS".equals(errorResponse.getErrorCode())) {
                                errorResponse.setMessage("Địa chỉ Email này đã được đăng ký. Vui lòng thử email khác.");
                            } else if ("SERVER_ERROR".equals(errorResponse.getErrorCode())) {
                                errorResponse.setMessage("Lỗi máy chủ nội bộ. Vui lòng thử lại sau.");
                            }
                        } else {
                            // Lỗi khi parse body
                            errorResponse = new AuthResponse();
                            errorResponse.setMessage("Lỗi máy chủ không xác định (Mã: " + response.code() + ")");
                            errorResponse.setSuccess(false);
                        }
                        otpSentResult.postValue(errorResponse);
                    } catch (Exception e) {
                        // Lỗi I/O hoặc JSON Parse
                        AuthResponse fallbackError = new AuthResponse();
                        fallbackError.setMessage("Lỗi: Không thể xử lý phản hồi lỗi từ máy chủ.");
                        fallbackError.setSuccess(false);
                        otpSentResult.postValue(fallbackError);
                        e.printStackTrace();
                    }
                } else {
                    // Lỗi API nhưng không có errorBody
                    AuthResponse errorResponse = new AuthResponse();
                    errorResponse.setMessage("Yêu cầu thất bại (Code: " + response.code() + ").");
                    errorResponse.setSuccess(false);
                    otpSentResult.postValue(errorResponse);
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                // SỬA: Callback thành Call<AuthResponse>
                AuthResponse errorResponse = new AuthResponse();
                errorResponse.setMessage("Lỗi kết nối server: Vui lòng kiểm tra mạng.");
                errorResponse.setSuccess(false);
                otpSentResult.postValue(errorResponse);
            }
        });
    }

    public void verifyOtp(VerifyOtpRequest request, MutableLiveData<AuthResponse> verifyOtpResult) {
        apiService.verifyRegistrationOtp(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // OTP OK, Backend trả về success: true và data: { verifyToken }
                    verifyOtpResult.postValue(response.body());
                } else if (response.errorBody() != null) {
                    try {
                        String errorBodyString = response.errorBody().string();
                        AuthResponse errorResponse = gson.fromJson(errorBodyString, AuthResponse.class);

                        if (errorResponse != null) {
                            errorResponse.setSuccess(false);
                            switch (errorResponse.getErrorCode()) {
                                case "OTP_NOT_FOUND":
                                    // Backend trả về 404
                                    errorResponse.setMessage("Mã OTP không hợp lệ hoặc đã hết hạn.");
                                    break;
                                case "OTP_ALREADY_USED":
                                    // Backend trả về 400
                                    errorResponse.setMessage("Mã xác thực đã được sử dụng. Vui lòng thử lại quy trình.");
                                    break;
                                case "OTP_EXPIRED":
                                    // Backend trả về 400
                                    errorResponse.setMessage("Mã OTP đã hết hạn. Vui lòng yêu cầu gửi lại mã mới.");
                                    break;
                                case "INCORRECT_OTP":
                                    // Backend trả về 400
                                    errorResponse.setMessage("Mã OTP không đúng. Vui lòng kiểm tra lại mã đã gửi trong email.");
                                    break;
                                case "SERVER_ERROR":
                                    // Backend trả về 500
                                    errorResponse.setMessage("Lỗi máy chủ nội bộ. Vui lòng thử lại sau.");
                                    break;
                                default:
                                    errorResponse.setMessage("Lỗi xác thực không xác định: " + errorResponse.getMessage());
                                    break;
                            }

                        } else {
                            // Lỗi khi parse body
                            errorResponse = new AuthResponse();
                            errorResponse.setMessage("Lỗi máy chủ: Phản hồi không hợp lệ (Mã: " + response.code() + ")");
                            errorResponse.setSuccess(false);
                        }
                        verifyOtpResult.postValue(errorResponse);

                    } catch (Exception e) {
                        // Lỗi I/O khi đọc errorBody hoặc lỗi JSON Parse
                        AuthResponse fallbackError = new AuthResponse();
                        fallbackError.setMessage("Lỗi: Không thể xử lý phản hồi lỗi từ máy chủ.");
                        fallbackError.setSuccess(false);
                        verifyOtpResult.postValue(fallbackError);
                        e.printStackTrace();
                    }
                } else {
                    // Lỗi HTTP không có errorBody
                    AuthResponse errorResponse = new AuthResponse();
                    errorResponse.setMessage("Yêu cầu thất bại (Code: " + response.code() + ").");
                    errorResponse.setSuccess(false);
                    verifyOtpResult.postValue(errorResponse);
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                // Lỗi Mạng/Kết nối server
                AuthResponse errorResponse = new AuthResponse();
                errorResponse.setMessage("Lỗi kết nối server: Vui lòng kiểm tra mạng.");
                errorResponse.setSuccess(false);
                verifyOtpResult.postValue(errorResponse);
            }
        });
    }

    public void register(RegisterRequest request, MutableLiveData<AuthResponse> registerResult) {
        apiService.createAccount(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    saveUserData(authResponse.getUser(), authResponse.getAccessToken());
                    registerResult.postValue(authResponse);
                } else if (response.errorBody() != null) {
                    try {
                        String errorBodyString = response.errorBody().string();
                        AuthResponse errorResponse = gson.fromJson(errorBodyString, AuthResponse.class);

                        if (errorResponse != null) {
                            errorResponse.setSuccess(false);

                            if ("INVALID_VERIFY_TOKEN".equals(errorResponse.getErrorCode())) {
                                errorResponse.setMessage("Phiên xác thực đã hết hạn hoặc không hợp lệ. Vui lòng thử lại quy trình đăng ký.");
                            } else if ("SERVER_ERROR".equals(errorResponse.getErrorCode())) {
                                errorResponse.setMessage("Lỗi máy chủ nội bộ. Vui lòng thử lại sau.");
                            }
                        } else {
                            errorResponse = new AuthResponse();
                            errorResponse.setMessage("Lỗi máy chủ: Phản hồi không hợp lệ (Mã: " + response.code() + ")");
                            errorResponse.setSuccess(false);
                        }
                        registerResult.postValue(errorResponse);

                    } catch (Exception e) {
                        AuthResponse fallbackError = new AuthResponse();
                        fallbackError.setMessage("Lỗi: Không thể xử lý phản hồi lỗi từ máy chủ.");
                        fallbackError.setSuccess(false);
                        registerResult.postValue(fallbackError);
                        e.printStackTrace();
                    }
                } else {
                    // Lỗi HTTP không có errorBody
                    AuthResponse errorResponse = new AuthResponse();
                    errorResponse.setMessage("Đăng ký thất bại (Code: " + response.code() + ").");
                    errorResponse.setSuccess(false);
                    registerResult.postValue(errorResponse);
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                // Lỗi Mạng/Kết nối server
                AuthResponse errorResponse = new AuthResponse();
                errorResponse.setMessage("Lỗi kết nối server: Vui lòng kiểm tra mạng.");
                errorResponse.setSuccess(false);
                registerResult.postValue(errorResponse);
            }
        });
    }

    public void forgotPassword(SendOtpRequest request, MutableLiveData<AuthResponse> forgotPasswordResult) {
        apiService.sendForgotPasswordReq(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    forgotPasswordResult.postValue(authResponse);

                } else if (response.errorBody() != null) {
                    try {
                        String errorBodyString = response.errorBody().string();
                        AuthResponse errorResponse = gson.fromJson(errorBodyString, AuthResponse.class);

                        if (errorResponse != null) {
                            errorResponse.setSuccess(false);

                            if ("SERVER_ERROR".equals(errorResponse.getErrorCode())) {
                                errorResponse.setMessage("Lỗi máy chủ nội bộ. Vui lòng thử lại sau.");
                            }
                        } else {
                            errorResponse = new AuthResponse();
                            errorResponse.setMessage("Lỗi máy chủ không xác định (Mã: " + response.code() + ")");
                            errorResponse.setSuccess(false);
                        }
                        forgotPasswordResult.postValue(errorResponse);

                    } catch (Exception e) {
                        // Lỗi I/O hoặc JSON Parse
                        AuthResponse fallbackError = new AuthResponse();
                        fallbackError.setMessage("Lỗi: Không thể xử lý phản hồi lỗi từ máy chủ.");
                        fallbackError.setSuccess(false);
                        forgotPasswordResult.postValue(fallbackError);
                        e.printStackTrace();
                    }
                } else {
                    // Lỗi HTTP không có errorBody
                    AuthResponse errorResponse = new AuthResponse();
                    errorResponse.setMessage("Yêu cầu thất bại (Code: " + response.code() + ").");
                    errorResponse.setSuccess(false);
                    forgotPasswordResult.postValue(errorResponse);
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                // Lỗi Mạng/Kết nối server
                AuthResponse errorResponse = new AuthResponse();
                errorResponse.setMessage("Lỗi kết nối server: Vui lòng kiểm tra mạng.");
                errorResponse.setSuccess(false);
                forgotPasswordResult.postValue(errorResponse);
            }
        });
    }

    public void verifyResetOtp(VerifyOtpRequest request, MutableLiveData<AuthResponse>  verifyResetOtpResult) {
        apiService.verifyResetOtp(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    verifyResetOtpResult.postValue(response.body());
                } else if (response.errorBody() != null) {
                    try {
                        String errorBodyString = response.errorBody().string();
                        AuthResponse errorResponse = gson.fromJson(errorBodyString, AuthResponse.class);

                        if (errorResponse != null) {
                            errorResponse.setSuccess(false);

                            switch (errorResponse.getErrorCode()) {
                                case "OTP_NOT_FOUND":
                                    errorResponse.setMessage("Mã OTP không hợp lệ hoặc đã hết hạn.");
                                    break;
                                case "OTP_ALREADY_USED":
                                    errorResponse.setMessage("Mã xác thực đã được sử dụng. Vui lòng thử lại quy trình.");
                                    break;
                                case "OTP_EXPIRED":
                                    errorResponse.setMessage("Mã OTP đã hết hạn. Vui lòng yêu cầu gửi lại mã mới.");
                                    break;
                                case "INCORRECT_OTP":
                                    errorResponse.setMessage("Mã OTP không đúng. Vui lòng kiểm tra lại mã đã gửi trong email.");
                                    break;
                                case "SERVER_ERROR":
                                    errorResponse.setMessage("Lỗi máy chủ nội bộ. Vui lòng thử lại sau.");
                                    break;
                                default:
                                    errorResponse.setMessage("Lỗi xác thực không xác định: " + errorResponse.getMessage());
                                    break;
                            }

                        } else {
                            errorResponse = new AuthResponse();
                            errorResponse.setMessage("Lỗi máy chủ: Phản hồi không hợp lệ (Mã: " + response.code() + ")");
                            errorResponse.setSuccess(false);
                        }
                        verifyResetOtpResult.postValue(errorResponse);

                    } catch (Exception e) {
                        AuthResponse fallbackError = new AuthResponse();
                        fallbackError.setMessage("Lỗi: Không thể xử lý phản hồi lỗi từ máy chủ.");
                        fallbackError.setSuccess(false);
                        verifyResetOtpResult.postValue(fallbackError);
                        e.printStackTrace();
                    }
                } else {
                    // Lỗi HTTP không có errorBody
                    AuthResponse errorResponse = new AuthResponse();
                    errorResponse.setMessage("Yêu cầu thất bại (Code: " + response.code() + ").");
                    errorResponse.setSuccess(false);
                    verifyResetOtpResult.postValue(errorResponse);
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                // Lỗi Mạng/Kết nối server
                AuthResponse errorResponse = new AuthResponse();
                errorResponse.setMessage("Lỗi kết nối server: Vui lòng kiểm tra mạng.");
                errorResponse.setSuccess(false);
                verifyResetOtpResult.postValue(errorResponse);
            }
        });
    }

    public void resetPassword(ResetPasswordRequest request, MutableLiveData<AuthResponse>  resetPasswordResult) {
        apiService.resetPassword(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resetPasswordResult.postValue(response.body());
                } else if (response.errorBody() != null) {
                    try {
                        String errorBodyString = response.errorBody().string();
                        AuthResponse errorResponse = gson.fromJson(errorBodyString, AuthResponse.class);

                        if (errorResponse != null) {
                            errorResponse.setSuccess(false);

                            switch (errorResponse.getErrorCode()) {
                                case "RESET_TOKEN_NOT_FOUND":
                                    errorResponse.setMessage("Yêu cầu đặt lại mật khẩu không tồn tại.");
                                    break;
                                case "OTP_NOT_VERIFIED":
                                    errorResponse.setMessage("Mã OTP chưa được xác thực. Vui lòng thử lại từ bước xác thực OTP.");
                                    break;
                                case "INVALID_RESET_TOKEN":
                                    errorResponse.setMessage("Token đặt lại mật khẩu không hợp lệ.");
                                    break;
                                case "USER_NOT_FOUND":
                                    errorResponse.setMessage("Tài khoản không tồn tại.");
                                    break;
                                case "SERVER_ERROR":
                                    errorResponse.setMessage("Lỗi máy chủ nội bộ. Vui lòng thử lại sau.");
                                    break;
                                default:
                                    errorResponse.setMessage("Đặt lại mật khẩu thất bại: " + errorResponse.getMessage());
                                    break;
                            }

                        } else {
                            errorResponse = new AuthResponse();
                            errorResponse.setMessage("Lỗi máy chủ: Phản hồi không hợp lệ (Mã: " + response.code() + ")");
                            errorResponse.setSuccess(false);
                        }
                        resetPasswordResult.postValue(errorResponse);

                    } catch (Exception e) {
                        AuthResponse fallbackError = new AuthResponse();
                        fallbackError.setMessage("Lỗi: Không thể xử lý phản hồi lỗi từ máy chủ.");
                        fallbackError.setSuccess(false);
                        resetPasswordResult.postValue(fallbackError);
                        e.printStackTrace();
                    }
                } else {
                    // Lỗi HTTP không có errorBody
                    AuthResponse errorResponse = new AuthResponse();
                    errorResponse.setMessage("Yêu cầu thất bại (Code: " + response.code() + ").");
                    errorResponse.setSuccess(false);
                    resetPasswordResult.postValue(errorResponse);
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                // Lỗi Mạng/Kết nối server
                AuthResponse errorResponse = new AuthResponse();
                errorResponse.setMessage("Lỗi kết nối server: Vui lòng kiểm tra mạng.");
                errorResponse.setSuccess(false);
                resetPasswordResult.postValue(errorResponse);
            }
        });
    }
}