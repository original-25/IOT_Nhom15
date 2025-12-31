package com.example.smarthome.ui.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthome.R;
import com.example.smarthome.model.response.HomeResponse;
import com.example.smarthome.ui.adapter.MemberAdapter;
import com.example.smarthome.viewmodel.HomeViewModel;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class HomeDetailFragment extends Fragment {
    private String homeId, homeName, authToken;
    private HomeViewModel homeViewModel;
    private MemberAdapter adapter;
    private List<HomeResponse.MemberData> memberList = new ArrayList<>();

    public static HomeDetailFragment newInstance(String homeId, String homeName) {
        HomeDetailFragment fragment = new HomeDetailFragment();
        Bundle args = new Bundle();
        args.putString("home_id", homeId);
        args.putString("home_name", homeName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View btnInvite = view.findViewById(R.id.btn_invite_member);
        btnInvite.setOnClickListener(v -> showInviteMemberDialog());

        if (getArguments() != null) {
            homeId = getArguments().getString("home_id");
            homeName = getArguments().getString("home_name");
        }

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        authToken = prefs.getString("authToken", "");
        // Lấy userId của chính mình để ẩn nút xóa bản thân
        String myUserId = prefs.getString("userId", "");

        TextView textHomeName = view.findViewById(R.id.text_home_name_detail);
        if (homeName != null) {
            textHomeName.setText("Tên nhà: " + homeName);
        }

        RecyclerView rv = view.findViewById(R.id.recycler_members);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        // Khởi tạo Adapter với Listener xử lý sự kiện xóa
        adapter = new MemberAdapter(memberList, myUserId, new MemberAdapter.OnMemberClickListener() {
            @Override
            public void onDeleteClick(HomeResponse.MemberData member) {
                showConfirmDeleteDialog(member);
            }
        });
        rv.setAdapter(adapter);

        // 1. Quan sát kết quả lấy chi tiết nhà (Danh sách thành viên)
        homeViewModel.getHomeDetailResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess() && response.getData() != null) {
                memberList.clear();
                memberList.addAll(response.getData().getMembers());
                adapter.notifyDataSetChanged();
            }
        });

        homeViewModel.getInviteMemberResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null) {
                if (response.isSuccess()) {
                    Toast.makeText(getContext(), "Đã gửi lời mời thành công!", Toast.LENGTH_SHORT).show();
                } else {
                    // Hiển thị lỗi từ backend (ví dụ: User already in home)
                    Toast.makeText(getContext(), "Lỗi: " + response.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

        // 2. Quan sát kết quả xóa thành viên
        homeViewModel.getRemoveMemberResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess()) {
                Toast.makeText(getContext(), "Đã xóa thành viên thành công", Toast.LENGTH_SHORT).show();
                // Tải lại danh sách mới sau khi xóa
                homeViewModel.fetchHomeDetail(authToken, homeId);
            } else if (response != null) {
                Toast.makeText(getContext(), "Lỗi: " + response.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Gọi API lấy dữ liệu lần đầu
        homeViewModel.fetchHomeDetail(authToken, homeId);
    }

    private void showConfirmDeleteDialog(HomeResponse.MemberData member) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn mời " + member.getEmail() + " ra khỏi nhà không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // Gọi ViewModel thực hiện xóa
                    homeViewModel.removeMember(authToken, homeId, member.getUserId());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showInviteMemberDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Mời thành viên");
        builder.setMessage("Nhập email người dùng bạn muốn mời vào ngôi nhà này:");

        // Tạo EditText để nhập Email
        final EditText input = new EditText(requireContext());
        input.setHint("example@gmail.com");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        // Thêm Padding cho EditText để không bị dính sát mép Dialog
        FrameLayout container = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50;
        params.rightMargin = 50;
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Gửi lời mời", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty()) {
                // Gọi ViewModel để thực hiện mời
                homeViewModel.inviteMember(authToken, homeId, email);
            } else {
                Toast.makeText(getContext(), "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_detail, container, false);
    }
}