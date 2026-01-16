package com.example.mineguard.home;

import static com.example.mineguard.MyApplication.globalIP;
import static com.example.mineguard.MyApplication.token;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 设备报警统计管理器 - 垂直柱状图
 */
public class DeviceAlarmStatisticsManager {
    private static final String TAG = "DeviceAlarmStatisticsManager";

    public enum TimeSpan {
        ONE_DAY, ONE_WEEK, ONE_MONTH
    }

    private Context context;
    private BarChart chartDeviceAlarm;
    private TextView tvDeviceTimeOneDay;
    private TextView tvDeviceTimeOneWeek;
    private TextView tvDeviceTimeOneMonth;
    private JSONArray apiDeviceData;
    private TimeSpan currentTimeSpan = TimeSpan.ONE_DAY;

    public DeviceAlarmStatisticsManager(Context context) {
        this.context = context;
    }

    /**
     * 绑定视图
     */
    public void bindViews(BarChart chart, TextView timeOneDay, TextView timeOneWeek, TextView timeOneMonth) {
        this.chartDeviceAlarm = chart;
        this.tvDeviceTimeOneDay = timeOneDay;
        this.tvDeviceTimeOneWeek = timeOneWeek;
        this.tvDeviceTimeOneMonth = timeOneMonth;
    }

    /**
     * 初始化图表
     */
    public void setupChart() {
        if (chartDeviceAlarm == null) return;

        chartDeviceAlarm.setDrawBarShadow(false);
        chartDeviceAlarm.setDrawValueAboveBar(true);
        chartDeviceAlarm.getDescription().setEnabled(false);
        chartDeviceAlarm.setMaxVisibleValueCount(60);
        chartDeviceAlarm.setPinchZoom(false);
        chartDeviceAlarm.setScaleEnabled(false);
        chartDeviceAlarm.setDoubleTapToZoomEnabled(false);
        chartDeviceAlarm.getLegend().setEnabled(false);

        // X轴配置（设备名称）
        XAxis xAxis = chartDeviceAlarm.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(11f);
        xAxis.setTextColor(Color.parseColor("#333333"));
        xAxis.setLabelRotationAngle(-45f); // 旋转标签以避免重叠

        // 左侧Y轴配置（数量）
        YAxis leftAxis = chartDeviceAlarm.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#EEEEEE"));
        leftAxis.setAxisLineColor(Color.parseColor("#CCCCCC"));
        leftAxis.setTextSize(12f);
        leftAxis.setTextColor(Color.parseColor("#666666"));
        leftAxis.setGranularity(1f);

        // 右侧Y轴禁用
        chartDeviceAlarm.getAxisRight().setEnabled(false);

        chartDeviceAlarm.setFitBars(true);
        chartDeviceAlarm.animateY(1000);
    }

    /**
     * 设置时间跨度监听器
     */
    public void setTimeSpanListeners() {
        if (tvDeviceTimeOneDay != null) {
            tvDeviceTimeOneDay.setOnClickListener(v -> selectTimeSpan(TimeSpan.ONE_DAY));
        }
        if (tvDeviceTimeOneWeek != null) {
            tvDeviceTimeOneWeek.setOnClickListener(v -> selectTimeSpan(TimeSpan.ONE_WEEK));
        }
        if (tvDeviceTimeOneMonth != null) {
            tvDeviceTimeOneMonth.setOnClickListener(v -> selectTimeSpan(TimeSpan.ONE_MONTH));
        }
    }

    /**
     * 设置默认选中样式
     */
    public void setDefaultTimeSpan() {
        setTimeSpanStyle(TimeSpan.ONE_DAY);
    }

