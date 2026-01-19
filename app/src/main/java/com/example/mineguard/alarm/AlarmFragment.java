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
import androidx.annotation.Nullable;
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
import com.example.mineguard.alarm.adapter.AlarmAdapterNew;
import com.example.mineguard.alarm.model.AlarmItem;
import com.example.mineguard.alarm.dialog.FilterDialog;
import com.example.mineguard.alarm.dialog.AlarmDetailDialog;
import com.example.mineguard.alarm.dialog.AlarmDetailDialogNew;
import com.example.mineguard.api.AlarmApiService;
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
import java.util.concurrent.TimeUnit;

// 在导入部分添加缺少的ResponseBody和Response类
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;      // 添加这一行
import okhttp3.ResponseBody;  // 添加这一行

import okhttp3.OkHttpClient;
import okhttp3.Request;

// 广播接收器
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Context;

/**
 * 报警管理Fragment
 * 实现实时报警推送、应急策略、报警信息查询和报警详情功能
 */
public class AlarmFragment extends Fragment implements FilterDialog.OnFilterChangeListener {

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
    private AlarmAdapterNew alarmAdapter;
    private List<AlarmItem> alarmList;
    private List<AlarmItem> filteredList;
    private NotificationManager notificationManager;
    private Vibrator vibrator;
    private Ringtone ringtone;
    private SharedPreferences preferences;
    private int currentAlarmCount = 0;
    private int totalAlarmCount = 0; // 存储API返回的报警总数

    // 筛选条件（新API对应）
    private String selectedDeviceName = "";    // 设备名称
    private int selectedRegion = 0;             // 区域ID
    private int selectedScene = 0;              // 场景ID
    private int selectedAlgorithm = 0;          // 算法ID
    private int selectedProcessStatus = -1;     // 处理状态 -1不筛选, 0未处理, 1已处理, 2误报
    private String selectedBeginTime = "";      // 开始时间
    private String selectedEndTime = "";        // 结束时间

    // 字典数据缓存
    private java.util.Map<String, String> sceneDict = new java.util.HashMap<>();
    private java.util.Map<String, String> regionDict = new java.util.HashMap<>();
    private java.util.Map<String, String> algorithmDict = new java.util.HashMap<>();

    private AlarmApiService alarmApiService;

    // 分页相关变量
    private int currentPage = 1;      // 当前页码
    private final int pageSize = 20;  // 每页数量
    private boolean isLoading = false; // 是否正在加载中（防止重复请求）
    private boolean isLastPage = false; // 是否已加载完所有数据

    // Gson 实例用于 JSON 解析
    private Gson gson;

    // WebSocket 报警更新广播接收器
    private BroadcastReceiver alarmUpdateReceiver;

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
        registerAlarmUpdateReceiver();
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

