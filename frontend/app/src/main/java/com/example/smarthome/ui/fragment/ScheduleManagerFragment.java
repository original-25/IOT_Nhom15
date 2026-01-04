package com.example.smarthome.ui.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthome.R;
import com.example.smarthome.model.response.Device;
import com.example.smarthome.model.response.Schedule;
import com.example.smarthome.ui.adapter.ScheduleAdapter;
import com.example.smarthome.viewmodel.ESPViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class ScheduleManagerFragment extends Fragment {
    private static final String ARG_DEVICE_ID = "device_id";
    private static final String ARG_DEVICE_NAME = "device_name";
    private static final String ARG_HOME_ID = "home_id";

    private String mDeviceId, mDeviceName, mHomeId, authToken;
    private ESPViewModel espViewModel;
    private ScheduleAdapter adapter;
    private List<Schedule> scheduleList = new ArrayList<>();

    public static ScheduleManagerFragment newInstance(String homeId, String deviceId, String deviceName) {
        ScheduleManagerFragment fragment = new ScheduleManagerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_HOME_ID, homeId);
        args.putString(ARG_DEVICE_ID, deviceId);
        args.putString(ARG_DEVICE_NAME, deviceName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mHomeId = getArguments().getString(ARG_HOME_ID);
            mDeviceId = getArguments().getString(ARG_DEVICE_ID);
            mDeviceName = getArguments().getString(ARG_DEVICE_NAME);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule_manager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        authToken = prefs.getString("authToken", "");
        espViewModel = new ViewModelProvider(this).get(ESPViewModel.class);

        // Hiển thị tên thiết bị trên tiêu đề (Tùy chọn)
        TextView tvTitle = view.findViewById(R.id.tv_title);
        tvTitle.setText("Lập lịch cho: " + mDeviceName);

        // Cấu hình RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.rv_schedules);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ScheduleAdapter(scheduleList, schedule -> {
            // Hiển thị dialog xác nhận trước khi xóa
            new AlertDialog.Builder(requireContext())
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc muốn xóa lịch '" + schedule.getName() + "'?")
                    .setPositiveButton("Xóa", (dialog, which) ->
                            espViewModel.deleteSchedule(authToken, mHomeId, schedule.getId()))
                    .setNegativeButton("Hủy", null)
                    .show();
        });
        recyclerView.setAdapter(adapter);

        // Nút dấu cộng (+)
        FloatingActionButton fab = view.findViewById(R.id.fab_add_schedule);
        fab.setOnClickListener(v -> showSchedulerDialog());

        observeViewModel();
        refreshSchedules();
    }

    private void observeViewModel() {
        espViewModel.getSchedulesResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess()) {
                // Lọc danh sách lịch trình chỉ dành cho thiết bị này
                List<Schedule> filteredList = new ArrayList<>();
                for (Schedule s : response.getData()) {
                    if (s.getDevice() != null && s.getDevice().getId().equals(mDeviceId)) {
                        filteredList.add(s);
                    }
                }
                scheduleList.clear();
                scheduleList.addAll(filteredList);
                adapter.notifyDataSetChanged();
            }
        });

        espViewModel.getCreateScheduleResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess()) {
                Toast.makeText(getContext(), "Tạo lịch thành công", Toast.LENGTH_SHORT).show();
                refreshSchedules();
            }
        });

        espViewModel.getDeleteScheduleResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null) {
                if (response.isSuccess()) {
                    Toast.makeText(getContext(), "Đã xóa lịch trình", Toast.LENGTH_SHORT).show();
                    // Gọi lại API lấy danh sách để cập nhật UI
                    refreshSchedules();
                } else {
                    Toast.makeText(getContext(), "Xóa thất bại: " + response.getMessage(), Toast.LENGTH_SHORT).show();
                }
                espViewModel.resetDeleteScheduleResult();
            }
        });
    }

    private void refreshSchedules() {
        espViewModel.fetchSchedules(authToken, mHomeId);
    }

    // Tận dụng code Dialog bạn đã có
    private void showSchedulerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_schedule, null);

        EditText editScheduleName = dialogView.findViewById(R.id.edit_schedule_name);
        TimePicker timePicker = dialogView.findViewById(R.id.time_picker);
        androidx.appcompat.widget.SwitchCompat switchAction = dialogView.findViewById(R.id.switch_action_on_off);
        TextView tvActionLabel = dialogView.findViewById(R.id.tv_action_label);

        timePicker.setIs24HourView(true);
        switchAction.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tvActionLabel.setText(isChecked ? "Hành động: Bật (ON)" : "Hành động: Tắt (OFF)");
        });

        builder.setTitle("Thêm lịch mới")
                .setView(dialogView)
                .setPositiveButton("Lưu lịch", (dialog, which) -> {
                    String name = editScheduleName.getText().toString().trim();
                    int hour = (android.os.Build.VERSION.SDK_INT >= 23) ? timePicker.getHour() : timePicker.getCurrentHour();
                    int minute = (android.os.Build.VERSION.SDK_INT >= 23) ? timePicker.getMinute() : timePicker.getCurrentMinute();
                    String timeStr = String.format("%02d:%02d", hour, minute);
                    String actionStr = switchAction.isChecked() ? "on" : "off";

                    if (!name.isEmpty()) {
                        espViewModel.createSchedule(authToken, mHomeId, mDeviceId, name, timeStr, actionStr);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}