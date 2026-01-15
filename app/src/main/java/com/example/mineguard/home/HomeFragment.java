package com.example.mineguard.home;
import static com.example.mineguard.MyApplication.globalIP;
import static com.example.mineguard.MyApplication.token;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import com.example.mineguard.R;
import com.example.mineguard.data.StatisticsData;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import java.util.ArrayList;
import java.util.List;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.OkHttpClient;
import okhttp3.Callback;
import java.io.IOException;
import okhttp3.Call;
import android.widget.Toast;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import java.util.Collections;
import okhttp3.FormBody;
import okhttp3.RequestBody;

public class HomeFragment extends Fragment {

    //今日概览成员变量
    private JSONObject platformStats; // 存储平台统计数据
    private TextView tvProcessedPercent;
    private ProgressBar pbRate;
    private TextView tvTotalAlarmsHeader;
    private TextView tvUntreatedAlarmsHeader;
    private TextView tvTotalDevicesHeader;
    private TextView tvOnlineDevicesHeader;
    private ImageButton btnRefresh; // 刷新按钮

    private TextView tvTypeChartTitle;

    private enum TimeSpan {
        ONE_DAY, ONE_WEEK, ONE_MONTH
    }

    // 排行榜成员变量
    private HorizontalBarChart chartAlarmRanking;  // 报警类型排行榜图表
    private TextView tvRankingTimeOneDay;  // 排行榜24小时切换按钮
    private TextView tvRankingTimeOneWeek;  // 排行榜周切换按钮
    private TextView tvRankingTimeOneMonth;  // 排行榜月切换按钮
    private JSONObject apiWeekTop;  // 存储周排行数据
    private JSONObject apiMonthTop;  // 存储月排行数据
    private TimeSpan currentRankingTimeSpan = TimeSpan.ONE_DAY;  // 当前排行榜时间跨度，默认24小时

    // 报警类型统计成员变量
    private JSONObject apiAlarmTypeData; // 存储报警类型统计数据
    private PieChart chartAlarmType; // 报警类型饼图
    private TextView tvAlarmTypeTitle; // 饼图标题
    private TextView tvAlarmTypeTimeOneDay; // 24小时切换按钮
    private TextView tvAlarmTypeTimeOneWeek; // 周切换按钮
    private TextView tvAlarmTypeTimeOneMonth; // 月切换按钮
    private TimeSpan currentAlarmTypeTimeSpan = TimeSpan.ONE_DAY; // 当前时间跨度，默认24小时
    
    //处理报警数据成员变量
    private JSONObject processingInfo; // 存储处理中报警数据

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    public static class AlarmRankingData {
        String name;
        int value;
        public AlarmRankingData(String name, int value) { this.name = name; this.value = value; }
        public String getName() { return name; }
        public int getValue() { return value; }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次界面可见时自动刷新数据
        fetchStatisticsDataFromApi();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 1. 绑定新UI控件
        initHeaderViews(view);
        initViews(view);
        setupTimeSpanListeners();
        fetchStatisticsDataFromApi();
        return view;
    }

    private void initHeaderViews(View root) {
        View overviewCard = root.findViewById(R.id.card_header_overview);
        tvProcessedPercent = overviewCard.findViewById(R.id.tv_processed_percent);
        pbRate = root.findViewById(R.id.pb_overview_rate);

        // 绑定报警卡片内的控件
        View alarmCard = root.findViewById(R.id.card_header_alarm);
        tvTotalAlarmsHeader = alarmCard.findViewById(R.id.tv_total_alarms_header);
        tvUntreatedAlarmsHeader = alarmCard.findViewById(R.id.tv_untreated_alarms_header);

        // 绑定设备卡片内的控件
        View deviceCard = root.findViewById(R.id.card_header_device);
        tvTotalDevicesHeader = deviceCard.findViewById(R.id.tv_total_devices_header);
        tvOnlineDevicesHeader = deviceCard.findViewById(R.id.tv_online_devices_header);
    }

