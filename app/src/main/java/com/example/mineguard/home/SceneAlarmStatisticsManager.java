package com.example.mineguard.home;

import static com.example.mineguard.MyApplication.globalIP;
import static com.example.mineguard.MyApplication.token;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;
import com.github.mikephil.charting.charts.HorizontalBarChart;
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
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 场景报警统计管理器
 */
public class SceneAlarmStatisticsManager {
    private static final String TAG = "SceneAlarmStatisticsManager";

    public enum TimeSpan {
        ONE_DAY, ONE_WEEK, ONE_MONTH
    }

    private Context context;
    private HorizontalBarChart chartAlarmRanking;
    private TextView tvRankingTimeOneDay;
    private TextView tvRankingTimeOneWeek;
    private TextView tvRankingTimeOneMonth;
    private JSONObject apiWeekTop;
    private TimeSpan currentTimeSpan = TimeSpan.ONE_DAY;

    public SceneAlarmStatisticsManager(Context context) {
        this.context = context;
    }

    /**
     * 绑定视图
     */
    public void bindViews(HorizontalBarChart chart, TextView timeOneDay, TextView timeOneWeek, TextView timeOneMonth) {
        this.chartAlarmRanking = chart;
        this.tvRankingTimeOneDay = timeOneDay;
        this.tvRankingTimeOneWeek = timeOneWeek;
        this.tvRankingTimeOneMonth = timeOneMonth;
    }

    /**
     * 初始化图表
     */
    public void setupChart() {
        if (chartAlarmRanking == null) return;

        chartAlarmRanking.setDrawBarShadow(false);
        chartAlarmRanking.setDrawValueAboveBar(true);
        chartAlarmRanking.getDescription().setEnabled(false);
        chartAlarmRanking.setMaxVisibleValueCount(60);
        chartAlarmRanking.setPinchZoom(false);
        chartAlarmRanking.setScaleEnabled(false);
        chartAlarmRanking.setDoubleTapToZoomEnabled(false);
        chartAlarmRanking.getLegend().setEnabled(false);

        XAxis xAxis = chartAlarmRanking.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(13f);
        xAxis.setTextColor(Color.parseColor("#333333"));

        chartAlarmRanking.getAxisLeft().setDrawAxisLine(false);
        chartAlarmRanking.getAxisLeft().setDrawGridLines(false);
        chartAlarmRanking.getAxisLeft().setDrawLabels(false);
        chartAlarmRanking.getAxisRight().setEnabled(false);
        chartAlarmRanking.setFitBars(true);
        chartAlarmRanking.animateY(1000);
    }

    /**
     * 设置时间跨度监听器
     */
    public void setTimeSpanListeners() {
        if (tvRankingTimeOneDay != null) {
            tvRankingTimeOneDay.setOnClickListener(v -> selectTimeSpan(TimeSpan.ONE_DAY));
        }
        if (tvRankingTimeOneWeek != null) {
            tvRankingTimeOneWeek.setOnClickListener(v -> selectTimeSpan(TimeSpan.ONE_WEEK));
        }
        if (tvRankingTimeOneMonth != null) {
            tvRankingTimeOneMonth.setOnClickListener(v -> selectTimeSpan(TimeSpan.ONE_MONTH));
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
                selectedView = tvRankingTimeOneDay;
                break;
            case ONE_WEEK:
                selectedView = tvRankingTimeOneWeek;
                break;
            case ONE_MONTH:
                selectedView = tvRankingTimeOneMonth;
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
                selectedView = tvRankingTimeOneDay;
                break;
            case ONE_WEEK:
                selectedView = tvRankingTimeOneWeek;
                break;
            case ONE_MONTH:
                selectedView = tvRankingTimeOneMonth;
                break;
        }

        if (selectedView != null) {
            selectedView.setTextColor(context.getResources().getColor(com.example.mineguard.R.color.primary_blue));
            selectedView.setBackground(context.getResources().getDrawable(com.example.mineguard.R.drawable.time_span_selected));
        }
    }

