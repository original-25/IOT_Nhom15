package com.example.smarthome.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smarthome.R;
import com.example.smarthome.model.response.Device;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private List<Device> deviceList;
    private OnDeviceClickListener listener;

    // Interface để xử lý sự kiện khi nhấn vào từng thiết bị con (nếu cần)
    public interface OnDeviceClickListener {
        void onDeviceClick(Device device);
    }

    public DeviceAdapter(List<Device> deviceList, OnDeviceClickListener listener) {
        this.deviceList = deviceList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng file layout item_sub_device.xml đã tạo
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sub_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = deviceList.get(position);

        // Đổ dữ liệu từ object Device vào các TextView
        holder.tvName.setText(device.getName());
        holder.tvType.setText("Loại: " + device.getType());

        // Bắt sự kiện click vào item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeviceClick(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return deviceList != null ? deviceList.size() : 0;
    }

    // Cập nhật danh sách thiết bị mới
    public void updateData(List<Device> newData) {
        this.deviceList.clear();
        this.deviceList.addAll(newData);
        notifyDataSetChanged();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvType;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_device_name);
            tvType = itemView.findViewById(R.id.tv_device_type);
        }
    }
}