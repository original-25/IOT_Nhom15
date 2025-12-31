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

    private MutableLiveData<HomeResponse<HomeResponse.HomeDetailData>> homeDetailResult = new MutableLiveData<>();

    private final MutableLiveData<HomeResponse<Void>> removeMemberResult = new MutableLiveData<>();

    private final MutableLiveData<HomeResponse<Void>> inviteMemberResult = new MutableLiveData<>();

    private final MutableLiveData<HomeResponse<List<HomeResponse.InvitationData>>> invitationsResult = new MutableLiveData<>();

    private final MutableLiveData<HomeResponse<Void>> acceptInvitationResult = new MutableLiveData<>();
    private final MutableLiveData<HomeResponse<Void>> declineInvitationResult = new MutableLiveData<>();

    public LiveData<HomeResponse<List<HomeResponse.InvitationData>>> getInvitationsResult() {
        return invitationsResult;
    }

    public LiveData<HomeResponse<HomeResponse.HomeDetailData>> getHomeDetailResult() {
        return homeDetailResult;
    }

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

    public LiveData<HomeResponse<Void>> getRemoveMemberResult() {
        return removeMemberResult;
    }
    public LiveData<HomeResponse<Void>> getInviteMemberResult() {
        return inviteMemberResult;
    }

    public LiveData<HomeResponse<Void>> getAcceptInvitationResult() {
        return acceptInvitationResult;
    }

    public LiveData<HomeResponse<Void>> getDeclineInvitationResult() {
        return declineInvitationResult;
    }

    // --- Phương thức thực thi API Chấp nhận ---
    public void acceptInvitation(String token, String inviteToken) {
        repository.acceptInvitation(token, inviteToken, acceptInvitationResult);
    }

    // --- Phương thức thực thi API Từ chối ---
    public void declineInvitation(String token, String inviteToken) {
        repository.declineInvitation(token, inviteToken, declineInvitationResult);
    }

    public void fetchMyInvitations(String token) {
        repository.getMyInvitations(token, invitationsResult);
    }

    public void inviteMember(String token, String homeId, String email) {
        repository.inviteMember(token, homeId, email, inviteMemberResult);
    }

    public void removeMember(String token, String homeId, String userId) {
        repository.removeMember(token, homeId, userId, removeMemberResult);
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

    public void fetchHomeDetail(String token, String homeId) {
        repository.getHomeDetail(token, homeId, homeDetailResult);
    }
}