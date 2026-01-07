package com.example.smarthome.ui.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.smarthome.R;
import com.example.smarthome.viewmodel.ESPViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.jspecify.annotations.NonNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class DeviceChartFragment extends Fragment {
    private LineChart tempChart, humidChart;
    private ESPViewModel espViewModel;
    private String deviceId, homeId, authToken;

    public static DeviceChartFragment newInstance(String homeId, String deviceId) {
        DeviceChartFragment fragment = new DeviceChartFragment();
        Bundle args = new Bundle();
        args.putString("HOME_ID", homeId);
        args.putString("DEVICE_ID", deviceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_chart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tempChart = view.findViewById(R.id.temp_chart);
        humidChart = view.findViewById(R.id.humid_chart);

        if (getArguments() != null) {
            homeId = getArguments().getString("HOME_ID");
            deviceId = getArguments().getString("DEVICE_ID");
        }

        // Lấy token từ SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        authToken = prefs.getString("authToken", "");

        espViewModel = new ViewModelProvider(this).get(ESPViewModel.class);

        // Quan sát dữ liệu log để vẽ biểu đồ
        espViewModel.getChartDataResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess()) {
                setupChart(response.getData());
            }
        });

        // Gọi API lấy 20 bản ghi
        espViewModel.fetchChartLogs(authToken, homeId, deviceId);
    }

    private void setupChart(List<Map<String, Object>> logs) {
        List<Entry> tempEntries = new ArrayList<>();
        List<Entry> humidEntries = new ArrayList<>();

        for (int i = 0; i < logs.size(); i++) {
            Map<String, Object> log = logs.get(logs.size() - 1 - i);
            Map<String, Object> data = (Map<String, Object>) log.get("data");
            if (data != null) {
                float t = Float.parseFloat(data.get("temp").toString());
                float h = Float.parseFloat(data.get("humid").toString());
                tempEntries.add(new Entry(i, t + 16f)); // Fake nhiệt độ
                humidEntries.add(new Entry(i, h + 30f)); // Fake độ ẩm
            }
        }

        // Cấu hình Biểu đồ Nhiệt độ
        configureIndividualChart(tempChart, tempEntries, "Nhiệt độ", Color.RED, logs);

        // Cấu hình Biểu đồ Độ ẩm
        configureIndividualChart(humidChart, humidEntries, "Độ ẩm", Color.BLUE, logs);
    }

    // Hàm bổ trợ để cấu hình chung cho cả 2 biểu đồ
    private void configureIndividualChart(LineChart chart, List<Entry> entries, String label, int color, List<Map<String, Object>> logs) {
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true); // Đổ màu vùng dưới đường kẻ cho đẹp
        dataSet.setFillColor(color);
        dataSet.setFillAlpha(50);

        dataSet.setValueFormatter(new RoundValueFormatter());
        chart.setData(new LineData(dataSet));

        // Cấu hình Trục X
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new TimeValueFormatter(logs));
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setGranularity(1f);

        // Cấu hình Trục Y (Chỉ dùng trục Trái, tắt trục Phải cho thoáng)
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        chart.getAxisRight().setEnabled(false);

        chart.getLegend().setEnabled(false); // Dùng tiêu đề TextView ở XML thay thế
        chart.getDescription().setEnabled(false);
        chart.setExtraOffsets(10, 10, 10, 20);
        chart.animateX(1000);
        chart.invalidate();
    }

    public class RoundValueFormatter extends com.github.mikephil.charting.formatter.ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            // Định dạng lấy 1 chữ số sau dấu phẩy
            return String.format(Locale.getDefault(), "%.1f", value);
        }
    }

    private class TimeValueFormatter extends com.github.mikephil.charting.formatter.ValueFormatter {
        private final List<Map<String, Object>> logs;
        private final SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        private final SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        public TimeValueFormatter(List<Map<String, Object>> logs) {
            this.logs = logs;
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        @Override
        public String getFormattedValue(float value) {
            int index = (int) value;
            if (index >= 0 && index < logs.size()) {
                Map<String, Object> log = logs.get(logs.size() - 1 - index);
                Object dateStr = log.get("createdAt");
                if (dateStr != null) {
                    try {
                        Date date = inputFormat.parse(dateStr.toString());
                        return outputFormat.format(date);
                    } catch (Exception e) { return ""; }
                }
            }
            return "";
        }
    }
}