        // 注销广播接收器
        if (alarmUpdateReceiver != null) {
            requireContext().unregisterReceiver(alarmUpdateReceiver);
        }
    }

    /**
     * 注册报警更新广播接收器
     */
    private void registerAlarmUpdateReceiver() {
        alarmUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.mineguard.ALARM_UPDATE".equals(intent.getAction())) {
                    String alarmJson = intent.getStringExtra("alarm_data");
                    if (alarmJson != null) {
                        // 解析报警数据
                        AlarmItem newAlarm = gson.fromJson(alarmJson, AlarmItem.class);

                        // 将新报警添加到列表顶部
                        alarmList.add(0, newAlarm);
                        alarmAdapter.notifyItemInserted(0);

                        // 滚动到顶部
                        if (recyclerView != null) {
                            recyclerView.smoothScrollToPosition(0);
                        }

                        // 显示提示
                        Toast.makeText(context,
                                "收到新报警: " + newAlarm.getDevice_name(),
                                Toast.LENGTH_SHORT).show();

                        Log.d(TAG, "收到 WebSocket 报警更新: " + newAlarm.getDevice_name());
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter("com.example.mineguard.ALARM_UPDATE");

        // Android 14+ 需要指定 RECEIVER_EXPORTED 或 RECEIVER_NOT_EXPORTED
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(alarmUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(alarmUpdateReceiver, filter);
        }
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 首次加载时显示模拟数据，提升用户体验
        // 当真实API数据返回时会自动替换
        if (alarmList.isEmpty()) {
            alarmList.addAll(createMockAlarmData());
            alarmAdapter.notifyDataSetChanged();
            Toast.makeText(requireContext(), "演示模式：显示模拟数据", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeServices() {
        notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        preferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // 初始化 Gson 实例
        gson = new Gson();

        // 初始化API服务
        alarmApiService = AlarmApiService.getInstance(requireContext());

        // 初始化铃声用于语音提醒
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        ringtone = RingtoneManager.getRingtone(requireContext(), alarmUri);

        // 加载字典数据
        loadDictData();
    }

    /**
     * 加载字典数据（场景、区域、算法）
     */
    private void loadDictData() {
        alarmApiService.getSceneDict(new AlarmApiService.AlarmApiCallback<java.util.Map<String, String>>() {
            @Override
            public void onSuccess(java.util.Map<String, String> response) {
                sceneDict.clear();
                sceneDict.putAll(response);
            }

            @Override
            public void onError(String errorMessage) {
                Log.w(TAG, "加载场景字典失败: " + errorMessage);
            }
        });

        alarmApiService.getRegionDict(new AlarmApiService.AlarmApiCallback<java.util.Map<String, String>>() {
            @Override
            public void onSuccess(java.util.Map<String, String> response) {
                regionDict.clear();
                regionDict.putAll(response);
            }

            @Override
            public void onError(String errorMessage) {
                Log.w(TAG, "加载区域字典失败: " + errorMessage);
            }
        });

        alarmApiService.getAlgorithmDict(new AlarmApiService.AlarmApiCallback<java.util.Map<String, String>>() {
            @Override
            public void onSuccess(java.util.Map<String, String> response) {
                algorithmDict.clear();
                algorithmDict.putAll(response);
            }

            @Override
            public void onError(String errorMessage) {
                Log.w(TAG, "加载算法字典失败: " + errorMessage);
            }
        });
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
        //设置RecyclerView布局管理器
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        //初始化新的适配器
        alarmAdapter = new AlarmAdapterNew(alarmList, new AlarmAdapterNew.OnAlarmClickListener() {
            @Override
            public void onAlarmClick(AlarmItem alarm) {
                AlarmDetailDialogNew dialog = AlarmDetailDialogNew.newInstance(alarm);
                dialog.show(getChildFragmentManager(), "AlarmDetailDialogNew");
            }

            @Override
            public void onAlarmLongClick(AlarmItem alarm) {
                showQuickProcessDialog(alarm);
            }

            @Override
            public void onStatusChanged(int position, AlarmItem alarm) {
                // 状态改变后刷新该项
                alarmAdapter.notifyItemChanged(position);
            }
        });
        recyclerView.setAdapter(alarmAdapter);

        // 【关键修改点】：必须使用 addOnScrollListener 包裹住里面的 onScrolled 方法
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // dy > 0 表示手指向上划（内容向下滚）
                if (dy > 0) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        int visibleItemCount = layoutManager.getChildCount();
                        int totalItemCount = layoutManager.getItemCount();
                        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                        // 如果不是正在加载，且没到最后一页，且滑到了倒数第几项
                        if (!isLoading && !isLastPage) {
                            if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                    && firstVisibleItemPosition >= 0) {
                                loadMoreData(); // 加载下一页
                            }
                        }
                    }
                }
            }
        }); // <--- 注意这里的大括号和分号，这里才算结束
    }
    // 下拉刷新专用：重置为第1页
    private void refreshData() {
        currentPage = 1;
        isLastPage = false;
        loadAlarmDataFromAPI();
    }

    // 上拉加载专用：页码 +1
    private void loadMoreData() {
        currentPage++;
        loadAlarmDataFromAPI();
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
        // 调用新API获取报警数据
        loadAlarmDataFromAPI();
    }

    /**
     * 创建模拟数据用于离线状态展示
     */
    private List<AlarmItem> createMockAlarmData() {
        List<AlarmItem> mockData = new ArrayList<>();

        // 模拟数据1：人员入侵检测 - 未处理
        AlarmItem alarm1 = new AlarmItem();
        alarm1.setId(1001);
        alarm1.setOccur_time("2024-10-17 15:30:25");
        alarm1.setDevice_id(1);
        alarm1.setDevice_name("主井皮带运输机 #01");
        alarm1.setDetect_target("人员入侵检测");
        alarm1.setProcess_status(0); // 未处理
        alarm1.setScene_name("主井运输巷");
        alarm1.setRegion_name("东区");
        alarm1.setAlarm_pic_url("http://via.placeholder.com/400x300/FF5252/FFFFFF?text=人员入侵");
        alarm1.setAlarm_video("alarm_1001_video.mp4");
        alarm1.setResponsible_person("张三");
        alarm1.setResponsible_unit("机电队");
        mockData.add(alarm1);

        // 模拟数据2：皮带跑偏检测 - 已处理
        AlarmItem alarm2 = new AlarmItem();
        alarm2.setId(1002);
        alarm2.setOccur_time("2024-10-17 14:20:15");
        alarm2.setDevice_id(2);
        alarm2.setDevice_name("副井提升机 #02");
        alarm2.setDetect_target("皮带跑偏检测");
        alarm2.setProcess_status(1); // 已处理
        alarm2.setProcess_time("2024-10-17 14:35:00");
        alarm2.setProcess_user("李四");
        alarm2.setProcess_desc("已调整皮带张力，检测正常，设备恢复正常运行");
        alarm2.setScene_name("副井提升机房");
        alarm2.setRegion_name("西区");
        alarm2.setAlarm_pic_url("http://via.placeholder.com/400x300/FF9800/FFFFFF?text=皮带跑偏");
        alarm2.setResponsible_person("王五");
        alarm2.setResponsible_unit("运输队");
        mockData.add(alarm2);

        // 模拟数据3：明火检测 - 未处理
        AlarmItem alarm3 = new AlarmItem();
        alarm3.setId(1003);
        alarm3.setOccur_time("2024-10-17 16:45:30");
        alarm3.setDevice_id(3);
        alarm3.setDevice_name("综采工作面监控 #03");
        alarm3.setDetect_target("明火检测");
        alarm3.setProcess_status(0); // 未处理
        alarm3.setScene_name("综采工作面");
        alarm3.setRegion_name("南区");
        alarm3.setAlarm_pic_url("http://via.placeholder.com/400x300/D32F2F/FFFFFF?text=明火检测");
        alarm3.setAlarm_video("alarm_1003_video.mp4");
        alarm3.setResponsible_person("赵六");
        alarm3.setResponsible_unit("通风队");
        mockData.add(alarm3);

        return mockData;
    }

    /**
     * 使用新API加载报警数据
     */
    private void loadAlarmDataFromAPI() {
        if (isLoading) return;
        isLoading = true;

        if (currentPage == 1 && !swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(true);
        }

        AlarmApiService.AlarmListRequest request = new AlarmApiService.AlarmListRequest(currentPage, pageSize);
        request.device_name = selectedDeviceName;
        request.region = selectedRegion;
        request.scene = selectedScene;
        request.algorithm = selectedAlgorithm;
        request.process_status = selectedProcessStatus;
        request.begin_time = selectedBeginTime;
        request.end_time = selectedEndTime;

        alarmApiService.getAlarmList(request, new AlarmApiService.AlarmApiCallback<AlarmApiService.AlarmListResponse>() {
            @Override
            public void onSuccess(AlarmApiService.AlarmListResponse response) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    isLoading = false;
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);

                    if (response.data != null && response.data.results != null) {
                        List<AlarmItem> newItems = response.data.results;
                        totalAlarmCount = response.data.count;

                        if (currentPage == 1) {
                            alarmList.clear();
                            alarmList.addAll(newItems);
                            alarmAdapter.notifyDataSetChanged();
                        } else {
                            int startPos = alarmList.size();
                            alarmList.addAll(newItems);
                            alarmAdapter.notifyItemRangeInserted(startPos, newItems.size());
                        }

                        if (newItems.size() < pageSize) {
                            isLastPage = true;
                            Toast.makeText(requireContext(), "没有更多数据了", Toast.LENGTH_SHORT).show();
                        } else {
                            isLastPage = false;
                        }
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                isLoading = false;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    if (currentPage > 1) currentPage--;

                    // 尝试加载缓存数据（离线模式）
                    if (currentPage == 1) {
                        AlarmApiService.AlarmListResponse cached = alarmApiService.getCachedData();
                        if (cached != null && cached.data != null && cached.data.results != null && !cached.data.results.isEmpty()) {
                            Toast.makeText(requireContext(), "网络离线，显示缓存数据", Toast.LENGTH_SHORT).show();
                            alarmList.clear();
                            alarmList.addAll(cached.data.results);
                            alarmAdapter.notifyDataSetChanged();
                        } else {
                            // 无缓存数据时，显示模拟数据
                            Toast.makeText(requireContext(), "网络离线，显示模拟数据", Toast.LENGTH_LONG).show();
                            alarmList.clear();
                            alarmList.addAll(createMockAlarmData());
                            alarmAdapter.notifyDataSetChanged();
                            isLastPage = true; // 模拟数据没有更多
                        }
                    } else {
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // ========== 筛选相关方法 ==========

    @Override
    public void onFilterChanged(String alarmType, String alarmLevel, String status, String location) {
        // 更新筛选条件
        selectedDeviceName = alarmType;
        selectedProcessStatus = parseStatus(status);

        // 重置分页并重新加载数据
        currentPage = 1;
        isLastPage = false;
        loadAlarmDataFromAPI();
    }

    private int parseStatus(String status) {
        if (status.isEmpty() || status.equals("全部")) return -1;
        switch (status) {
            case "未处理": return 0;
            case "已处理": return 1;
            case "误报": return 2;
            default: return -1;
        }
    }

    private void filterAlarms(String query) {
        filteredList.clear();

        for (AlarmItem alarm : alarmList) {
            boolean matches = true;

            // 搜索关键词匹配
            if (!query.isEmpty()) {
                matches = (alarm.getDevice_name() != null && alarm.getDevice_name().toLowerCase().contains(query.toLowerCase())) ||
                        (alarm.getDetect_target() != null && alarm.getDetect_target().toLowerCase().contains(query.toLowerCase())) ||
                        (alarm.getRegion_name() != null && alarm.getRegion_name().toLowerCase().contains(query.toLowerCase()));
            }

            if (matches) {
                filteredList.add(alarm);
            }
        }

        alarmAdapter.notifyDataSetChanged();
    }

    private void applyFilters() {
        filterAlarms(searchView.getQuery().toString());
    }

    private void clearFilters() {
        selectedDeviceName = "";
        selectedRegion = 0;
        selectedScene = 0;
        selectedAlgorithm = 0;
        selectedProcessStatus = -1;
        selectedBeginTime = "";
        selectedEndTime = "";
        applyFilters();
        currentPage = 1;
        loadAlarmDataFromAPI();
        Toast.makeText(requireContext(), "筛选条件已清空", Toast.LENGTH_SHORT).show();
    }

    // 修改showFilterDialog方法
    private void showFilterDialog() {
        FilterDialog dialog = FilterDialog.newInstance(
                "", "", "", "");
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

    private void showQuickProcessDialog(AlarmItem alarm) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("快速处理")
                .setItems(new String[]{"标记为已处理", "标记为误报", "查看详情"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            alarm.setProcess_status(AlarmItem.STATUS_PROCESSED);
                            break;
                        case 1:
                            alarm.setProcess_status(AlarmItem.STATUS_FALSE_ALARM);
                            break;
                        case 2:
                            AlarmDetailDialogNew detailDialog = AlarmDetailDialogNew.newInstance(alarm);
                            detailDialog.show(getChildFragmentManager(), "AlarmDetailDialogNew");
                            break;
                    }
                    alarmAdapter.notifyDataSetChanged();
                })
                .show();
    }
}