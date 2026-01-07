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

public class DeviceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_CONTROL = 1;
    private static final int TYPE_SENSOR = 2;

    private List<Device> deviceList;
    private OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onDeviceClick(Device device);
        void onPowerChange(Device device, boolean isOn);
        void onUpdateSubDevice(Device device);
        void onAdvancedSettings(Device device);
        void onDeleteSubDevice(Device device);
        void onShowStatistics(Device device);
    }

    public DeviceAdapter(List<Device> deviceList, OnDeviceClickListener listener) {
        this.deviceList = deviceList;
        this.listener = listener;
    }

    public void updateSensorFromLog(String deviceId, Map<String, Object> logEntry) {
        if (deviceList == null || deviceId == null || logEntry == null) return;

        for (int i = 0; i < deviceList.size(); i++) {
            Device device = deviceList.get(i);
            if (deviceId.equals(device.getId())) {

                // 1. Lấy object "data" từ logEntry
                Object dataField = logEntry.get("data");

                if (dataField instanceof Map) {
                    Map<String, Object> sensorValues = (Map<String, Object>) dataField;

                    // 2. Cập nhật vào lastState của Device để UI tự vẽ lại
                    // Chú ý: Ta truyền Map { "temp": 0, "humid": 18.9 } vào lastState
                    device.setLastState(sensorValues);
                    notifyItemChanged(i);
                }
                break;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        String type = deviceList.get(position).getType();
        if (type != null && type.equalsIgnoreCase("sensor")) {
            return TYPE_SENSOR;
        }
        return TYPE_CONTROL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENSOR) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sensor_device, parent, false);
            return new SensorViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sub_device, parent, false);
            return new ControlViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Device device = deviceList.get(position);
        if (holder instanceof SensorViewHolder) {
            bindSensor((SensorViewHolder) holder, device);
        } else {
            bindControl((ControlViewHolder) holder, device);
        }
    }

    private void bindSensor(SensorViewHolder holder, Device device) {
        holder.tvName.setText(device.getName());
        holder.tvType.setText("Loại: " + device.getType());

        Object stateObj = device.getLastState();
        if (stateObj instanceof Map) {
            Map<String, Object> stateMap = (Map<String, Object>) stateObj;

            // Khớp với database: "temp" và "humid"
            Object temp = stateMap.get("temp");
            Object humid = stateMap.get("humid");

            holder.tvTemp.setText(temp != null ? temp.toString() + "°C" : "--°C");
            holder.tvHumi.setText(humid != null ? humid.toString() + "%" : "--%");
        } else {
            holder.tvTemp.setText("--°C");
            holder.tvHumi.setText("--%");
        }

        holder.btnOptions.setOnClickListener(v -> showSensorPopupMenu(v, device));
    }

    private void bindControl(ControlViewHolder holder, Device device) {
        holder.tvName.setText(device.getName());
        holder.tvType.setText("Loại: " + device.getType());

        boolean isActive = false;
        Object stateObj = device.getLastState();

        if (stateObj != null) {
            if (stateObj instanceof Number) {
                isActive = ((Number) stateObj).intValue() == 1;
            } else if (stateObj instanceof Map) {
                Map<String, Object> stateMap = (Map<String, Object>) stateObj;
                Object val = stateMap.get("value");
                if (val instanceof Number) {
                    isActive = ((Number) val).intValue() == 1;
                }
            }
        }

        holder.switchPower.setOnCheckedChangeListener(null);
        holder.switchPower.setChecked(isActive);
        updateSwitchColors(holder.switchPower, isActive);

        holder.switchPower.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onPowerChange(device, isChecked);
        });

        holder.btnOptions.setOnClickListener(v -> showControlPopupMenu(v, device));
    }

    // --- Giữ nguyên các hàm updateSwitchColors, showPopupMenu, ViewHolder như cũ ---

    private void showSensorPopupMenu(View view, Device device) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        popup.getMenu().add(0, 1, 0, "Cập nhật thiết bị");
        popup.getMenu().add(0, 2, 1, "Xem thống kê");
        popup.getMenu().add(0, 3, 2, "Xóa thiết bị");

        popup.setOnMenuItemClickListener(item -> {
            if (listener == null) return false;
            switch (item.getItemId()) {
                case 1: listener.onUpdateSubDevice(device); return true;
                case 2: listener.onShowStatistics(device); return true;
                case 3: listener.onDeleteSubDevice(device); return true;
                default: return false;
            }
        });
        popup.show();
    }

    private void showControlPopupMenu(View view, Device device) {
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

    private void updateSwitchColors(androidx.appcompat.widget.SwitchCompat switchView, boolean isActive) {
        if (isActive) {
            switchView.setThumbTintList(ColorStateList.valueOf(Color.parseColor("#FFEB3B")));
            switchView.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#FFF59D")));
        } else {
            switchView.setThumbTintList(ColorStateList.valueOf(Color.parseColor("#BDBDBD")));
            switchView.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
        }
    }

    @Override
    public int getItemCount() { return deviceList != null ? deviceList.size() : 0; }

    public void updateData(List<Device> newData) {
        if (newData == null) return;
        this.deviceList.clear();
        this.deviceList.addAll(newData);
        notifyDataSetChanged();
    }

    public void updateSingleDeviceState(String deviceId, Object newState) {
        if (deviceList == null || deviceId == null) return;
        for (int i = 0; i < deviceList.size(); i++) {
            Device device = deviceList.get(i);
            if (deviceId.equals(device.getId())) {
                device.setLastState(newState);
                notifyItemChanged(i);
                break;
            }
        }
    }

    static class ControlViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvType;
        ImageView btnOptions;
        androidx.appcompat.widget.SwitchCompat switchPower;
        public ControlViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_device_name);
            tvType = itemView.findViewById(R.id.tv_device_type);
            btnOptions = itemView.findViewById(R.id.btn_device_options);
            switchPower = itemView.findViewById(R.id.switch_power);
        }
    }

    static class SensorViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvType, tvTemp, tvHumi;
        ImageView btnOptions;
        public SensorViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_sensor_name);
            tvType = itemView.findViewById(R.id.tv_sensor_type);
            tvTemp = itemView.findViewById(R.id.tv_temperature);
            tvHumi = itemView.findViewById(R.id.tv_humidity);
            btnOptions = itemView.findViewById(R.id.btn_sensor_options);
        }
    }
}