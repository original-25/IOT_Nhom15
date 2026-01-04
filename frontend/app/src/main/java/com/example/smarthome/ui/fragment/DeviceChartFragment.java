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
    private LineChart lineChart;
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
        lineChart = view.findViewById(R.id.line_chart);

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
                tempEntries.add(new Entry(i, t));
                humidEntries.add(new Entry(i, h));
            }
        }

        // 1. Cấu hình đường Nhiệt độ
        LineDataSet tempSet = new LineDataSet(tempEntries, "Nhiệt độ (°C)");
        tempSet.setColor(Color.RED);
        tempSet.setCircleColor(Color.RED);
        tempSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        tempSet.setValueTextSize(11f);
        tempSet.setLineWidth(2f);
        tempSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        // 2. Cấu hình đường Độ ẩm
        LineDataSet humidSet = new LineDataSet(humidEntries, "Độ ẩm (%)");
        humidSet.setColor(Color.BLUE);
        humidSet.setCircleColor(Color.BLUE);
        humidSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        humidSet.setValueTextSize(11f);
        humidSet.setLineWidth(2f);
        humidSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        // 3. Cấu hình Trục X
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(12f);
        xAxis.setValueFormatter(new TimeValueFormatter(logs));
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setGranularity(1f);

        // 4. Cấu hình 2 Trục Y - ĐỒNG BỘ LƯỚI
        int labelCount = 6;
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(Color.RED);
        leftAxis.setTextSize(12f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(2.0f); // Tùy chỉnh để không sít nhau
        leftAxis.setLabelCount(labelCount, true);

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setTextColor(Color.BLUE);
        rightAxis.setTextSize(12f);
        rightAxis.setAxisMinimum(10f);
        rightAxis.setAxisMaximum(50f); // Tùy chỉnh để không sít nhau
        rightAxis.setDrawGridLines(false);
        rightAxis.setLabelCount(labelCount, true);

        // 5. Đưa dữ liệu vào biểu đồ
        lineChart.setData(new LineData(tempSet, humidSet));

        // 6. LOẠI BỎ CHÚ THÍCH MẶC ĐỊNH
        lineChart.getLegend().setEnabled(false); // Đã loại bỏ đoạn cũ

        // 7. Cấu hình hiển thị chung
        lineChart.getDescription().setEnabled(false);

        // Tạo lề dưới vừa đủ để nhãn thời gian trục X không bị che,
        // còn chú thích sẽ do XML bên dưới quản lý
        lineChart.setExtraOffsets(10, 10, 10, 15);

        lineChart.animateX(1000);
        lineChart.invalidate();
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
