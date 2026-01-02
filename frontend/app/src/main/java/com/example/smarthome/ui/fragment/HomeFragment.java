package com.example.smarthome.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthome.R;
import com.example.smarthome.model.response.HomeResponse;
import com.example.smarthome.ui.MainActivity;
import com.example.smarthome.ui.adapter.HomeAdapter;
import com.example.smarthome.ui.adapter.InvitationAdapter;
import com.example.smarthome.viewmodel.HomeViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private RecyclerView recyclerView;
    private HomeAdapter adapter;
    private final List<HomeResponse.HomeData> listHomes = new ArrayList<>();
    private List<HomeResponse.InvitationData> invitationList = new ArrayList<>();
    private String authToken;

    private AlertDialog invitationDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        authToken = prefs.getString("authToken", "");
        String username = prefs.getString("username", "Người dùng");

        TextView welcomeTextView = view.findViewById(R.id.text_welcome_user);
        welcomeTextView.setText(getGreeting() + ", " + username + "!");

        // 1. Thiết lập RecyclerView với Listener cho Adapter
        recyclerView = view.findViewById(R.id.recycler_view_homes);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Khởi tạo adapter khớp với Interface OnHomeItemClickListener
        adapter = new HomeAdapter(listHomes, new HomeAdapter.OnHomeItemClickListener() {
            @Override
            public void onUpdateClick(HomeResponse.HomeData home) {
                showUpdateHomeDialog(home);
            }

            @Override
            public void onDetailsClick(HomeResponse.HomeData home) {
                // Chuyển sang Fragment Chi tiết nhà
                HomeDetailFragment detailFragment = HomeDetailFragment.newInstance(home.getId(), home.getName());

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, detailFragment)
                        .addToBackStack(null) // Để nhấn back quay lại danh sách nhà
                        .commit();
            }

            public void onManageDevicesClick(HomeResponse.HomeData home) {
                ESPManagerFragment fragment = ESPManagerFragment.newInstance(home.getId(), home.getName());

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack("manager_fragment") // THÊM DÒNG NÀY
                        .commit();
            }

        });
        recyclerView.setAdapter(adapter);

        // 2. Quan sát các LiveData kết quả
        observeHomesListResult();
        observeCreateHomeResult();
        observeUpdateHomeResult();

        observeAcceptInvitation();
        observeDeclineInvitation();

        // 3. Tải dữ liệu ban đầu
        if (authToken != null && !authToken.isEmpty()) {
            homeViewModel.fetchAllHomes(authToken);
        }

        homeViewModel.fetchMyInvitations(authToken);

        view.findViewById(R.id.button_add_home).setOnClickListener(v -> showAddHomeDialog());
        view.findViewById(R.id.button_invitations).setOnClickListener(v -> showInvitationsDialog());
        view.findViewById(R.id.button_logout).setOnClickListener(v -> handleLogout());

        homeViewModel.getInvitationsResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess()) {
                invitationList = response.getData();
                // Nếu có lời mời, bạn có thể đổi màu nút 📩 hoặc hiện thông báo ở đây
                if (!invitationList.isEmpty()) {
                    Toast.makeText(getContext(), "Bạn có " + invitationList.size() + " lời mời mới!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Quan sát danh sách nhà trả về
    private void observeHomesListResult() {
        homeViewModel.getHomesListResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess() && response.getData() != null) {
                listHomes.clear();
                listHomes.addAll(response.getData());
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void observeAcceptInvitation() {
        homeViewModel.getAcceptInvitationResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess()) {
                Toast.makeText(getContext(), "Gia nhập nhà thành công!", Toast.LENGTH_SHORT).show();
                homeViewModel.fetchAllHomes(authToken); // Tải lại danh sách nhà để hiện nhà mới
                homeViewModel.fetchMyInvitations(authToken); // Cập nhật lại danh sách lời mời (để biến mất cái vừa nhận)
            } else if (response != null) {
                Toast.makeText(getContext(), response.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 2. Quan sát kết quả Từ chối
    private void observeDeclineInvitation() {
        homeViewModel.getDeclineInvitationResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess()) {
                Toast.makeText(getContext(), "Đã từ chối lời mời", Toast.LENGTH_SHORT).show();
                homeViewModel.fetchMyInvitations(authToken); // Tải lại để lời mời biến mất
            }
        });
    }

    // Quan sát kết quả tạo nhà mới
    private void observeCreateHomeResult() {
        homeViewModel.getCreateHomeResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess()) {
                Toast.makeText(getContext(), "Tạo nhà thành công!", Toast.LENGTH_SHORT).show();
                if (response.getData() != null) {
                    listHomes.add(response.getData());
                    adapter.notifyItemInserted(listHomes.size() - 1);
                    recyclerView.scrollToPosition(listHomes.size() - 1);
                }
            } else if (response != null) {
                Toast.makeText(getContext(), "Lỗi: " + response.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 4. Quan sát kết quả cập nhật tên (PATCH)
    private void observeUpdateHomeResult() {
        homeViewModel.getUpdateHomeResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess()) {
                HomeResponse.HomeData updatedHome = response.getData();
                if (updatedHome != null) {
                    // Cập nhật tại chỗ trong List mà không cần tải lại toàn bộ
                    for (int i = 0; i < listHomes.size(); i++) {
                        if (listHomes.get(i).getId().equals(updatedHome.getId())) {
                            listHomes.set(i, updatedHome);
                            adapter.notifyItemChanged(i);
                            break;
                        }
                    }
                    Toast.makeText(getContext(), "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                }
            } else if (response != null) {
                Toast.makeText(getContext(), "Lỗi cập nhật: " + response.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 5. Dialog cập nhật tên nhà
    private void showUpdateHomeDialog(HomeResponse.HomeData home) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_home, null);
        builder.setView(dialogView);

        EditText editHomeName = dialogView.findViewById(R.id.edit_home_name);
        editHomeName.setText(home.getName()); // Hiện tên hiện tại

        builder.setTitle("Cập nhật tên nhà")
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    String newName = editHomeName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        homeViewModel.updateHomeName(authToken, home.getId(), newName);
                    }
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) return "Chào buổi sáng";
        if (hour >= 12 && hour < 18) return "Chào buổi chiều";
        return "Chào buổi tối";
    }

    private void showInvitationsDialog() {
        if (invitationList == null || invitationList.isEmpty()) {
            Toast.makeText(getContext(), "Không có lời mời nào", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        // Inflate layout custom của bạn
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_invitations, null);
        builder.setView(dialogView);
        builder.setTitle("Danh sách lời mời");

        RecyclerView rv = dialogView.findViewById(R.id.recycler_view_invitations);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        InvitationAdapter invAdapter = new InvitationAdapter(invitationList, new InvitationAdapter.OnInvitationClickListener() {
            @Override
            public void onAccept(HomeResponse.InvitationData invitation) {
                homeViewModel.acceptInvitation(authToken, invitation.getToken());
                if (invitationDialog != null) invitationDialog.dismiss(); // Đóng dialog sau khi bấm
            }

            @Override
            public void onDecline(HomeResponse.InvitationData invitation) {
                homeViewModel.declineInvitation(authToken, invitation.getToken());
                if (invitationDialog != null) invitationDialog.dismiss();
            }
        });

        rv.setAdapter(invAdapter);
        builder.setNegativeButton("Đóng", (dialog, which) -> dialog.dismiss());

        invitationDialog = builder.create();
        invitationDialog.show();
    }

    private void acceptInvite(HomeResponse.InvitationData invitation) {
        // Gọi API acceptInvitation đã viết ở bước trước
        // Sau khi thành công, nhớ gọi homeViewModel.fetchAllHomes(authToken) để cập nhật danh sách nhà mới
        Toast.makeText(getContext(), "Đang chấp nhận lời mời...", Toast.LENGTH_SHORT).show();
        // homeViewModel.acceptInvitation(authToken, invitation.getToken());
    }

    private void showAddHomeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_home, null);
        builder.setView(dialogView);

        EditText editHomeName = dialogView.findViewById(R.id.edit_home_name);

        builder.setTitle("Thêm ngôi nhà mới")
                .setCancelable(false)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String homeName = editHomeName.getText().toString().trim();
                    if (!homeName.isEmpty()) {
                        homeViewModel.createHome(authToken, homeName);
                    }
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void handleLogout() {
        requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE).edit().clear().apply();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).replaceFragment(new LoginFragment());
            Toast.makeText(getContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();
        }
    }
}