package com.example.smarthome.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthome.R;
import com.example.smarthome.model.response.HomeResponse;

import java.util.List;

public class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.HomeViewHolder> {
    private List<HomeResponse.HomeData> homeList;
    private OnHomeItemClickListener listener;

    // Định nghĩa Interface xử lý sự kiện
    public interface OnHomeItemClickListener {
        void onUpdateClick(HomeResponse.HomeData home);
        void onDetailsClick(HomeResponse.HomeData home);
        void onManageDevicesClick(HomeResponse.HomeData home);
    }

    // ĐÃ SỬA: Thêm tham số listener vào Constructor
    public HomeAdapter(List<HomeResponse.HomeData> homeList, OnHomeItemClickListener listener) {
        this.homeList = homeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HomeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home, parent, false);
        return new HomeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HomeViewHolder holder, int position) {
        HomeResponse.HomeData home = homeList.get(position);
        holder.textHomeName.setText(home.getName());

        holder.btnMore.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
            popupMenu.getMenuInflater().inflate(R.menu.home_options_menu, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {
                if (listener == null) return false;

                int itemId = item.getItemId();
                if (itemId == R.id.action_edit_name) {
                    listener.onUpdateClick(home);
                    return true;
                } else if (itemId == R.id.action_view_details) {
                    listener.onDetailsClick(home);
                    return true;
                }else if (itemId == R.id.action_add_esp_manager) {
                    listener.onManageDevicesClick(home);
                    return true;
                }
                return false;
            });
            popupMenu.show();
        });
    }

    @Override
    public int getItemCount() { return homeList != null ? homeList.size() : 0; }

    public static class HomeViewHolder extends RecyclerView.ViewHolder {
        TextView textHomeName;
        ImageButton btnMore;
        public HomeViewHolder(@NonNull View itemView) {
            super(itemView);
            textHomeName = itemView.findViewById(R.id.text_home_name);
            btnMore = itemView.findViewById(R.id.button_more_options);
        }
    }
}