package com.example.smarthome.ui.adapter;

import android.content.res.ColorStateList;
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
import com.example.smarthome.model.response.Device;
import java.util.List;
import java.util.Map;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private List<Device> deviceList;
    private OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onDeviceClick(Device device);
        void onPowerChange(Device device, boolean isOn);
        void onUpdateSubDevice(Device device);
        void onAdvancedSettings(Device device);
        void onDeleteSubDevice(Device device);
    }

    public DeviceAdapter(List<Device> deviceList, OnDeviceClickListener listener) {
        this.deviceList = deviceList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sub_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = deviceList.get(position);

        holder.tvName.setText(device.getName());
        holder.tvType.setText("Loại: " + device.getType());

        // --- LOGIC ĐIỀU KHIỂN BẬT/TẮT THEO LOGIC 1/0 ---
        String type = device.getType().toLowerCase();
        if (type.equals("light") || type.equals("fan")) {
            holder.switchPower.setVisibility(View.VISIBLE);

            // 1. Kiểm tra giá trị từ lastState (Backend dùng { "value": 1/0 })
            boolean isActive = false;
            Map<String, Object> lastState = device.getLastState();

            if (lastState != null && lastState.containsKey("value")) {
                Object val = lastState.get("value");
                if (val instanceof Number) {
                    isActive = ((Number) val).intValue() == 1;
                }
            }

            // 2. Cập nhật UI Switch
            holder.switchPower.setOnCheckedChangeListener(null); // Gỡ listener tránh loop khi bind
            holder.switchPower.setChecked(isActive);

            // 3. Thay đổi màu sắc Sáng (Vàng) / Tối (Xám)
            updateSwitchColors(holder.switchPower, isActive);

            // 4. Lắng nghe sự kiện gạt công tắc
            holder.switchPower.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onPowerChange(device, isChecked);
                }
            });
        } else {
            holder.switchPower.setVisibility(View.GONE);
        }

        holder.btnOptions.setOnClickListener(v -> showPopupMenu(v, device));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDeviceClick(device);
        });
    }

    private void updateSwitchColors(androidx.appcompat.widget.SwitchCompat switchView, boolean isActive) {
        if (isActive) {
            switchView.setThumbTintList(ColorStateList.valueOf(Color.parseColor("#FFEB3B"))); // Vàng rực
            switchView.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#FFF59D"))); // Vàng nhạt
        } else {
            switchView.setThumbTintList(ColorStateList.valueOf(Color.parseColor("#BDBDBD"))); // Xám đậm
            switchView.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0"))); // Xám nhạt
        }
    }

    @Override
    public int getItemCount() {
        return deviceList != null ? deviceList.size() : 0;
    }

    public void updateData(List<Device> newData) {
        if (newData == null) return;
        this.deviceList.clear();
        this.deviceList.addAll(newData);
        notifyDataSetChanged();
    }

    public void updateSingleDeviceState(String deviceId, Map<String, Object> newState) {
        if (deviceList == null || deviceId == null) return;

        for (int i = 0; i < deviceList.size(); i++) {
            Device device = deviceList.get(i);
            if (deviceId.equals(device.getId())) {
                device.setLastState(newState); // Cập nhật dữ liệu mới vào item
                notifyItemChanged(i); // Yêu cầu vẽ lại duy nhất item này
                break;
            }
        }
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvType;
        ImageView btnOptions;
        androidx.appcompat.widget.SwitchCompat switchPower;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_device_name);
            tvType = itemView.findViewById(R.id.tv_device_type);
            btnOptions = itemView.findViewById(R.id.btn_device_options);
            switchPower = itemView.findViewById(R.id.switch_power);
        }
    }

    private void showPopupMenu(View view, Device device) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        popup.getMenu().add(0, 1, 0, "Cập nhật thiết bị");
        popup.getMenu().add(0, 2, 1, "Cài đặt nâng cao");
        popup.getMenu().add(0, 3, 2, "Xóa thiết bị");

        popup.setOnMenuItemClickListener(item -> {
            if (listener == null) return false;
            switch (item.getItemId()) {
                case 1: listener.onUpdateSubDevice(device); return true;
                case 2: listener.onAdvancedSettings(device); return true;
                case 3: listener.onDeleteSubDevice(device); return true;
                default: return false;
            }
        });
        popup.show();
    }
}