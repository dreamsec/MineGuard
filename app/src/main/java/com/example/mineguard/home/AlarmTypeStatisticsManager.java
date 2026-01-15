package com.example.mineguard.home;

import static com.example.mineguard.MyApplication.globalIP;
import static com.example.mineguard.MyApplication.token;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import com.example.mineguard.data.StatisticsData;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 报警类型统计管理器
 */
public class AlarmTypeStatisticsManager {
    private static final String TAG = "AlarmTypeStatisticsManager";

    public enum TimeSpan {
        ONE_DAY, ONE_WEEK, ONE_MONTH
    }

    private Context context;
    private PieChart chartAlarmType;
    private TextView tvAlarmTypeTitle;
    private TextView tvAlarmTypeTimeOneDay;
    private TextView tvAlarmTypeTimeOneWeek;
    private TextView tvAlarmTypeTimeOneMonth;
    private JSONObject apiAlarmTypeData;
    private TimeSpan currentTimeSpan = TimeSpan.ONE_DAY;

    public AlarmTypeStatisticsManager(Context context) {
        this.context = context;
    }

    /**
     * 绑定视图
     */
    public void bindViews(PieChart chart, TextView title, TextView timeOneDay, TextView timeOneWeek, TextView timeOneMonth) {
        this.chartAlarmType = chart;
        this.tvAlarmTypeTitle = title;
        this.tvAlarmTypeTimeOneDay = timeOneDay;
        this.tvAlarmTypeTimeOneWeek = timeOneWeek;
        this.tvAlarmTypeTimeOneMonth = timeOneMonth;
    }

