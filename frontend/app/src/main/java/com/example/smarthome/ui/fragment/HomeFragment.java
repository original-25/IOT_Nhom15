package com.example.smarthome.ui.fragment;

import android.content.Context;
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
import com.example.smarthome.viewmodel.HomeViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private RecyclerView recyclerView;
    private HomeAdapter adapter;
    private final List<HomeResponse.HomeData> listHomes = new ArrayList<>();
    private String authToken;

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
                Toast.makeText(getContext(), "Chi tiết: " + home.getName(), Toast.LENGTH_SHORT).show();
                // Xử lý chuyển màn hình chi tiết tại đây
            }
        });
        recyclerView.setAdapter(adapter);

        // 2. Quan sát các LiveData kết quả
        observeHomesListResult();
        observeCreateHomeResult();
        observeUpdateHomeResult();

        // 3. Tải dữ liệu ban đầu
        if (authToken != null && !authToken.isEmpty()) {
            homeViewModel.fetchAllHomes(authToken);
        }

        view.findViewById(R.id.button_add_home).setOnClickListener(v -> showAddHomeDialog());
        view.findViewById(R.id.button_logout).setOnClickListener(v -> handleLogout());
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