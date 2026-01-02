package com.example.smarthome.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smarthome.R;
import com.example.smarthome.model.response.Esp32Device;
import java.util.List;

public class ESPAdapter extends RecyclerView.Adapter<ESPAdapter.ESPViewHolder> {

    private List<Esp32Device> deviceList;
    private OnItemClickListener listener;

    // Cập nhật Interface: Bỏ onUpdate, onDelete vì đã chuyển vào Detail
    // Thêm onManageOtherDevicesClick cho Menu mới
    public interface OnItemClickListener {
        void onItemClick(Esp32Device device);
        void onDetailsClick(Esp32Device device);
        void onManageOtherDevicesClick(Esp32Device device);
    }

    public ESPAdapter(List<Esp32Device> deviceList, OnItemClickListener listener) {
        this.deviceList = deviceList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ESPViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_esp, parent, false);
        return new ESPViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ESPViewHolder holder, int position) {
        Esp32Device device = deviceList.get(position);
        holder.bind(device, listener);
    }

    @Override
    public int getItemCount() {
        return deviceList != null ? deviceList.size() : 0;
    }

    public void updateData(List<Esp32Device> newData) {
        this.deviceList.clear();
        this.deviceList.addAll(newData);
        notifyDataSetChanged();
    }

    static class ESPViewHolder extends RecyclerView.ViewHolder {
        TextView textName, textStatus;
        ImageView btnMore;

        public ESPViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_device_name);
            textStatus = itemView.findViewById(R.id.text_device_status);
            btnMore = itemView.findViewById(R.id.btn_more_options);
        }

        public void bind(Esp32Device device, OnItemClickListener listener) {
            textName.setText(device.getName());

            if ("provisioned".equals(device.getStatus())) {
                textStatus.setText("Trực tuyến");
                textStatus.setTextColor(Color.parseColor("#4CAF50"));
            } else {
                textStatus.setText("Chờ cấu hình");
                textStatus.setTextColor(Color.parseColor("#F44336"));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(device);
            });

            btnMore.setOnClickListener(v -> {
                showPopupMenu(v, device, listener);
            });
        }

        private void showPopupMenu(View view, Esp32Device device, OnItemClickListener listener) {
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            // SỬA: Inflate từ file XML menu của bạn
            popup.getMenuInflater().inflate(R.menu.menu_esp_options, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                if (listener == null) return false;

                int itemId = item.getItemId();
                if (itemId == R.id.action_details) {
                    listener.onDetailsClick(device);
                    return true;
                } else if (itemId == R.id.action_manage_other_device) {
                    listener.onManageOtherDevicesClick(device);
                    return true;
                }
                return false;
            });
            popup.show();
        }
    }
}