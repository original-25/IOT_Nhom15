package com.example.smarthome.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smarthome.R;
import com.example.smarthome.model.response.Esp32Device;
import java.util.List;

public class ESPAdapter extends RecyclerView.Adapter<ESPAdapter.ESPViewHolder> {

    private List<Esp32Device> deviceList;
    private OnItemClickListener listener;

    // Interface để xử lý sự kiện khi nhấn vào item
    public interface OnItemClickListener {
        void onItemClick(Esp32Device device);
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

        public ESPViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_device_name);
            textStatus = itemView.findViewById(R.id.text_device_status);
        }

        public void bind(Esp32Device device, OnItemClickListener listener) {
            textName.setText(device.getName());

            // Xử lý logic trạng thái dựa trên Backend
            if ("provisioned".equals(device.getStatus())) {
                textStatus.setText("Trực tuyến (Đã cấu hình)");
                textStatus.setTextColor(Color.parseColor("#4CAF50")); // Màu xanh lá
            } else {
                textStatus.setText("Chưa kích hoạt (Chờ cấu hình)");
                textStatus.setTextColor(Color.parseColor("#F44336")); // Màu đỏ
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(device);
            });
        }
    }
}