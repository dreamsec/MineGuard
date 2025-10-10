package com.example.mineguard.home;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.example.mineguard.R;
import com.example.mineguard.data.StatisticsData;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HomeFragment extends Fragment {

    private TextView tvTotalAlarms;
    private TextView tvProcessedAlarms;
    private TextView tvTotalDevices;
    private TextView tvOnlineDevices;
    
    private LineChart chartAlarmTrends;
    private PieChart chartSceneStatistics;
    private PieChart chartDeviceStatistics;
    private PieChart chartTypeStatistics;
    
    private TextView tvTimeOneHour;
    private TextView tvTimeOneWeek;
    private TextView tvTimeOneMonth;
    private TextView tvTimeOneYear;
    
    private TextView tvSceneChartTitle;
    private TextView tvDeviceChartTitle;
    private TextView tvTypeChartTitle;
    
    private TimeSpan currentTimeSpan = TimeSpan.ONE_HOUR;
    
    private enum TimeSpan {
        ONE_HOUR, ONE_WEEK, ONE_MONTH, ONE_YEAR
    }

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        initViews(view);
        setupTimeSpanListeners();
        loadStatisticsData();
        
        return view;
    }
    
    private void initViews(View view) {
        // Overview card views
        tvTotalAlarms = view.findViewById(R.id.tv_total_alarms);
        tvProcessedAlarms = view.findViewById(R.id.tv_processed_alarms);
        tvTotalDevices = view.findViewById(R.id.tv_total_devices);
        tvOnlineDevices = view.findViewById(R.id.tv_online_devices);
        
        // Chart views
        chartAlarmTrends = view.findViewById(R.id.chart_alarm_trends);
        chartSceneStatistics = view.findViewById(R.id.pie_chart);
        chartDeviceStatistics = view.findViewById(R.id.card_device_statistics).findViewById(R.id.pie_chart);
        chartTypeStatistics = view.findViewById(R.id.card_type_statistics).findViewById(R.id.pie_chart);
        
        // Time span views
        tvTimeOneHour = view.findViewById(R.id.tv_time_one_hour);
        tvTimeOneWeek = view.findViewById(R.id.tv_time_one_week);
        tvTimeOneMonth = view.findViewById(R.id.tv_time_one_month);
        tvTimeOneYear = view.findViewById(R.id.tv_time_one_year);
        
        // Chart title views
        tvSceneChartTitle = view.findViewById(R.id.card_scene_statistics).findViewById(R.id.tv_chart_title);
        tvDeviceChartTitle = view.findViewById(R.id.card_device_statistics).findViewById(R.id.tv_chart_title);
        tvTypeChartTitle = view.findViewById(R.id.card_type_statistics).findViewById(R.id.tv_chart_title);
        
        // Set chart titles
        tvSceneChartTitle.setText(R.string.scene_statistics);
        tvDeviceChartTitle.setText(R.string.device_statistics);
        tvTypeChartTitle.setText(R.string.type_statistics);
    }
    
    private void setupTimeSpanListeners() {
        tvTimeOneHour.setOnClickListener(v -> selectTimeSpan(TimeSpan.ONE_HOUR));
        tvTimeOneWeek.setOnClickListener(v -> selectTimeSpan(TimeSpan.ONE_WEEK));
        tvTimeOneMonth.setOnClickListener(v -> selectTimeSpan(TimeSpan.ONE_MONTH));
        tvTimeOneYear.setOnClickListener(v -> selectTimeSpan(TimeSpan.ONE_YEAR));
    }
    
    private void selectTimeSpan(TimeSpan timeSpan) {
        currentTimeSpan = timeSpan;
        
        // Reset all time span text views
        resetTimeSpanStyles();
        
        // Set selected style
        TextView selectedView = null;
        switch (timeSpan) {
            case ONE_HOUR:
                selectedView = tvTimeOneHour;
                break;
            case ONE_WEEK:
                selectedView = tvTimeOneWeek;
                break;
            case ONE_MONTH:
                selectedView = tvTimeOneMonth;
                break;
            case ONE_YEAR:
                selectedView = tvTimeOneYear;
                break;
        }
        
        if (selectedView != null) {
            selectedView.setTextColor(getResources().getColor(R.color.primary_blue));
            selectedView.setBackground(getResources().getDrawable(R.drawable.time_span_selected));
        }
        
        // Reload chart data with new time span
        loadAlarmTrendsData();
    }
    
    private void resetTimeSpanStyles() {
        TextView[] timeSpanViews = {tvTimeOneHour, tvTimeOneWeek, tvTimeOneMonth, tvTimeOneYear};
        for (TextView view : timeSpanViews) {
            view.setTextColor(getResources().getColor(R.color.text_secondary));
            view.setBackground(null);
        }
    }
    
    private void loadStatisticsData() {
        // Generate mock data for demonstration
        StatisticsData data = generateMockStatisticsData();
        
        // Update overview data
        tvTotalAlarms.setText(String.valueOf(data.getTotalAlarms()));
        tvProcessedAlarms.setText(String.valueOf(data.getProcessedAlarms()));
        tvTotalDevices.setText(String.valueOf(data.getTotalDevices()));
        tvOnlineDevices.setText(String.valueOf(data.getOnlineDevices()));
        
        // Load chart data
        loadAlarmTrendsData();
        loadSceneStatisticsData();
        loadDeviceStatisticsData();
        loadAlarmTypeStatisticsData();
    }
    
    private void loadAlarmTrendsData() {
        List<StatisticsData.AlarmTrendData> trendData = generateMockAlarmTrendData(currentTimeSpan);
        
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        
        for (int i = 0; i < trendData.size(); i++) {
            entries.add(new Entry(i, trendData.get(i).getAlarmCount()));
            labels.add(trendData.get(i).getTimeLabel());
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "报警数量");
        dataSet.setColor(getResources().getColor(R.color.primary_blue));
        dataSet.setCircleColor(getResources().getColor(R.color.primary_blue));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);
        
        LineData lineData = new LineData(dataSets);
        chartAlarmTrends.setData(lineData);
        
        // Customize chart appearance
        chartAlarmTrends.getDescription().setEnabled(false);
        chartAlarmTrends.setTouchEnabled(true);
        chartAlarmTrends.setDragEnabled(true);
        chartAlarmTrends.setScaleEnabled(true);
        chartAlarmTrends.setPinchZoom(true);
        chartAlarmTrends.setDrawGridBackground(false);
        chartAlarmTrends.getLegend().setEnabled(false);
        
        // Customize x-axis
        chartAlarmTrends.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < labels.size()) {
                    return labels.get(index);
                }
                return "";
            }
        });
        
        chartAlarmTrends.invalidate();
    }
    
    private void loadSceneStatisticsData() {
        List<StatisticsData.SceneAlarmData> sceneData = generateMockSceneData();
        
        ArrayList<PieEntry> entries = new ArrayList<>();
        for (StatisticsData.SceneAlarmData data : sceneData) {
            entries.add(new PieEntry(data.getAlarmCount(), data.getSceneName()));
        }
        
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(getColorArray());
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        
        PieData pieData = new PieData(dataSet);
        chartSceneStatistics.setData(pieData);
        
        // Customize chart appearance
        chartSceneStatistics.getDescription().setEnabled(false);
        chartSceneStatistics.setDrawHoleEnabled(true);
        chartSceneStatistics.setHoleColor(Color.WHITE);
        chartSceneStatistics.setTransparentCircleRadius(61f);
        chartSceneStatistics.setHoleRadius(58f);
        chartSceneStatistics.setDrawCenterText(true);
        chartSceneStatistics.setCenterText("场景分布");
        chartSceneStatistics.setCenterTextSize(16f);
        chartSceneStatistics.getLegend().setEnabled(true);
        chartSceneStatistics.getLegend().setTextSize(12f);
        
        chartSceneStatistics.invalidate();
    }
    
    private void loadDeviceStatisticsData() {
        List<StatisticsData.DeviceAlarmData> deviceData = generateMockDeviceData();
        
        ArrayList<PieEntry> entries = new ArrayList<>();
        for (StatisticsData.DeviceAlarmData data : deviceData) {
            entries.add(new PieEntry(data.getAlarmCount(), data.getDeviceName()));
        }
        
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(getColorArray());
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        
        PieData pieData = new PieData(dataSet);
        chartDeviceStatistics.setData(pieData);
        
        // Customize chart appearance
        chartDeviceStatistics.getDescription().setEnabled(false);
        chartDeviceStatistics.setDrawHoleEnabled(true);
        chartDeviceStatistics.setHoleColor(Color.WHITE);
        chartDeviceStatistics.setTransparentCircleRadius(61f);
        chartDeviceStatistics.setHoleRadius(58f);
        chartDeviceStatistics.setDrawCenterText(true);
        chartDeviceStatistics.setCenterText("设备分布");
        chartDeviceStatistics.setCenterTextSize(16f);
        chartDeviceStatistics.getLegend().setEnabled(true);
        chartDeviceStatistics.getLegend().setTextSize(12f);
        
        chartDeviceStatistics.invalidate();
    }
    
    private void loadAlarmTypeStatisticsData() {
        List<StatisticsData.AlarmTypeData> typeData = generateMockAlarmTypeData();
        
        ArrayList<PieEntry> entries = new ArrayList<>();
        for (StatisticsData.AlarmTypeData data : typeData) {
            entries.add(new PieEntry(data.getAlarmCount(), data.getTypeName()));
        }
        
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(getColorArray());
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        
        PieData pieData = new PieData(dataSet);
        chartTypeStatistics.setData(pieData);
        
        // Customize chart appearance
        chartTypeStatistics.getDescription().setEnabled(false);
        chartTypeStatistics.setDrawHoleEnabled(true);
        chartTypeStatistics.setHoleColor(Color.WHITE);
        chartTypeStatistics.setTransparentCircleRadius(61f);
        chartTypeStatistics.setHoleRadius(58f);
        chartTypeStatistics.setDrawCenterText(true);
        chartTypeStatistics.setCenterText("类型分布");
        chartTypeStatistics.setCenterTextSize(16f);
        chartTypeStatistics.getLegend().setEnabled(true);
        chartTypeStatistics.getLegend().setTextSize(12f);
        
        chartTypeStatistics.invalidate();
    }
    
    private StatisticsData generateMockStatisticsData() {
        Random random = new Random();
        return new StatisticsData(
            random.nextInt(50) + 20,  // total alarms
            random.nextInt(40) + 10,  // processed alarms
            random.nextInt(20) + 10,  // total devices
            random.nextInt(15) + 5    // online devices
        );
    }
    
    private List<StatisticsData.AlarmTrendData> generateMockAlarmTrendData(TimeSpan timeSpan) {
        List<StatisticsData.AlarmTrendData> data = new ArrayList<>();
        Random random = new Random();
        
        switch (timeSpan) {
            case ONE_HOUR:
                for (int i = 0; i < 12; i++) {
                    data.add(new StatisticsData.AlarmTrendData(
                        i * 5 + "分钟", random.nextInt(10) + 1));
                }
                break;
            case ONE_WEEK:
                String[] days = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
                for (String day : days) {
                    data.add(new StatisticsData.AlarmTrendData(day, random.nextInt(50) + 10));
                }
                break;
            case ONE_MONTH:
                for (int i = 1; i <= 30; i++) {
                    data.add(new StatisticsData.AlarmTrendData(i + "日", random.nextInt(30) + 5));
                }
                break;
            case ONE_YEAR:
                String[] months = {"1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"};
                for (String month : months) {
                    data.add(new StatisticsData.AlarmTrendData(month, random.nextInt(100) + 20));
                }
                break;
        }
        
        return data;
    }
    
    private List<StatisticsData.SceneAlarmData> generateMockSceneData() {
        List<StatisticsData.SceneAlarmData> data = new ArrayList<>();
        Random random = new Random();
        
        data.add(new StatisticsData.SceneAlarmData("生产区域", random.nextInt(30) + 10));
        data.add(new StatisticsData.SceneAlarmData("仓储区域", random.nextInt(25) + 8));
        data.add(new StatisticsData.SceneAlarmData("办公区域", random.nextInt(15) + 5));
        data.add(new StatisticsData.SceneAlarmData("公共区域", random.nextInt(20) + 7));
        data.add(new StatisticsData.SceneAlarmData("危险区域", random.nextInt(35) + 15));
        
        return data;
    }
    
    private List<StatisticsData.DeviceAlarmData> generateMockDeviceData() {
        List<StatisticsData.DeviceAlarmData> data = new ArrayList<>();
        Random random = new Random();
        
        data.add(new StatisticsData.DeviceAlarmData("摄像头001", random.nextInt(20) + 5));
        data.add(new StatisticsData.DeviceAlarmData("传感器002", random.nextInt(25) + 8));
        data.add(new StatisticsData.DeviceAlarmData("报警器003", random.nextInt(15) + 3));
        data.add(new StatisticsData.DeviceAlarmData("监控器004", random.nextInt(18) + 6));
        data.add(new StatisticsData.DeviceAlarmData("探测器005", random.nextInt(22) + 7));
        
        return data;
    }
    
    private List<StatisticsData.AlarmTypeData> generateMockAlarmTypeData() {
        List<StatisticsData.AlarmTypeData> data = new ArrayList<>();
        Random random = new Random();
        
        data.add(new StatisticsData.AlarmTypeData("入侵报警", random.nextInt(25) + 10));
        data.add(new StatisticsData.AlarmTypeData("火灾报警", random.nextInt(20) + 8));
        data.add(new StatisticsData.AlarmTypeData("设备故障", random.nextInt(30) + 12));
        data.add(new StatisticsData.AlarmTypeData("环境异常", random.nextInt(15) + 5));
        data.add(new StatisticsData.AlarmTypeData("其他报警", random.nextInt(18) + 7));
        
        return data;
    }
    
    private int[] getColorArray() {
        return new int[] {
            getResources().getColor(R.color.primary_blue),
            getResources().getColor(R.color.primary_green),
            getResources().getColor(R.color.primary_orange),
            getResources().getColor(R.color.primary_red),
            getResources().getColor(R.color.primary_purple)
        };
    }
}
