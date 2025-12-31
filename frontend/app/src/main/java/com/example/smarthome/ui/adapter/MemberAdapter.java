package com.example.smarthome.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthome.R;
import com.example.smarthome.model.response.HomeResponse;

import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {
    private List<HomeResponse.MemberData> memberList;
    private String currentUserId; // ID của bạn để ẩn nút xóa chính mình
    private OnMemberClickListener listener;

    // Interface để Fragment xử lý sự kiện xóa
    public interface OnMemberClickListener {
        void onDeleteClick(HomeResponse.MemberData member);
    }

    public MemberAdapter(List<HomeResponse.MemberData> memberList, String currentUserId, OnMemberClickListener listener) {
        this.memberList = memberList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        HomeResponse.MemberData member = memberList.get(position);

        holder.textEmail.setText(member.getEmail());

        boolean isOwner = "owner".equals(member.getRole());
        if (isOwner) {
            holder.textRole.setText("Vai trò: Chủ nhà");
            holder.textRole.setTextColor(Color.RED);
            holder.badgeRole.setVisibility(View.VISIBLE);
            holder.badgeRole.setText("CHỦ NHÀ");
        } else {
            holder.textRole.setText("Vai trò: Thành viên");
            holder.textRole.setTextColor(Color.GRAY);
            holder.badgeRole.setVisibility(View.GONE);
        }

        // LOGIC HIỂN THỊ NÚT XÓA:
        // Nếu ID thành viên trùng với ID của bạn, hoặc bạn không phải Owner (tùy logic app)
        // thì ẩn nút xóa. Ở đây ta ẩn nút xóa chính mình.
        if (member.getUserId() != null && member.getUserId().equals(currentUserId)) {
            holder.btnDelete.setVisibility(View.GONE);
        } else {
            holder.btnDelete.setVisibility(View.VISIBLE);
        }

        // Sự kiện click nút xóa
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(member);
            }
        });
    }

    @Override
    public int getItemCount() {
        return memberList != null ? memberList.size() : 0;
    }

    public void updateData(List<HomeResponse.MemberData> newData) {
        this.memberList = newData;
        notifyDataSetChanged();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView textEmail, textRole, badgeRole;
        ImageButton btnDelete; // Khai báo nút xóa

        MemberViewHolder(View v) {
            super(v);
            textEmail = v.findViewById(R.id.text_member_email);
            textRole = v.findViewById(R.id.text_member_role);
            badgeRole = v.findViewById(R.id.badge_role);
            btnDelete = v.findViewById(R.id.btn_delete_member); // Ánh xạ từ XML
        }
    }
}