    /**
     * 设置时间跨度监听器
     */
    public void setTimeSpanListeners() {
        if (tvAlarmTypeTimeOneDay != null) {
            tvAlarmTypeTimeOneDay.setOnClickListener(v -> selectTimeSpan(TimeSpan.ONE_DAY));
        }
        if (tvAlarmTypeTimeOneWeek != null) {
            tvAlarmTypeTimeOneWeek.setOnClickListener(v -> selectTimeSpan(TimeSpan.ONE_WEEK));
        }
        if (tvAlarmTypeTimeOneMonth != null) {
            tvAlarmTypeTimeOneMonth.setOnClickListener(v -> selectTimeSpan(TimeSpan.ONE_MONTH));
        }
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
                selectedView = tvAlarmTypeTimeOneDay;
                break;
            case ONE_WEEK:
                selectedView = tvAlarmTypeTimeOneWeek;
                break;
            case ONE_MONTH:
                selectedView = tvAlarmTypeTimeOneMonth;
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

    private void resetTimeSpanStyles() {
        TextView[] timeSpanViews = {tvAlarmTypeTimeOneDay, tvAlarmTypeTimeOneWeek, tvAlarmTypeTimeOneMonth};
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
        String url = baseUrl + "/api/get/index/alarm/detect/target/data/";
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
                Log.e(TAG, "报警类型统计API请求失败: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() -> {
                    loadMockData();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "报警类型统计API响应，状态码: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    Log.d(TAG, "报警类型统计响应: " + jsonData);
                    try {
                        JSONObject jsonObject = new JSONObject(jsonData);
                        if (jsonObject.getBoolean("success_status")) {
                            JSONObject data = jsonObject.getJSONObject("data");
                            apiAlarmTypeData = data;
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
     * 加载并显示报警类型统计数据
     */
    private void loadData() {
        List<StatisticsData.AlarmTypeData> typeData;

        if (apiAlarmTypeData != null) {
            typeData = generateAlarmTypeDataFromApi(apiAlarmTypeData);
            if (typeData.isEmpty()) {
                loadMockData();
                return;
            }
        } else {
            loadMockData();
            return;
        }

        updateChart(typeData);
    }

    /**
     * 从API数据生成报警类型统计
     */
    private List<StatisticsData.AlarmTypeData> generateAlarmTypeDataFromApi(JSONObject alarmTypeData) {
        List<StatisticsData.AlarmTypeData> data = new ArrayList<>();

        try {
            if (alarmTypeData.has("data")) {
                JSONArray dataArray = alarmTypeData.getJSONArray("data");
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject item = dataArray.getJSONObject(i);
                    String typeName = item.getString("item_name");
                    int count = item.getInt("count");
                    data.add(new StatisticsData.AlarmTypeData(typeName, count));
                    Log.d(TAG, "添加报警类型: " + typeName + " = " + count);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "解析报警类型数据失败: " + e.getMessage());
        }

        return data;
    }

    /**
     * 加载模拟数据（离线时使用）
     */
    private void loadMockData() {
        List<StatisticsData.AlarmTypeData> typeData = new ArrayList<>();

        switch (currentTimeSpan) {
            case ONE_DAY:
                typeData.add(new StatisticsData.AlarmTypeData("余煤检测", 8));
                typeData.add(new StatisticsData.AlarmTypeData("人员入侵", 5));
                typeData.add(new StatisticsData.AlarmTypeData("设备故障", 3));
                typeData.add(new StatisticsData.AlarmTypeData("烟雾检测", 2));
                break;
            case ONE_WEEK:
                typeData.add(new StatisticsData.AlarmTypeData("余煤检测", 45));
                typeData.add(new StatisticsData.AlarmTypeData("人员入侵", 32));
                typeData.add(new StatisticsData.AlarmTypeData("设备故障", 21));
                typeData.add(new StatisticsData.AlarmTypeData("烟雾检测", 15));
                typeData.add(new StatisticsData.AlarmTypeData("温度异常", 8));
                break;
            case ONE_MONTH:
                typeData.add(new StatisticsData.AlarmTypeData("余煤检测", 168));
                typeData.add(new StatisticsData.AlarmTypeData("人员入侵", 125));
                typeData.add(new StatisticsData.AlarmTypeData("设备故障", 98));
                typeData.add(new StatisticsData.AlarmTypeData("烟雾检测", 76));
                typeData.add(new StatisticsData.AlarmTypeData("温度异常", 54));
                typeData.add(new StatisticsData.AlarmTypeData("未佩戴安全帽", 32));
                break;
        }

        Log.d(TAG, "使用模拟报警类型数据 - 数据条数:" + typeData.size());
        updateChart(typeData);
    }

    /**
     * 更新报警类型饼图
     */
    private void updateChart(List<StatisticsData.AlarmTypeData> typeData) {
        if (typeData.isEmpty()) {
            Log.d(TAG, "没有报警类型数据，添加默认项");
            typeData.add(new StatisticsData.AlarmTypeData("暂无报警", 0));
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (StatisticsData.AlarmTypeData data : typeData) {
            entries.add(new PieEntry(data.getAlarmCount(), data.getTypeName()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(getColorArray());
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueLinePart1OffsetPercentage(80.f);
        dataSet.setValueLinePart1Length(0.3f);
        dataSet.setValueLinePart2Length(0.4f);
        dataSet.setValueLineWidth(5f);
        dataSet.setValueLineColor(Color.GRAY);
        dataSet.setValueTextSize(20f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueFormatter(new PercentFormatter(chartAlarmType));

        PieData pieData = new PieData(dataSet);
        chartAlarmType.setData(pieData);
        chartAlarmType.setUsePercentValues(true);
        chartAlarmType.getDescription().setEnabled(false);
        chartAlarmType.setExtraOffsets(20.f, 0.f, 20.f, 0.f);
        chartAlarmType.setDrawHoleEnabled(true);
        chartAlarmType.setHoleColor(Color.WHITE);
        chartAlarmType.setTransparentCircleColor(Color.WHITE);
        chartAlarmType.setTransparentCircleAlpha(110);
        chartAlarmType.setHoleRadius(45f);
        chartAlarmType.setTransparentCircleRadius(55f);
        chartAlarmType.setDrawCenterText(true);
        chartAlarmType.setCenterText(getCenterTextForTimeSpan(currentTimeSpan));
        chartAlarmType.setCenterTextSize(18f);
        chartAlarmType.setCenterTextColor(Color.GRAY);
        chartAlarmType.setDrawEntryLabels(false);

        Legend l = chartAlarmType.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setXEntrySpace(7f);
        l.setYEntrySpace(0f);
        l.setYOffset(10f);
        l.setTextSize(11f);
        l.setTextColor(Color.parseColor("#666666"));
        l.setWordWrapEnabled(true);

        chartAlarmType.invalidate();
    }

    private int[] getColorArray() {
        return new int[]{
                context.getResources().getColor(com.example.mineguard.R.color.primary_blue),
                context.getResources().getColor(com.example.mineguard.R.color.primary_green),
                context.getResources().getColor(com.example.mineguard.R.color.primary_orange),
                context.getResources().getColor(com.example.mineguard.R.color.primary_red),
                context.getResources().getColor(com.example.mineguard.R.color.primary_purple)
        };
    }

    private String getCenterTextForTimeSpan(TimeSpan timeSpan) {
        switch (timeSpan) {
            case ONE_DAY:
                return "24小时\n分布";
            case ONE_WEEK:
                return "本周\n分布";
            case ONE_MONTH:
                return "本月\n分布";
            default:
                return "分布";
        }
    }

    /**
     * 获取当前时间跨度
     */
    public TimeSpan getCurrentTimeSpan() {
        return currentTimeSpan;
    }
}
