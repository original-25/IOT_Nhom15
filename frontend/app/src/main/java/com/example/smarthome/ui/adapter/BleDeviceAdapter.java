package com.example.smarthome.ui.adapter;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthome.R;

import java.util.List;

public class BleDeviceAdapter extends RecyclerView.Adapter<BleDeviceAdapter.ViewHolder> {

    private final List<BluetoothDevice> deviceList;
    private final OnDeviceClickListener listener;

    // Interface để xử lý sự kiện click
    public interface OnDeviceClickListener {
        void onDeviceClick(BluetoothDevice device);
    }

    public BleDeviceAdapter(List<BluetoothDevice> deviceList, OnDeviceClickListener listener) {
        this.deviceList = deviceList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ble_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    @SuppressLint("MissingPermission")
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BluetoothDevice device = deviceList.get(position);

        // Hiển thị tên (nếu null thì hiện Unknown)
        String name = device.getName();
        holder.tvName.setText(name != null ? name : "Thiết bị không tên");

        // Hiển thị địa chỉ MAC
        holder.tvMac.setText(device.getAddress());

        // Xử lý sự kiện click vào item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeviceClick(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvMac;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_device_name);
            tvMac = itemView.findViewById(R.id.tv_mac_address);
        }
    }
}