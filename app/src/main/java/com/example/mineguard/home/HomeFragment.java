package com.example.mineguard.home;

import static com.example.mineguard.MyApplication.globalIP;

import com.github.mikephil.charting.charts.BarChart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.example.mineguard.R;

/**
 * 首页Fragment
 */
public class HomeFragment extends Fragment {

    private PlatformStatisticsManager platformStatisticsManager;
    private SceneAlarmStatisticsManager sceneAlarmStatisticsManager;
    private AlarmTypeStatisticsManager alarmTypeStatisticsManager;
    private DeviceAlarmStatisticsManager deviceAlarmStatisticsManager;
    private ImageButton btnRefresh;

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
    public void onResume() {
        super.onResume();
        // 每次界面可见时自动刷新数据
        fetchStatisticsDataFromApi();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initManagers();
        initHeaderViews(view);
        initViews(view);
        setupTimeSpanListeners();
        fetchStatisticsDataFromApi();

        return view;
    }

    /**
     * 初始化管理器
     */
    private void initManagers() {
        platformStatisticsManager = new PlatformStatisticsManager(requireContext());
        sceneAlarmStatisticsManager = new SceneAlarmStatisticsManager(requireContext());
        alarmTypeStatisticsManager = new AlarmTypeStatisticsManager(requireContext());
        deviceAlarmStatisticsManager = new DeviceAlarmStatisticsManager(requireContext());
    }

    /**
     * 初始化头部视图
     */
    private void initHeaderViews(View root) {
        View overviewCard = root.findViewById(R.id.card_header_overview);
        View alarmCard = root.findViewById(R.id.card_header_alarm);
        View deviceCard = root.findViewById(R.id.card_header_device);

        platformStatisticsManager.bindViews(
                overviewCard.findViewById(R.id.tv_processed_percent),
                root.findViewById(R.id.pb_overview_rate),
                alarmCard.findViewById(R.id.tv_total_alarms_header),
                alarmCard.findViewById(R.id.tv_untreated_alarms_header),
                deviceCard.findViewById(R.id.tv_total_devices_header),
                deviceCard.findViewById(R.id.tv_online_devices_header)
        );
    }

    /**
     * 初始化视图
     */
    private void initViews(View view) {
        // 初始化刷新按钮
        btnRefresh = view.findViewById(R.id.btn_refresh);
        btnRefresh.setOnClickListener(v -> fetchStatisticsDataFromApi());

        // 初始化场景报警统计图表
        View sceneStatsCard = view.findViewById(R.id.card_bar_statistics);
        sceneAlarmStatisticsManager.bindViews(
                sceneStatsCard.findViewById(R.id.bar_chart),
                sceneStatsCard.findViewById(R.id.tv_time_one_day),
                sceneStatsCard.findViewById(R.id.tv_time_one_week),
                sceneStatsCard.findViewById(R.id.tv_time_one_month)
        );
        sceneAlarmStatisticsManager.setupChart();
        sceneAlarmStatisticsManager.setDefaultTimeSpan();

        // 设置图表标题
        TextView tvBarChartTitle = sceneStatsCard.findViewById(R.id.tv_chart_title);
        tvBarChartTitle.setText("场景报警统计");

        // 初始化饼图相关视图
        View pieChartCard = view.findViewById(R.id.card_piechart_WeekMonthAlarmType);
        alarmTypeStatisticsManager.bindViews(
                pieChartCard.findViewById(R.id.pie_chart),
                pieChartCard.findViewById(R.id.tv_chart_title),
                pieChartCard.findViewById(R.id.tv_time_one_day),
                pieChartCard.findViewById(R.id.tv_time_one_week),
                pieChartCard.findViewById(R.id.tv_time_one_month)
        );

        // 初始化设备报警统计图表
        View deviceStatsCard = view.findViewById(R.id.card_bar_device_statistics);
        deviceAlarmStatisticsManager.bindViews(
                deviceStatsCard.findViewById(R.id.bar_chart),
                deviceStatsCard.findViewById(R.id.tv_time_one_day),
                deviceStatsCard.findViewById(R.id.tv_time_one_week),
                deviceStatsCard.findViewById(R.id.tv_time_one_month)
        );
        deviceAlarmStatisticsManager.setupChart();
        deviceAlarmStatisticsManager.setDefaultTimeSpan();

        // 设置图表标题
        TextView tvDeviceChartTitle = deviceStatsCard.findViewById(R.id.tv_chart_title);
        tvDeviceChartTitle.setText("设备报警统计");
    }

    /**
     * 设置时间跨度监听器
     */
    private void setupTimeSpanListeners() {
        sceneAlarmStatisticsManager.setTimeSpanListeners();
        alarmTypeStatisticsManager.setTimeSpanListeners();
        deviceAlarmStatisticsManager.setTimeSpanListeners();
    }

    /**
     * 从API获取所有统计数据
     */
    private void fetchStatisticsDataFromApi() {
        String baseUrl = "http://" + globalIP + ":80/prod-api";

        // 1. 请求当日数据
        platformStatisticsManager.fetchStatisticsData();

        // 2. 请求场景报警统计数据
        String sceneTimeParam = getTimeParam(sceneAlarmStatisticsManager.getCurrentTimeSpan());
        sceneAlarmStatisticsManager.fetchStatisticsData(baseUrl, sceneTimeParam);

        // 3. 请求报警类型统计数据
        String alarmTypeTimeParam = getTimeParam(alarmTypeStatisticsManager.getCurrentTimeSpan());
        alarmTypeStatisticsManager.fetchStatisticsData(baseUrl, alarmTypeTimeParam);

        // 4. 请求设备报警统计数据
        String deviceTimeParam = getDeviceTimeParam(deviceAlarmStatisticsManager.getCurrentTimeSpan());
        deviceAlarmStatisticsManager.fetchStatisticsData(baseUrl, deviceTimeParam);
    }

    private String getTimeParam(SceneAlarmStatisticsManager.TimeSpan timeSpan) {
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

    private String getTimeParam(AlarmTypeStatisticsManager.TimeSpan timeSpan) {
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

    private String getDeviceTimeParam(DeviceAlarmStatisticsManager.TimeSpan timeSpan) {
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
}
