package com.example.mineguard.alarm;

import static com.example.mineguard.MyApplication.globalIP;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.example.mineguard.MainActivity;
import com.example.mineguard.R;
import com.example.mineguard.alarm.adapter.AlarmAdapter;
import com.example.mineguard.alarm.model.AlarmItem;
import com.example.mineguard.alarm.dialog.FilterDialog;
import com.example.mineguard.alarm.dialog.AlarmDetailDialog;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// 在导入部分添加缺少的ResponseBody和Response类
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;      // 添加这一行
import okhttp3.ResponseBody;  // 添加这一行

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * 报警管理Fragment
 * 实现实时报警推送、应急策略、报警信息查询和报警详情功能
 */
public class AlarmFragment extends Fragment implements AlarmAdapter.OnAlarmClickListener,
        FilterDialog.OnFilterChangeListener {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String PREF_NAME = "alarm_prefs";
    private static final String KEY_ALARM_COUNT = "alarm_count";
    private static final String CHANNEL_ID = "alarm_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final String TAG = "AlarmFragment";
    private LinearLayout layoutEmpty;
    private LinearLayout headerContainer;
    private View btnFilter;
    private RecyclerView recyclerView;
    private SearchView searchView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AlarmAdapter alarmAdapter;
    private List<AlarmItem> alarmList;
    private List<AlarmItem> filteredList;
    private NotificationManager notificationManager;
    private Vibrator vibrator;
    private Ringtone ringtone;
    private SharedPreferences preferences;
    private int currentAlarmCount = 0;
    private int totalAlarmCount = 0; // 存储API返回的报警总数

    // 筛选条件
    private String selectedAlarmType = "";
    private String selectedAlarmLevel = "";
    private String selectedStatus = "";
    private String selectedLocation = "";

    public AlarmFragment() {
        // Required empty public constructor
    }

    public static AlarmFragment newInstance(String param1, String param2) {
        AlarmFragment fragment = new AlarmFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String mParam1 = getArguments().getString(ARG_PARAM1);
            String mParam2 = getArguments().getString(ARG_PARAM2);
        }

        initializeServices();
        createNotificationChannel();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarm, container, false);

        initializeViews(view);
        setupRecyclerView();
        setupSearchView();
        setupSwipeRefresh();
        loadAlarmData();

        return view;
    }

    private void initializeServices() {
        notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        preferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // 初始化铃声用于语音提醒
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        ringtone = RingtoneManager.getRingtone(requireContext(), alarmUri);
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "报警通知",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("矿山设备报警通知");
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void initializeViews(View view) {
        headerContainer = view.findViewById(R.id.headerContainer);
        recyclerView = view.findViewById(R.id.recyclerView);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        searchView = view.findViewById(R.id.searchView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        btnFilter = view.findViewById(R.id.btnFilter);

        // 【插入在这里】动态适配状态栏高度
        // =======================================================
        if (headerContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(headerContainer, (v, windowInsets) -> {
                // 获取系统状态栏的高度
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());

                // 动态设置 Padding:
                // 左(不变), 上(状态栏高度 + 原本的12dp), 右(不变), 下(不变)
                v.setPadding(
                        v.getPaddingLeft(),
                        insets.top + dp2px(getContext(), 12),
                        v.getPaddingRight(),
                        v.getPaddingBottom()
                );

                return windowInsets;
            });
        }
        // 设置筛选按钮点击事件
        view.findViewById(R.id.btnFilter).setOnClickListener(v -> showFilterDialog());

        // 设置清空筛选按钮点击事件
        //view.findViewById(R.id.btnClearFilter).setOnClickListener(v -> clearFilters());
    }

    private int dp2px(Context context, float dpValue) {
        if (context == null) return 0;
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private void setupRecyclerView() {
        alarmList = new ArrayList<>();
        filteredList = new ArrayList<>();
        alarmAdapter = new AlarmAdapter(filteredList, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(alarmAdapter);
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterAlarms(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterAlarms(newText);
                return false;
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadAlarmData();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void loadAlarmData() {
        // 调用API获取真实数据，默认都为空
        getAlertsBundle(null, null, null);
    }

    /**
     * 从API获取报警数据
     *
     * @param camera_id 可选，按摄像机ID筛选
     * @param page      可选，分页页码
     * @param limitNum  可选，每页条数（最大500）
     */
    private void getAlertsBundle(Integer camera_id, Integer page, Integer limitNum) {
        OkHttpClient client = new OkHttpClient();

        // 构建URL和参数
        String baseUrl = "http://" + globalIP + ":5004/data/alerts_bundle";
        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        boolean firstParam = true;

        if (camera_id != null) {
            urlBuilder.append(firstParam ? "?" : "&");
            urlBuilder.append("camera_id=").append(camera_id);
            firstParam = false;
        }
        if (page != null) {
            urlBuilder.append(firstParam ? "?" : "&");
            urlBuilder.append("page=").append(page);
            firstParam = false;
        }
        if (limitNum != null) {
            urlBuilder.append(firstParam ? "?" : "&");
            urlBuilder.append("limitNum=").append(limitNum);
            firstParam = false;
        }

        String url = urlBuilder.toString();
        Log.d(TAG, "API请求URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API请求失败: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(requireContext(), "网络请求失败，请检查网络连接", Toast.LENGTH_SHORT).show();
                    applyFilters();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 使用try-with-resources确保ResponseBody被关闭
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful() && responseBody != null) {
                        String responseBodyString = responseBody.string();
                        Log.d(TAG, "API返回数据: " + responseBodyString);

                        try {
                            // 解析JSON数据
                            JsonObject jsonResponse = new JsonParser().parse(responseBodyString).getAsJsonObject();
                            if (jsonResponse.has("code") && jsonResponse.get("code").getAsInt() == 0) {
                                JsonObject data = jsonResponse.getAsJsonObject("data");

                                // 获取报警总数
                                if (data.has("count")) {
                                    totalAlarmCount = data.get("count").getAsInt();
                                    Log.d(TAG, "总报警数: " + totalAlarmCount);
                                }

                                // 解析报警列表
                                if (data.has("alerts")) {
                                    JsonArray alertsArray = data.getAsJsonArray("alerts");
                                    List<AlarmItem> newAlarmList = new ArrayList<>();

                                    for (int i = 0; i < alertsArray.size(); i++) {
                                        JsonObject alert = alertsArray.get(i).getAsJsonObject();
                                        AlarmItem alarmItem = new AlarmItem();

                                        // 填充AlarmItem字段
                                        if (alert.has("id"))
                                            alarmItem.setId(alert.get("id").getAsInt());
                                        if (alert.has("channel"))
                                            alarmItem.setChannel(alert.get("channel").getAsString());
                                        if (alert.has("type"))
                                            alarmItem.setType(alert.get("type").getAsString());
                                        if (alert.has("level"))
                                            alarmItem.setLevel(alert.get("level").getAsString());
                                        if (alert.has("path"))
                                            alarmItem.setPath(alert.get("path").getAsString());
                                        if (alert.has("video_path"))
                                            alarmItem.setVideo_path(alert.get("video_path").getAsString());
                                        if (alert.has("status"))
                                            alarmItem.setStatus(alert.get("status").getAsInt());
                                        if (alert.has("camera_id")) {
                                            if (!alert.get("camera_id").isJsonNull()) {
                                                alarmItem.setCamera_id(alert.get("camera_id").getAsInt());
                                            }
                                        }
                                        if (alert.has("url")) {
                                            if (!alert.get("url").isJsonNull()) {
                                                alarmItem.setUrl(alert.get("url").getAsString());
                                            }
                                        }
                                        if (alert.has("solve_time")) {
                                            if (!alert.get("solve_time").isJsonNull()) {
                                                alarmItem.setSolve_time(alert.get("solve_time").getAsString());
                                            }
                                        }
                                        if (alert.has("ip")) {
                                            if (!alert.get("ip").isJsonNull()) {
                                                alarmItem.setIp(alert.get("ip").getAsString());
                                            }
                                        }
                                        if (alert.has("name")) {
                                            if (!alert.get("name").isJsonNull()) {
                                                alarmItem.setName(alert.get("name").getAsString());
                                            }
                                        }
                                        if (alert.has("location")) {
                                            if (!alert.get("location").isJsonNull()) {
                                                alarmItem.setLocation(alert.get("location").getAsString());
                                            }
                                        }
                                        if (alert.has("flow")) {
                                            if (!alert.get("flow").isJsonNull()) {
                                                alarmItem.setFlow(alert.get("flow").getAsString());
                                            }
                                        }

                                        // 解析video_paths数组
                                        if (alert.has("video_paths") && !alert.get("video_paths").isJsonNull()) {
                                            JsonArray videoPathsArray = alert.getAsJsonArray("video_paths");
                                            String[] videoPaths = new String[videoPathsArray.size()];
                                            for (int j = 0; j < videoPathsArray.size(); j++) {
                                                videoPaths[j] = videoPathsArray.get(j).getAsString();
                                            }
                                            alarmItem.setVideo_paths(videoPaths);
                                        }

                                        newAlarmList.add(alarmItem);
                                    }

                                    // 在主线程更新UI
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        alarmList.clear();
                                        alarmList.addAll(newAlarmList);
                                        applyFilters();
                                        checkNewAlarms();

                                        // 显示总报警数
                                        Toast.makeText(requireContext(), "共获取到 " + totalAlarmCount + " 条报警记录", Toast.LENGTH_SHORT).show();
                                    });
                                }
                            } else {
                                String message = jsonResponse.has("message") ?
                                        jsonResponse.get("message").getAsString() : "请求失败";
                                Log.e(TAG, "API返回错误: " + message);
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                                });
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "解析JSON数据失败: " + e.getMessage());
                            new Handler(Looper.getMainLooper()).post(() -> {
                                Toast.makeText(requireContext(), "数据解析失败", Toast.LENGTH_SHORT).show();
                                applyFilters();
                            });
                        }
                    } else {
                        Log.e(TAG, "API请求失败，状态码: " + response.code());
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(requireContext(), "请求失败，状态码: " + response.code(), Toast.LENGTH_SHORT).show();
                            applyFilters();
                        });
                    }
                }
            }
        });
    }

    @Override
    public void onFilterChanged(String alarmType, String alarmLevel, String status, String location) {
        selectedAlarmType = alarmType;
        selectedAlarmLevel = alarmLevel;
        selectedStatus = status;
        selectedLocation = location;
        applyFilters();
    }

    // 修改filterAlarms方法，使用新的筛选字段
    private void filterAlarms(String query) {
        filteredList.clear();

        for (AlarmItem alarm : alarmList) {
            boolean matches = true;

            // 搜索关键词匹配
            if (!query.isEmpty()) {
                matches = (alarm.getName() != null && alarm.getName().toLowerCase().contains(query.toLowerCase())) ||
                        (alarm.getType() != null && alarm.getType().toLowerCase().contains(query.toLowerCase())) ||
                        (alarm.getLocation() != null && alarm.getLocation().toLowerCase().contains(query.toLowerCase()));
            }

            // 应用新的筛选条件
            if (matches && !selectedAlarmType.isEmpty()) {
                matches = alarm.getType() != null && alarm.getType().equals(selectedAlarmType);
            }
            if (matches && !selectedAlarmLevel.isEmpty()) {
                // 将levelConstant从int改为String类型，直接使用字符串匹配
                String levelConstant = AlarmItem.LEVEL_WARNING;
                if (selectedAlarmLevel.equals("严重")) {
                    levelConstant = AlarmItem.LEVEL_CRITICAL;
                }
                matches = levelConstant.equals(alarm.getLevel());
            }
            if (matches && !selectedStatus.isEmpty()) {
                String statusDesc = alarm.getStatusDescription();
                matches = statusDesc != null && statusDesc.equals(selectedStatus);
            }
            if (matches && !selectedLocation.isEmpty()) {
                matches = alarm.getLocation() != null && alarm.getLocation().equals(selectedLocation);
            }

            if (matches) {
                filteredList.add(alarm);
            }
        }

        alarmAdapter.notifyDataSetChanged();
    }

    // 添加缺失的applyFilters方法
    private void applyFilters() {
        filterAlarms(searchView.getQuery().toString());
    }

    // 修改clearFilters方法
    private void clearFilters() {
        // 清空新的筛选字段
        selectedAlarmType = "";
        selectedAlarmLevel = "";
        selectedStatus = "";
        selectedLocation = "";
        applyFilters();
        Toast.makeText(requireContext(), "筛选条件已清空", Toast.LENGTH_SHORT).show();
    }

    // 修改showFilterDialog方法
    private void showFilterDialog() {
        FilterDialog dialog = FilterDialog.newInstance(
                selectedAlarmType, selectedAlarmLevel, selectedStatus, selectedLocation);
        dialog.setOnFilterChangeListener(this); // 设置监听器
        dialog.show(getChildFragmentManager(), "FilterDialog");
    }

    private void checkNewAlarms() {
        int newAlarmCount = getUnprocessedAlarmCount();
        int previousCount = preferences.getInt(KEY_ALARM_COUNT, 0);

        if (newAlarmCount > previousCount) {
            // 有新报警
            currentAlarmCount = newAlarmCount - previousCount;
            sendNotification();
            // 如果有严重报警，触发应急策略
            if (hasCriticalAlarm()) {
                triggerEmergencyResponse();
            }
        }

        // 更新保存的计数
        preferences.edit().putInt(KEY_ALARM_COUNT, newAlarmCount).apply();
    }

    private int getUnprocessedAlarmCount() {
        int count = 0;
        for (AlarmItem alarm : alarmList) {
            if (alarm.isUnprocessed()) {
                count++;
            }
        }
        return count;
    }

    private boolean hasCriticalAlarm() {
        for (AlarmItem alarm : alarmList) {
            if (alarm.isCritical() && alarm.isUnprocessed()) {
                return true;
            }
        }
        return false;
    }

    private void sendNotification() {
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                requireContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("新报警通知")
                .setContentText("您有 " + currentAlarmCount + " 条新报警需要处理")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setNumber(currentAlarmCount);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void triggerEmergencyResponse() {
        // 触发振动
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 500, 200, 500, 200, 500};
            vibrator.vibrate(pattern, -1);
        }

        // 播放语音提醒
        if (ringtone != null && !ringtone.isPlaying()) {
            // 循环播放铃声
        }

        Toast.makeText(requireContext(), "严重报警！已触发应急响应", Toast.LENGTH_LONG).show();
    }

    // AlarmAdapter.OnAlarmClickListener 实现
    @Override
    public void onAlarmClick(AlarmItem alarm) {
        // 跳转到报警详情页
        AlarmDetailDialog dialog = AlarmDetailDialog.newInstance(alarm);
        dialog.show(getChildFragmentManager(), "AlarmDetailDialog");
    }

    @Override
    public void onAlarmLongClick(AlarmItem alarm) {
        // 长按显示快速处理选项
        showQuickProcessDialog(alarm);
    }

    private void showQuickProcessDialog(AlarmItem alarm) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("快速处理")
                .setItems(new String[]{"标记为已处理", "标记为处理中", "查看详情"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            alarm.setStatus(AlarmItem.STATUS_PROCESSED);
                            break;
                        case 1:
                            // 注意：新的AlarmItem只有两种状态，这里可能需要额外处理
                            // 暂时也标记为已处理
                            alarm.setStatus(AlarmItem.STATUS_PROCESSED);
                            break;
                        case 2:
                            onAlarmClick(alarm);
                            break;
                    }
                    alarmAdapter.notifyDataSetChanged();
                })
                .show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ringtone != null) {
            if (ringtone.isPlaying()) {
                ringtone.stop();
            }
            ringtone = null;
        }
    }
}