    /**
     * 选择时间跨度
     */
    private void selectTimeSpan(TimeSpan timeSpan) {
        currentTimeSpan = timeSpan;
        resetTimeSpanStyles();

        TextView selectedView = null;
        switch (timeSpan) {
            case ONE_DAY:
                selectedView = tvDeviceTimeOneDay;
                break;
            case ONE_WEEK:
                selectedView = tvDeviceTimeOneWeek;
                break;
            case ONE_MONTH:
                selectedView = tvDeviceTimeOneMonth;
                break;
        }

        if (selectedView != null) {
            selectedView.setTextColor(context.getResources().getColor(com.example.mineguard.R.color.primary_blue));
            selectedView.setBackground(context.getResources().getDrawable(com.example.mineguard.R.drawable.time_span_selected));
        }

        String baseUrl = "http://" + globalIP + ":80/prod-api";
        String timeParam = getTimeParam(timeSpan);
        fetchStatisticsData(baseUrl, timeParam);
    }

    /**
     * 仅设置样式，不触发API请求
     */
    private void setTimeSpanStyle(TimeSpan timeSpan) {
        currentTimeSpan = timeSpan;
        resetTimeSpanStyles();

        TextView selectedView = null;
        switch (timeSpan) {
            case ONE_DAY:
                selectedView = tvDeviceTimeOneDay;
                break;
            case ONE_WEEK:
                selectedView = tvDeviceTimeOneWeek;
                break;
            case ONE_MONTH:
                selectedView = tvDeviceTimeOneMonth;
                break;
        }

        if (selectedView != null) {
            selectedView.setTextColor(context.getResources().getColor(com.example.mineguard.R.color.primary_blue));
            selectedView.setBackground(context.getResources().getDrawable(com.example.mineguard.R.drawable.time_span_selected));
        }
    }

    private void resetTimeSpanStyles() {
        TextView[] timeSpanViews = {tvDeviceTimeOneDay, tvDeviceTimeOneWeek, tvDeviceTimeOneMonth};
        for (TextView view : timeSpanViews) {
            if (view != null) {
                view.setTextColor(context.getResources().getColor(com.example.mineguard.R.color.text_secondary));
                view.setBackground(null);
            }
        }
    }

    private String getTimeParam(TimeSpan timeSpan) {
        switch (timeSpan) {
            case ONE_DAY:
                return "hour24";
            case ONE_WEEK:
                return "day7";
            case ONE_MONTH:
                return "day30";
            default:
                return "hour24";
        }
    }