    private void resetTimeSpanStyles() {
        TextView[] timeSpanViews = {tvRankingTimeOneDay, tvRankingTimeOneWeek, tvRankingTimeOneMonth};
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
        String url = baseUrl + "/api/get/index/scene/data/";
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
                Log.e(TAG, "场景统计API请求失败: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() -> {
                    loadMockData();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "场景统计API响应，状态码: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    Log.d(TAG, "场景统计响应: " + jsonData);
                    try {
                        JSONObject jsonObject = new JSONObject(jsonData);
                        if (jsonObject.getBoolean("success_status")) {
                            JSONObject data = jsonObject.getJSONObject("data");
                            apiWeekTop = data;
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
     * 加载并显示场景报警统计数据
     */
    private void loadData() {
        List<AlarmRankingData> sceneData;

        if (apiWeekTop != null) {
            sceneData = generateSceneDataFromApi(apiWeekTop);
            if (sceneData.isEmpty()) {
                loadMockData();
                return;
            }
        } else {
            loadMockData();
            return;
        }

        updateChart(sceneData);
    }

    /**
     * 加载模拟数据（离线时使用）
     */
    private void loadMockData() {
        List<AlarmRankingData> mockData = new ArrayList<>();

        switch (currentTimeSpan) {
            case ONE_DAY:
                mockData.add(new AlarmRankingData("主井口", 12));
                mockData.add(new AlarmRankingData("副井口", 8));
                mockData.add(new AlarmRankingData("运输大巷", 5));
                mockData.add(new AlarmRankingData("采煤工作面", 3));
                break;
            case ONE_WEEK:
                mockData.add(new AlarmRankingData("主井口", 45));
                mockData.add(new AlarmRankingData("副井口", 32));
                mockData.add(new AlarmRankingData("运输大巷", 28));
                mockData.add(new AlarmRankingData("采煤工作面", 21));
                mockData.add(new AlarmRankingData("掘进工作面", 15));
                break;
            case ONE_MONTH:
                mockData.add(new AlarmRankingData("主井口", 168));
                mockData.add(new AlarmRankingData("副井口", 125));
                mockData.add(new AlarmRankingData("运输大巷", 98));
                mockData.add(new AlarmRankingData("采煤工作面", 76));
                mockData.add(new AlarmRankingData("掘进工作面", 54));
                mockData.add(new AlarmRankingData("变电所", 32));
                break;
        }

        updateChart(mockData);
        Log.d(TAG, "使用模拟场景数据 - 数据条数:" + mockData.size());
    }

    /**
     * 从API数据生成场景统计
     */
    private List<AlarmRankingData> generateSceneDataFromApi(JSONObject sceneData) {
        List<AlarmRankingData> data = new ArrayList<>();

        try {
            if (sceneData.has("res_list")) {
                JSONArray resList = sceneData.getJSONArray("res_list");
                for (int i = 0; i < resList.length(); i++) {
                    JSONObject sceneObj = resList.getJSONObject(i);
                    String sceneName = sceneObj.getString("scene_name");
                    int count = sceneObj.getInt("count");
                    data.add(new AlarmRankingData(sceneName, count));
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "解析场景统计数据错误: " + e.getMessage());
        }

        return data;
    }

    /**
     * 更新水平条形图
     */
    private void updateChart(List<AlarmRankingData> rankingData) {
        if (chartAlarmRanking == null) return;

        if (rankingData == null || rankingData.isEmpty()) {
            chartAlarmRanking.clear();
            return;
        }

        List<AlarmRankingData> chartDataList = new ArrayList<>(rankingData);
        Collections.reverse(chartDataList);

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> sceneNames = new ArrayList<>();

        for (int i = 0; i < chartDataList.size(); i++) {
            AlarmRankingData item = chartDataList.get(i);
            entries.add(new BarEntry(i, item.getValue()));
            sceneNames.add(item.getName());
        }

        int itemHeightDp = 60;
        int totalHeightDp = 40 + (chartDataList.size() * itemHeightDp);

        ViewGroup.LayoutParams params = chartAlarmRanking.getLayoutParams();
        params.height = dpToPx(totalHeightDp);
        chartAlarmRanking.setLayoutParams(params);

        BarDataSet set1;
        if (chartAlarmRanking.getData() != null &&
                chartAlarmRanking.getData().getDataSetCount() > 0) {
            set1 = (BarDataSet) chartAlarmRanking.getData().getDataSetByIndex(0);
            set1.setValues(entries);
            chartAlarmRanking.getData().notifyDataChanged();
            chartAlarmRanking.notifyDataSetChanged();
        } else {
            set1 = new BarDataSet(entries, "场景报警");
            set1.setColor(Color.parseColor("#F97316"));
            set1.setValueTextSize(12f);
            set1.setValueTextColor(Color.parseColor("#666666"));
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
            chartAlarmRanking.setData(data);
        }

        chartAlarmRanking.getXAxis().setValueFormatter(new IndexAxisValueFormatter(sceneNames));
        chartAlarmRanking.getXAxis().setLabelCount(sceneNames.size());

        chartAlarmRanking.invalidate();
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * 获取当前时间跨度
     */
    public TimeSpan getCurrentTimeSpan() {
        return currentTimeSpan;
    }
}