    private void initViews(View view) {
        // 初始化刷新按钮
        btnRefresh = view.findViewById(R.id.btn_refresh);
        btnRefresh.setOnClickListener(v -> {
            // 点击刷新按钮时重新获取数据
            fetchStatisticsDataFromApi();
        });

        // 初始化场景报警统计图表
        chartAlarmRanking = view.findViewById(R.id.card_bar_statistics).findViewById(R.id.bar_chart);
        tvRankingTimeOneDay = view.findViewById(R.id.card_bar_statistics).findViewById(R.id.tv_time_one_day);
        tvRankingTimeOneWeek = view.findViewById(R.id.card_bar_statistics).findViewById(R.id.tv_time_one_week);
        tvRankingTimeOneMonth = view.findViewById(R.id.card_bar_statistics).findViewById(R.id.tv_time_one_month);
        setupHorizontalChart();

        // 设置默认选中24小时按钮的样式（不触发API请求）
        setRankingTimeSpanStyle(TimeSpan.ONE_DAY);

        // 初始化饼图相关视图
        chartAlarmType = view.findViewById(R.id.card_piechart_WeekMonthAlarmType).findViewById(R.id.pie_chart);
        tvAlarmTypeTitle = view.findViewById(R.id.card_piechart_WeekMonthAlarmType).findViewById(R.id.tv_chart_title);
        tvAlarmTypeTimeOneDay = view.findViewById(R.id.card_piechart_WeekMonthAlarmType).findViewById(R.id.tv_time_one_day);
        tvAlarmTypeTimeOneWeek = view.findViewById(R.id.card_piechart_WeekMonthAlarmType).findViewById(R.id.tv_time_one_week);
        tvAlarmTypeTimeOneMonth = view.findViewById(R.id.card_piechart_WeekMonthAlarmType).findViewById(R.id.tv_time_one_month);

        // 设置图表标题
        TextView tvBarChartTitle = view.findViewById(R.id.card_bar_statistics).findViewById(R.id.tv_chart_title);
        tvBarChartTitle.setText("场景报警统计");
    }

    private void setupTimeSpanListeners() {
        // 场景报警统计时间切换监听器
        tvRankingTimeOneDay.setOnClickListener(v -> selectRankingTimeSpan(TimeSpan.ONE_DAY));
        tvRankingTimeOneWeek.setOnClickListener(v -> selectRankingTimeSpan(TimeSpan.ONE_WEEK));
        tvRankingTimeOneMonth.setOnClickListener(v -> selectRankingTimeSpan(TimeSpan.ONE_MONTH));

        // 报警类型统计时间切换监听器
        tvAlarmTypeTimeOneDay.setOnClickListener(v -> selectAlarmTypeTimeSpan(TimeSpan.ONE_DAY));
        tvAlarmTypeTimeOneWeek.setOnClickListener(v -> selectAlarmTypeTimeSpan(TimeSpan.ONE_WEEK));
        tvAlarmTypeTimeOneMonth.setOnClickListener(v -> selectAlarmTypeTimeSpan(TimeSpan.ONE_MONTH));
    }