    /**
     * 从API获取统计数据
     */
    public void fetchStatisticsData(String baseUrl, String statisticTime) {
        String url = baseUrl + "/api/get/index/dev/alarm/count/data/";
        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("statistic_time", statisticTime)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "设备报警统计API请求失败: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() -> {
                    loadMockData();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "设备报警统计API响应，状态码: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    Log.d(TAG, "设备报警统计响应: " + jsonData);
                    try {
                        JSONObject jsonObject = new JSONObject(jsonData);
                        if (jsonObject.getBoolean("success_status")) {
                            JSONArray data = jsonObject.getJSONArray("data");
                            apiDeviceData = data;
                            new Handler(Looper.getMainLooper()).post(() -> {
                                loadData();
                            });
                            return;
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON解析错误: " + e.getMessage());
                    }
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    loadMockData();
                });
            }
        });
    }

    /**
     * 加载并显示设备报警统计数据
     */
    private void loadData() {
        List<AlarmRankingData> deviceData;

        if (apiDeviceData != null) {
            deviceData = generateDeviceDataFromApi(apiDeviceData);
            if (deviceData.isEmpty()) {
                loadMockData();
                return;
            }
        } else {
            loadMockData();
            return;
        }

        updateChart(deviceData);
    }

    /**
     * 加载模拟数据（离线时使用）
     */
    private void loadMockData() {
        List<AlarmRankingData> mockData = new ArrayList<>();

        switch (currentTimeSpan) {
            case ONE_DAY:
                mockData.add(new AlarmRankingData("摄像头01", 15));
                mockData.add(new AlarmRankingData("摄像头02", 10));
                mockData.add(new AlarmRankingData("摄像头03", 6));
                mockData.add(new AlarmRankingData("摄像头04", 3));
                break;
            case ONE_WEEK:
                mockData.add(new AlarmRankingData("摄像头01", 52));
                mockData.add(new AlarmRankingData("摄像头02", 38));
                mockData.add(new AlarmRankingData("摄像头03", 25));
                mockData.add(new AlarmRankingData("摄像头04", 18));
                mockData.add(new AlarmRankingData("摄像头05", 12));
                break;
            case ONE_MONTH:
                mockData.add(new AlarmRankingData("摄像头01", 185));
                mockData.add(new AlarmRankingData("摄像头02", 142));
                mockData.add(new AlarmRankingData("摄像头03", 98));
                mockData.add(new AlarmRankingData("摄像头04", 76));
                mockData.add(new AlarmRankingData("摄像头05", 54));
                mockData.add(new AlarmRankingData("摄像头06", 35));
                break;
        }

        updateChart(mockData);
        Log.d(TAG, "使用模拟设备数据 - 数据条数:" + mockData.size());
    }

    /**
     * 从API数据生成设备统计，按报警数量降序排列（最大值在左侧）
     */
    private List<AlarmRankingData> generateDeviceDataFromApi(JSONArray deviceData) {
        List<AlarmRankingData> data = new ArrayList<>();

        try {
            for (int i = 0; i < deviceData.length(); i++) {
                JSONObject deviceObj = deviceData.getJSONObject(i);
                String deviceName = deviceObj.getString("dev_name");
                int count = deviceObj.getInt("count");
                data.add(new AlarmRankingData(deviceName, count));
            }

            // 按报警数量降序排列（最大值在第一个，即最左侧）
            Collections.sort(data, new Comparator<AlarmRankingData>() {
                @Override
                public int compare(AlarmRankingData o1, AlarmRankingData o2) {
                    return Integer.compare(o2.getValue(), o1.getValue());
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "解析设备统计数据错误: " + e.getMessage());
        }

        return data;
    }

    /**
     * 更新垂直柱状图
     */
    private void updateChart(List<AlarmRankingData> rankingData) {
        if (chartDeviceAlarm == null) return;

        if (rankingData == null || rankingData.isEmpty()) {
            chartDeviceAlarm.clear();
            return;
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> deviceNames = new ArrayList<>();

        for (int i = 0; i < rankingData.size(); i++) {
            AlarmRankingData item = rankingData.get(i);
            entries.add(new BarEntry(i, item.getValue()));
            deviceNames.add(item.getName());
        }

        BarDataSet set1;
        if (chartDeviceAlarm.getData() != null &&
                chartDeviceAlarm.getData().getDataSetCount() > 0) {
            set1 = (BarDataSet) chartDeviceAlarm.getData().getDataSetByIndex(0);
            set1.setValues(entries);
            chartDeviceAlarm.getData().notifyDataChanged();
            chartDeviceAlarm.notifyDataSetChanged();
        } else {
            set1 = new BarDataSet(entries, "设备报警");
            set1.setColor(Color.parseColor("#10B981"));
            set1.setValueTextSize(12f);
            set1.setValueTextColor(Color.parseColor("#333333"));
            // 启用数值显示
            set1.setDrawValues(true);
            set1.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.valueOf((int) value);
                }
            });

            ArrayList<IBarDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1);

            BarData data = new BarData(dataSets);
            data.setBarWidth(0.5f);
            data.setDrawValues(true);
            chartDeviceAlarm.setData(data);
        }

        chartDeviceAlarm.getXAxis().setValueFormatter(new IndexAxisValueFormatter(deviceNames));
        chartDeviceAlarm.getXAxis().setLabelCount(deviceNames.size());

        chartDeviceAlarm.invalidate();
    }

    /**
     * 获取当前时间跨度
     */
    public TimeSpan getCurrentTimeSpan() {
        return currentTimeSpan;
    }
}
