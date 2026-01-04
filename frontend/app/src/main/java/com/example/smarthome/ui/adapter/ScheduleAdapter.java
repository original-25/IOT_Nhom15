package com.example.smarthome.ui.adapter;

import android.graphics.Color; // Import thêm thư viện màu sắc
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smarthome.R;
import com.example.smarthome.model.response.Schedule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    private List<Schedule> scheduleList;
    private OnScheduleClickListener listener;

    public interface OnScheduleClickListener {
        void onDeleteClick(Schedule schedule);
    }

    public ScheduleAdapter(List<Schedule> scheduleList, OnScheduleClickListener listener) {
        this.scheduleList = scheduleList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        Schedule schedule = scheduleList.get(position);

        holder.tvTime.setText(schedule.getTime()); // Định dạng chuỗi "HH:mm"
        holder.tvName.setText(schedule.getName()); // Tên lịch trình

        // Lấy tên thiết bị từ object device đã được backend populate
        String deviceName = (schedule.getDevice() != null) ? schedule.getDevice().getName() : "N/A";
        String action = schedule.getAction().equalsIgnoreCase("on") ? "Bật" : "Tắt";

        holder.tvDetail.setText("Thiết bị: " + deviceName + " - Lệnh: " + action);

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(schedule);
        });

        // Kiểm tra thời gian thực của Frontend so với giờ hệ thống
        if (isTimePassed(schedule.getTime())) {
            holder.tvStatusLocal.setVisibility(View.VISIBLE);
            holder.tvStatusLocal.setText("● Đã thực hiện");
            holder.tvStatusLocal.setTextColor(Color.parseColor("#4CAF50")); // Màu xanh
        } else {
            holder.tvStatusLocal.setVisibility(View.VISIBLE);
            holder.tvStatusLocal.setText("○ Chờ đến giờ");
            holder.tvStatusLocal.setTextColor(Color.parseColor("#757575")); // Màu xám
        }
    }

    private boolean isTimePassed(String scheduleTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String currentTime = sdf.format(new Date());

            Date dateSchedule = sdf.parse(scheduleTime);
            Date dateCurrent = sdf.parse(currentTime);

            return dateCurrent != null && (dateCurrent.after(dateSchedule) || dateCurrent.equals(dateSchedule));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getItemCount() {
        return scheduleList != null ? scheduleList.size() : 0;
    }

    public void updateData(List<Schedule> newData) {
        this.scheduleList = newData;
        notifyDataSetChanged();
    }

    static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvName, tvDetail, tvStatusLocal; // Thêm tvStatusLocal vào đây
        ImageButton btnDelete;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_schedule_time);
            tvName = itemView.findViewById(R.id.tv_schedule_name);
            tvDetail = itemView.findViewById(R.id.tv_schedule_detail);
            btnDelete = itemView.findViewById(R.id.btn_delete_schedule);
            // Ánh xạ trường trạng thái mới từ layout XML
            tvStatusLocal = itemView.findViewById(R.id.tv_status_local);
        }
    }
}