    // 仅设置按钮样式，不触发API请求
    private void setRankingTimeSpanStyle(TimeSpan timeSpan) {
        currentRankingTimeSpan = timeSpan;

        // 重置所有时间跨度文本视图样式
        resetRankingTimeSpanStyles();

        // 设置选中样式
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
            selectedView.setTextColor(getResources().getColor(R.color.primary_blue));
            selectedView.setBackground(getResources().getDrawable(R.drawable.time_span_selected));
        }
    }

    private void selectRankingTimeSpan(TimeSpan timeSpan) {
        currentRankingTimeSpan = timeSpan;

        // 重置所有时间跨度文本视图样式
        resetRankingTimeSpanStyles();

        // 设置选中样式
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
            selectedView.setTextColor(getResources().getColor(R.color.primary_blue));
            selectedView.setBackground(getResources().getDrawable(R.drawable.time_span_selected));
        }

        // 重新请求场景统计数据
        String baseUrl = "http://" + globalIP + ":80/prod-api";
        String timeParam;
        switch (currentRankingTimeSpan) {
            case ONE_DAY:
                timeParam = "hour24";
                break;
            case ONE_WEEK:
                timeParam = "day7";
                break;
            case ONE_MONTH:
                timeParam = "day30";
                break;
            default:
                timeParam = "hour24";
        }
        fetchSceneStatisticData(baseUrl, timeParam);
    }

    private void resetRankingTimeSpanStyles() {
        TextView[] timeSpanViews = {tvRankingTimeOneDay, tvRankingTimeOneWeek, tvRankingTimeOneMonth};
        for (TextView view : timeSpanViews) {
            if (view != null) {
                view.setTextColor(getResources().getColor(R.color.text_secondary));
                view.setBackground(null);
            }
        }
    }

    private void selectAlarmTypeTimeSpan(TimeSpan timeSpan) {
        currentAlarmTypeTimeSpan = timeSpan;

        // 重置所有时间跨度文本视图样式
        resetAlarmTypeTimeSpanStyles();

        // 设置选中样式
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
            selectedView.setTextColor(getResources().getColor(R.color.primary_blue));
            selectedView.setBackground(getResources().getDrawable(R.drawable.time_span_selected));
        }

        // 重新请求报警类型统计数据
        String baseUrl = "http://" + globalIP + ":80/prod-api";
        String timeParam;
        switch (currentAlarmTypeTimeSpan) {
            case ONE_DAY:
                timeParam = "hour24";
                break;
            case ONE_WEEK:
                timeParam = "day7";
                break;
            case ONE_MONTH:
                timeParam = "day30";
                break;
            default:
                timeParam = "hour24";
        }
        fetchAlarmTypeDetectData(baseUrl, timeParam);
    }

    private void resetAlarmTypeTimeSpanStyles() {
        TextView[] timeSpanViews = {tvAlarmTypeTimeOneDay, tvAlarmTypeTimeOneWeek, tvAlarmTypeTimeOneMonth};
        for (TextView view : timeSpanViews) {
            if (view != null) {
                view.setTextColor(getResources().getColor(R.color.text_secondary));
                view.setBackground(null);
            }
        }
    }
    private void updateHeaderUI(int totalAlarms, int unprocessedAlarms, int totalDevices, int onlineDevices) {
        // --- 1. 更新今日概览卡片 ---
        // 避免除以0错误
        int processedAlarms = totalAlarms - unprocessedAlarms;
        int percent = 0;
        if (totalAlarms > 0) {
            percent = (int) (((float) processedAlarms / totalAlarms) * 100);
        }

        // 设置百分比 (例如: 85%)
        tvProcessedPercent.setText(percent + "%");
        pbRate.setProgress(percent);

        // --- 2. 更新报警卡片  ---
        tvTotalAlarmsHeader.setText(String.valueOf(totalAlarms));
        tvUntreatedAlarmsHeader.setText(String.valueOf(unprocessedAlarms));

        // --- 3. 更新设备卡片 ---
        tvTotalDevicesHeader.setText(String.valueOf(totalDevices));
        tvOnlineDevicesHeader.setText(String.valueOf(onlineDevices));
    }

    private void fetchStatisticsDataFromApi() {
        Log.d("HomeFragment", "开始API请求...");
        // 使用POST请求
        String baseUrl = "http://" + globalIP + ":80/prod-api";

        // 1. 请求当日数据
        fetchDailyStatisticData(baseUrl);

        // 2. 请求场景报警统计数据（根据当前时间跨度，默认24小时）
        String timeParam;
        switch (currentRankingTimeSpan) {
            case ONE_DAY:
                timeParam = "hour24";
                break;
            case ONE_WEEK:
                timeParam = "day7";
                break;
            case ONE_MONTH:
                timeParam = "day30";
                break;
            default:
                timeParam = "hour24";
        }
        fetchSceneStatisticData(baseUrl, timeParam);

        // 3. 请求报警类型统计数据（根据当前时间跨度，默认24小时）
        String alarmTypeTimeParam;
        switch (currentAlarmTypeTimeSpan) {
            case ONE_DAY:
                alarmTypeTimeParam = "hour24";
                break;
            case ONE_WEEK:
                alarmTypeTimeParam = "day7";
                break;
            case ONE_MONTH:
                alarmTypeTimeParam = "day30";
                break;
            default:
                alarmTypeTimeParam = "hour24";
        }
        fetchAlarmTypeDetectData(baseUrl, alarmTypeTimeParam);
    }

    // 请求当日统计数据
    private void fetchDailyStatisticData(String baseUrl) {
        String url = baseUrl + "/api/get/index/statistic/data/";
        OkHttpClient client = new OkHttpClient();

        // 构建POST请求体
        RequestBody formBody = new FormBody.Builder()
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("HomeFragment", "当日数据API请求失败: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() -> {
                    // 显示离线提示
                    Toast.makeText(getContext(), "网络不可用，显示模拟数据", Toast.LENGTH_LONG).show();
                    // 使用模拟数据
                    loadMockPlatformStatisticsData();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("HomeFragment", "当日数据API响应，状态码: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    Log.d("HomeFragment", "当日数据响应: " + jsonData);
                    try {
                        JSONObject jsonObject = new JSONObject(jsonData);
                        if (jsonObject.getBoolean("success_status")) {
                            JSONObject data = jsonObject.getJSONObject("data");
                            // 保存当日数据
                            platformStats = data;

                            // 在主线程更新UI
                            new Handler(Looper.getMainLooper()).post(() -> {
                                loadPlatformStatisticsData();
                            });
                        }
                    } catch (JSONException e) {
                        Log.e("HomeFragment", "JSON解析错误: " + e.getMessage());
                        // 解析失败时使用模拟数据
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(getContext(), "数据解析失败，显示模拟数据", Toast.LENGTH_SHORT).show();
                            loadMockPlatformStatisticsData();
                        });
                    }
                } else {
                    // 响应失败时使用模拟数据
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getContext(), "服务器响应异常，显示模拟数据", Toast.LENGTH_SHORT).show();
                        loadMockPlatformStatisticsData();
                    });
                }
            }
        });
    }

    // 请求场景报警统计数据
    private void fetchSceneStatisticData(String baseUrl, String statisticTime) {
        String url = baseUrl + "/api/get/index/scene/data/";
        OkHttpClient client = new OkHttpClient();

        // 构建POST请求体
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
                Log.e("HomeFragment", "场景统计API请求失败: " + e.getMessage());
                // 使用模拟数据
                new Handler(Looper.getMainLooper()).post(() -> {
                    loadMockSceneAlarmStatisticsData();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("HomeFragment", "场景统计API响应，状态码: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    Log.d("HomeFragment", "场景统计响应: " + jsonData);
                    try {
                        JSONObject jsonObject = new JSONObject(jsonData);
                        if (jsonObject.getBoolean("success_status")) {
                            JSONObject data = jsonObject.getJSONObject("data");
                            // 保存场景统计数据
                            apiWeekTop = data;

                            // 在主线程更新UI
                            new Handler(Looper.getMainLooper()).post(() -> {
                                loadSceneAlarmStatisticsData();
                            });
                            return;
                        }
                    } catch (JSONException e) {
                        Log.e("HomeFragment", "JSON解析错误: " + e.getMessage());
                    }
                }
                // 响应失败或解析失败时使用模拟数据
                new Handler(Looper.getMainLooper()).post(() -> {
                    loadMockSceneAlarmStatisticsData();
                });
            }
        });
    }

    // 请求报警类型统计数据
    private void fetchAlarmTypeDetectData(String baseUrl, String statisticTime) {
        String url = baseUrl + "/api/get/index/alarm/detect/target/data/";
        OkHttpClient client = new OkHttpClient();

        // 构建POST请求体
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
                Log.e("HomeFragment", "报警类型统计API请求失败: " + e.getMessage());
                // 使用模拟数据
                new Handler(Looper.getMainLooper()).post(() -> {
                    loadMockAlarmTypeData();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("HomeFragment", "报警类型统计API响应，状态码: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    Log.d("HomeFragment", "报警类型统计响应: " + jsonData);
                    try {
                        JSONObject jsonObject = new JSONObject(jsonData);
                        if (jsonObject.getBoolean("success_status")) {
                            JSONObject data = jsonObject.getJSONObject("data");
                            // 保存报警类型统计数据
                            apiAlarmTypeData = data;

                            // 在主线程更新UI
                            new Handler(Looper.getMainLooper()).post(() -> {
                                loadAlarmTypeData();
                            });
                            return;
                        }
                    } catch (JSONException e) {
                        Log.e("HomeFragment", "JSON解析错误: " + e.getMessage());
                    }
                }
                // 响应失败或解析失败时使用模拟数据
                new Handler(Looper.getMainLooper()).post(() -> {
                    loadMockAlarmTypeData();
                });
            }
        });
    }

    private void loadPlatformStatisticsData() {
        if (platformStats != null) {
            try {
                // 根据API文档提取数据
                int totalDevices = platformStats.getInt("dev_sum_count"); // 当前设备总数
                int onlineDevices = platformStats.getInt("dev_online_count"); // 当前在线设备数
                int totalAlarms = platformStats.getInt("alarm_count_today"); // 今日待处理报警数
                int newAlarms = platformStats.getInt("todo_count_today"); // 今日新增报警数

                // 更新UI
                updateHeaderUI(newAlarms, totalAlarms, totalDevices, onlineDevices);

                Log.d("HomeFragment", "平台统计数据更新成功 - 设备总数:" + totalDevices +
                      " 在线设备:" + onlineDevices + " 今日报警:" + totalAlarms + " 新增:" + newAlarms);
            } catch (JSONException e) {
                Log.e("HomeFragment", "解析平台统计数据错误: " + e.getMessage());
                // 发生错误时使用模拟数据
                loadMockPlatformStatisticsData();
            }
        } else {
            // API数据不可用时使用模拟数据
            loadMockPlatformStatisticsData();
        }
    }

    // 加载模拟的平台统计数据（离线时使用）
    private void loadMockPlatformStatisticsData() {
        // 模拟数据
        int totalDevices = 24;
        int onlineDevices = 17;
        int totalAlarms = 36;
        int newAlarms = 8;

        updateHeaderUI(newAlarms, totalAlarms, totalDevices, onlineDevices);
        Log.d("HomeFragment", "使用模拟数据 - 设备总数:" + totalDevices +
              " 在线设备:" + onlineDevices + " 今日报警:" + totalAlarms + " 新增:" + newAlarms);
    }

    // 加载场景报警统计数据
    private void loadSceneAlarmStatisticsData() {
        List<AlarmRankingData> sceneData;

        if (apiWeekTop != null) {
            sceneData = generateSceneDataFromApi(apiWeekTop);
            // 如果API返回空数据，使用模拟数据
            if (sceneData.isEmpty()) {
                loadMockSceneAlarmStatisticsData();
                return;
            }
        } else {
            // API数据不可用时使用模拟数据
            loadMockSceneAlarmStatisticsData();
            return;
        }

        // 更新图表UI
        updateHorizontalBarChart(sceneData);
    }

    // 加载模拟的场景报警统计数据（离线时使用）
    private void loadMockSceneAlarmStatisticsData() {
        List<AlarmRankingData> mockData = new ArrayList<>();

        // 根据当前时间跨度生成不同的模拟数据
        switch (currentRankingTimeSpan) {
            case ONE_DAY:
                // 24小时数据
                mockData.add(new AlarmRankingData("主井口", 12));
                mockData.add(new AlarmRankingData("副井口", 8));
                mockData.add(new AlarmRankingData("运输大巷", 5));
                mockData.add(new AlarmRankingData("采煤工作面", 3));
                break;
            case ONE_WEEK:
                // 7天数据
                mockData.add(new AlarmRankingData("主井口", 45));
                mockData.add(new AlarmRankingData("副井口", 32));
                mockData.add(new AlarmRankingData("运输大巷", 28));
                mockData.add(new AlarmRankingData("采煤工作面", 21));
                mockData.add(new AlarmRankingData("掘进工作面", 15));
                break;
            case ONE_MONTH:
                // 30天数据
                mockData.add(new AlarmRankingData("主井口", 168));
                mockData.add(new AlarmRankingData("副井口", 125));
                mockData.add(new AlarmRankingData("运输大巷", 98));
                mockData.add(new AlarmRankingData("采煤工作面", 76));
                mockData.add(new AlarmRankingData("掘进工作面", 54));
                mockData.add(new AlarmRankingData("变电所", 32));
                break;
        }

        updateHorizontalBarChart(mockData);
        Log.d("HomeFragment", "使用模拟场景数据 - 数据条数:" + mockData.size());
    }

    // 从API数据生成场景统计
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
            Log.e("HomeFragment", "解析场景统计数据错误: " + e.getMessage());
        }

        return data;
    }

    // 更新水平条形图
    private void updateHorizontalBarChart(List<AlarmRankingData> rankingData) {
        if (chartAlarmRanking == null) return;

        if (rankingData == null || rankingData.isEmpty()) {
            chartAlarmRanking.clear();
            return;
        }

        // 反转数据让最大的在最上面
        List<AlarmRankingData> chartDataList = new ArrayList<>(rankingData);
        Collections.reverse(chartDataList);

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> sceneNames = new ArrayList<>();

        for (int i = 0; i < chartDataList.size(); i++) {
            AlarmRankingData item = chartDataList.get(i);
            entries.add(new BarEntry(i, item.getValue()));
            sceneNames.add(item.getName());
        }

        // 动态调整高度
        int itemHeightDp = 60;
        int totalHeightDp = 40 + (chartDataList.size() * itemHeightDp);

        ViewGroup.LayoutParams params = chartAlarmRanking.getLayoutParams();
        params.height = dpToPx(totalHeightDp);
        chartAlarmRanking.setLayoutParams(params);

        // 设置数据
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

        // 设置 X 轴标签
        chartAlarmRanking.getXAxis().setValueFormatter(new IndexAxisValueFormatter(sceneNames));
        chartAlarmRanking.getXAxis().setLabelCount(sceneNames.size());

        chartAlarmRanking.invalidate();
    }

    // 生成报警排行榜数据:根据API返回的week_top、month_top数据
    private void loadAlarmRankingData() {
        // 此方法已废弃，使用loadSceneAlarmStatisticsData代替
        loadSceneAlarmStatisticsData();
    }
    // 辅助方法
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    private void setupHorizontalChart() {
        if (chartAlarmRanking == null) return;

        chartAlarmRanking.setDrawBarShadow(false);
        chartAlarmRanking.setDrawValueAboveBar(true);
        chartAlarmRanking.getDescription().setEnabled(false); // 隐藏描述
        chartAlarmRanking.setMaxVisibleValueCount(60);
        chartAlarmRanking.setPinchZoom(false); // 禁止缩放
        chartAlarmRanking.setScaleEnabled(false);
        chartAlarmRanking.setDoubleTapToZoomEnabled(false);

        // 隐藏图例
        chartAlarmRanking.getLegend().setEnabled(false);

        // 配置 X 轴 (显示类型名称，在左侧)
        XAxis xAxis = chartAlarmRanking.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // 水平图中 BOTTOM 即左侧
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(13f);
        xAxis.setTextColor(Color.parseColor("#333333"));

        // 配置 Y 轴 (隐藏上下侧的线条和数值，只看条形)
        chartAlarmRanking.getAxisLeft().setDrawAxisLine(false);
        chartAlarmRanking.getAxisLeft().setDrawGridLines(false);
        chartAlarmRanking.getAxisLeft().setDrawLabels(false); // 不显示底部数值

        chartAlarmRanking.getAxisRight().setEnabled(false); // 隐藏右侧轴

        chartAlarmRanking.setFitBars(true); // 使条形对齐
        chartAlarmRanking.animateY(1000);   // 进场动画
    }

    // 从API数据生成排行榜数据
    private List<AlarmRankingData> generateApiRankingData(JSONObject rankingData) {
        List<AlarmRankingData> data = new ArrayList<>();

        try {
            if (rankingData.has("alerts")) {
                JSONArray alertsArray = rankingData.getJSONArray("alerts");
                for (int i = 0; i < alertsArray.length(); i++) {
                    JSONObject alertObj = alertsArray.getJSONObject(i);
                    if (alertObj.has("name") && alertObj.has("value")) {
                        String name = alertObj.getString("name");
                        int value = alertObj.getInt("value");
                        data.add(new AlarmRankingData(name, value));
                    }
                }
            }
        } catch (JSONException e) {
            Log.e("HomeFragment", "解析排行榜数据错误: " + e.getMessage());
        }

        return data;
    }

    // 加载报警类型统计数据（使用API返回的数据）
    private void loadAlarmTypeData() {
        List<StatisticsData.AlarmTypeData> typeData;

        if (apiAlarmTypeData != null) {
            typeData = generateAlarmTypeDataFromApi(apiAlarmTypeData);
            // 如果API返回空数据，使用模拟数据
            if (typeData.isEmpty()) {
                loadMockAlarmTypeData();
                return;
            }
        } else {
            // API数据不可用时使用模拟数据
            loadMockAlarmTypeData();
            return;
        }

        // 更新饼图UI
        updateAlarmTypePieChart(typeData);
    }

    // 从API数据生成报警类型统计
    private List<StatisticsData.AlarmTypeData> generateAlarmTypeDataFromApi(JSONObject alarmTypeData) {
        List<StatisticsData.AlarmTypeData> data = new ArrayList<>();

        try {
            // API返回的数据格式：
            // data: [
            //   {item_name: "余煤检测", count: 10},
            //   {item_name: "人员入侵", count: 5}
            // ]
            if (alarmTypeData.has("data")) {
                JSONArray dataArray = alarmTypeData.getJSONArray("data");
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject item = dataArray.getJSONObject(i);
                    String typeName = item.getString("item_name");
                    int count = item.getInt("count");
                    data.add(new StatisticsData.AlarmTypeData(typeName, count));
                    Log.d("HomeFragment", "添加报警类型: " + typeName + " = " + count);
                }
            }
        } catch (JSONException e) {
            Log.e("HomeFragment", "解析报警类型数据失败: " + e.getMessage());
        }

        return data;
    }

    // 更新报警类型饼图
    private void updateAlarmTypePieChart(List<StatisticsData.AlarmTypeData> typeData) {
        // 确保有数据可显示
        if (typeData.isEmpty()) {
            Log.d("HomeFragment", "没有报警类型数据，添加默认项");
            typeData.add(new StatisticsData.AlarmTypeData("暂无报警", 0));
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (StatisticsData.AlarmTypeData data : typeData) {
            // PieEntry 第二个参数是标签，这里传空字符串，因为我们会用图例或外部标签显示
            entries.add(new PieEntry(data.getAlarmCount(), data.getTypeName()));
        }

        // === DataSet 美化设置 ===
        PieDataSet dataSet = new PieDataSet(entries, ""); // 图例标签设为空，因为单独配置

        dataSet.setColors(getColorArray()); // 设置颜色

        // 间距与样式
        dataSet.setSliceSpace(3f); // 切片之间的白缝，看起来更精致
        dataSet.setSelectionShift(5f); // 点击时的突出距离

        // --- 数据线与数值位置设置 (让图表不拥挤的核心) ---
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE); // 标签在外部
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE); // 数值在外部

        // 连接线样式
        dataSet.setValueLinePart1OffsetPercentage(80.f); // 连接线起始位置
        dataSet.setValueLinePart1Length(0.3f); // 第一段线长
        dataSet.setValueLinePart2Length(0.4f); // 第二段线长
        dataSet.setValueLineWidth(5f);         // 线宽
        dataSet.setValueLineColor(Color.GRAY); // 线颜色

        // 数值文字样式
        dataSet.setValueTextSize(20f);
        dataSet.setValueTextColor(Color.BLACK); // 外部文字用黑色，清晰

        // 格式化为百分比
        dataSet.setValueFormatter(new PercentFormatter(chartAlarmType));

        // === Chart 全局设置 ===
        PieData pieData = new PieData(dataSet);
        chartAlarmType.setData(pieData);

        // 启用百分比显示
        chartAlarmType.setUsePercentValues(true);

        // 样式设置
        chartAlarmType.getDescription().setEnabled(false); // 隐藏描述
        chartAlarmType.setExtraOffsets(20.f, 0.f, 20.f, 0.f); // 【关键】设置额外边距，防止外部标签被截断

        // 圈设置
        chartAlarmType.setDrawHoleEnabled(true);
        chartAlarmType.setHoleColor(Color.WHITE);
        chartAlarmType.setTransparentCircleColor(Color.WHITE);
        chartAlarmType.setTransparentCircleAlpha(110);
        chartAlarmType.setHoleRadius(45f); // 中间孔的大小
        chartAlarmType.setTransparentCircleRadius(55f); // 半透明圈大小

        // 中间文字
        chartAlarmType.setDrawCenterText(true);
        chartAlarmType.setCenterText(getCenterTextForTimeSpan(currentAlarmTypeTimeSpan));
        chartAlarmType.setCenterTextSize(18f);
        chartAlarmType.setCenterTextColor(Color.GRAY);

        // 隐藏饼图上的 Entry Label (也就是具体的报警名称文字)，只显示数值百分比
        chartAlarmType.setDrawEntryLabels(false);

        // === 图例 Legend 设置 (美化底部图例) ===
        Legend l = chartAlarmType.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setXEntrySpace(7f); // 图例间距
        l.setYEntrySpace(0f);
        l.setYOffset(10f);    // 底部抬高一点
        l.setTextSize(11f);   // 图例文字大小
        l.setTextColor(Color.parseColor("#666666")); // 灰色文字
        l.setWordWrapEnabled(true); // 允许图例换行，防止太长显示不全

        // 刷新图表
        chartAlarmType.invalidate();
    }

    // 加载模拟的报警类型数据（离线时使用）
    private void loadMockAlarmTypeData() {
        List<StatisticsData.AlarmTypeData> typeData = new ArrayList<>();

        // 根据时间跨度生成模拟数据
        switch (currentAlarmTypeTimeSpan) {
            case ONE_DAY:
                // 24小时数据
                typeData.add(new StatisticsData.AlarmTypeData("余煤检测", 8));
                typeData.add(new StatisticsData.AlarmTypeData("人员入侵", 5));
                typeData.add(new StatisticsData.AlarmTypeData("设备故障", 3));
                typeData.add(new StatisticsData.AlarmTypeData("烟雾检测", 2));
                break;
            case ONE_WEEK:
                // 周数据
                typeData.add(new StatisticsData.AlarmTypeData("余煤检测", 45));
                typeData.add(new StatisticsData.AlarmTypeData("人员入侵", 32));
                typeData.add(new StatisticsData.AlarmTypeData("设备故障", 21));
                typeData.add(new StatisticsData.AlarmTypeData("烟雾检测", 15));
                typeData.add(new StatisticsData.AlarmTypeData("温度异常", 8));
                break;
            case ONE_MONTH:
                // 月数据
                typeData.add(new StatisticsData.AlarmTypeData("余煤检测", 168));
                typeData.add(new StatisticsData.AlarmTypeData("人员入侵", 125));
                typeData.add(new StatisticsData.AlarmTypeData("设备故障", 98));
                typeData.add(new StatisticsData.AlarmTypeData("烟雾检测", 76));
                typeData.add(new StatisticsData.AlarmTypeData("温度异常", 54));
                typeData.add(new StatisticsData.AlarmTypeData("未佩戴安全帽", 32));
                break;
        }

        Log.d("HomeFragment", "使用模拟报警类型数据 - 数据条数:" + typeData.size());
        updateAlarmTypePieChart(typeData);
    }

    // 旧的loadWeekMonthAlarmTypeData方法，已废弃
    private void loadWeekMonthAlarmTypeData() {
        // 此方法已废弃，使用loadAlarmTypeData代替
        loadAlarmTypeData();
    }

    private List<StatisticsData.AlarmTypeData> generateAlarmTypeDataFromWeekMonthTotal(JSONObject weekMonthTotal) {
        List<StatisticsData.AlarmTypeData> typeData = new ArrayList<>();

        try {
            // 获取报警类型列表
            if (weekMonthTotal.has("alert_types")) {
                JSONArray alertTypes = weekMonthTotal.getJSONArray("alert_types");
                // 获取各类型数量映射
                if (weekMonthTotal.has("counts")) {
                    JSONObject counts = weekMonthTotal.getJSONObject("counts");

                    // 遍历所有报警类型
                    for (int i = 0; i < alertTypes.length(); i++) {
                        String type = alertTypes.getString(i);
                        if (counts.has(type)) {
                            int count = counts.getInt(type);
                            typeData.add(new StatisticsData.AlarmTypeData(type, count));
                            Log.d("HomeFragment", "添加统计结果: " + type + " = " + count);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e("HomeFragment", "解析week_total/month_total数据失败: " + e.getMessage());
        }

        return typeData;
    }

    // 生成报警类型统计数据:根据API返回的processing_info数据
//    private void loadAlarmTypeStatisticsData() {
//        List<StatisticsData.AlarmTypeData> typeData;
//
//        // 尝试使用API返回的processing_info数据
//        if (processingInfo != null) {
//            typeData = generateAlarmTypeDataFromApi(processingInfo);
//            Log.d("HomeFragment", "使用API数据生成报警类型统计，类型数量: " + typeData.size());
//        } else {
//            // API数据不可用时直接在方法内生成模拟数据，不再调用独立方法
//            typeData = new ArrayList<>();
//            Random random = new Random();
//            typeData.add(new StatisticsData.AlarmTypeData("入侵报警", random.nextInt(25) + 10));
//            typeData.add(new StatisticsData.AlarmTypeData("火灾报警", random.nextInt(20) + 8));
//            typeData.add(new StatisticsData.AlarmTypeData("设备故障", random.nextInt(30) + 12));
//            typeData.add(new StatisticsData.AlarmTypeData("环境异常", random.nextInt(15) + 5));
//            typeData.add(new StatisticsData.AlarmTypeData("其他报警", random.nextInt(18) + 7));
//            Log.d("HomeFragment", "使用模拟数据生成报警类型统计");
//        }
//
//        // 确保有数据可显示
//        if (typeData.isEmpty()) {
//            Log.d("HomeFragment", "没有报警类型数据，添加默认项");
//            typeData.add(new StatisticsData.AlarmTypeData("暂无报警", 0));
//        }
//
//        ArrayList<PieEntry> entries = new ArrayList<>();
//        for (StatisticsData.AlarmTypeData data : typeData) {
//            entries.add(new PieEntry(data.getAlarmCount(), data.getTypeName()));
//            Log.d("HomeFragment", "添加报警类型: " + data.getTypeName() + ", 数量: " + data.getAlarmCount());
//        }
//
//        PieDataSet dataSet = new PieDataSet(entries, "");
//        dataSet.setColors(getColorArray());
//        dataSet.setValueTextSize(12f);
//        dataSet.setValueTextColor(Color.WHITE);
//
//        // 添加整数值格式化器
//        dataSet.setValueFormatter(new ValueFormatter() {
//            @Override
//            public String getFormattedValue(float value) {
//                return String.valueOf((int) value);
//            }
//        });
//
//        PieData pieData = new PieData(dataSet);
//        chartTypeStatistics.setData(pieData);
//
//        // Customize chart appearance
//        chartTypeStatistics.getDescription().setEnabled(false);
//        chartTypeStatistics.setDrawHoleEnabled(true);
//        chartTypeStatistics.setHoleColor(Color.WHITE);
//        chartTypeStatistics.setTransparentCircleRadius(61f);
//        chartTypeStatistics.setHoleRadius(58f);
//        chartTypeStatistics.setDrawCenterText(true);
//        chartTypeStatistics.setCenterText("类型分布");
//        chartTypeStatistics.setCenterTextSize(16f);
//        chartTypeStatistics.getLegend().setEnabled(true);
//        chartTypeStatistics.getLegend().setTextSize(12f);
//
//        chartTypeStatistics.invalidate();
//    }
//
//    // 生成报警类型统计数据:根据API返回的processing_info数据
//    private List<StatisticsData.AlarmTypeData> generateAlarmTypeDataFromApi(JSONObject processingInfo) {
//        List<StatisticsData.AlarmTypeData> typeData = new ArrayList<>();
//        Map<String, Integer> typeCountMap = new HashMap<>();
//
//        try {
//            // 检查processing_info是否包含list字段
//            if (processingInfo.has("list")) {
//                JSONArray alarmList = processingInfo.getJSONArray("list");
//                Log.d("HomeFragment", "processing_info.list长度: " + alarmList.length());
//
//                // 统计每种报警类型的数量
//                for (int i = 0; i < alarmList.length(); i++) {
//                    JSONObject alarmItem = alarmList.getJSONObject(i);
//                    if (alarmItem.has("type")) {
//                        String type = alarmItem.getString("type");
//                        typeCountMap.put(type, typeCountMap.getOrDefault(type, 0) + 1);
//                        Log.d("HomeFragment", "处理报警项 " + i + ": type=" + type);
//                    }
//                }
//
//                // 将统计结果转换为AlarmTypeData列表
//                for (Map.Entry<String, Integer> entry : typeCountMap.entrySet()) {
//                    typeData.add(new StatisticsData.AlarmTypeData(entry.getKey(), entry.getValue()));
//                    Log.d("HomeFragment", "添加统计结果: " + entry.getKey() + " = " + entry.getValue());
//                }
//            }
//        } catch (JSONException e) {
//            Log.e("HomeFragment", "解析processing_info数据失败: " + e.getMessage());
//        }
//
//        return typeData;
//    }

    private int[] getColorArray() {
        return new int[]{
                getResources().getColor(R.color.primary_blue),
                getResources().getColor(R.color.primary_green),
                getResources().getColor(R.color.primary_orange),
                getResources().getColor(R.color.primary_red),
                getResources().getColor(R.color.primary_purple)
        };
    }

    // 根据时间跨度获取饼图中心文字
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
}