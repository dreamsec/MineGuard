package com.example.mineguard.preview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mineguard.R;
import com.example.mineguard.preview.adapter.DeviceInfoAdapter;
import com.example.mineguard.preview.model.DeviceInfo;
import com.example.mineguard.preview.model.DeviceInfoPush;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备监控 Fragment（新版本）
 * 使用 WebSocket 接收实时设备信息推送
 */
public class PreviewFragment extends Fragment {

    private static final String TAG = "PreviewFragment";
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    // UI 组件
    private RecyclerView recyclerViewDevices;
    private LinearLayout layoutEmpty;
    private TextView tvDeviceCount;

    // 适配器和数据
    private DeviceInfoAdapter deviceAdapter;
    private List<DeviceInfo> deviceList;
    private Gson gson;

    // WebSocket 设备更新广播接收器
    private BroadcastReceiver deviceUpdateReceiver;

    public PreviewFragment() {
        // Required empty public constructor
    }

    public static PreviewFragment newInstance(String param1, String param2) {
        PreviewFragment fragment = new PreviewFragment();
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
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        gson = new Gson();
        deviceList = new ArrayList<>();

        // 注册设备更新广播接收器
        registerDeviceUpdateReceiver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_preview, container, false);

        initViews(view);
        setupRecyclerView();
        loadOfflineData(); // 先显示离线数据

        return view;
    }

    private void initViews(View view) {
        recyclerViewDevices = view.findViewById(R.id.recyclerViewDevices);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        tvDeviceCount = view.findViewById(R.id.tvDeviceCount);
    }

    private void setupRecyclerView() {
        deviceAdapter = new DeviceInfoAdapter(requireContext());
        recyclerViewDevices.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewDevices.setAdapter(deviceAdapter);
    }

    /**
     * 加载离线模拟数据
     */
    private void loadOfflineData() {
        List<DeviceInfo> mockDevices = createMockDevices();
        updateDeviceList(mockDevices);

        // 延迟后显示 Toast 提示
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Toast.makeText(requireContext(), "离线模式：显示模拟设备", Toast.LENGTH_SHORT).show();
        }, 500);
    }

    /**
     * 创建模拟设备数据
     */
    private List<DeviceInfo> createMockDevices() {
        List<DeviceInfo> mockDevices = new ArrayList<>();

        // 模拟设备 1
        DeviceInfo device1 = new DeviceInfo();
        device1.setDevice_id(1001);
        device1.setDevice_name("主井皮带运输机 #01");
        device1.setScene_id(1);
        device1.setScene_name("主运输巷");
        device1.setRegion_id(1);
        device1.setRegion_name("东区");
        device1.setRtsp("rtsp://admin:123456@192.168.1.101:554/stream1");
        device1.setOnline(1);
        device1.setEnable(true);

        // 模拟设备 2
        DeviceInfo device2 = new DeviceInfo();
        device2.setDevice_id(1002);
        device2.setDevice_name("副井提升机 #02");
        device2.setScene_id(2);
        device2.setScene_name("副井提升机房");
        device2.setRegion_id(2);
        device2.setRegion_name("西区");
        device2.setRtsp("rtsp://admin:123456@192.168.1.102:554/stream1");
        device2.setOnline(1);
        device2.setEnable(true);

        // 模拟设备 3
        DeviceInfo device3 = new DeviceInfo();
        device3.setDevice_id(1003);
        device3.setDevice_name("综采工作面监控 #03");
        device3.setScene_id(3);
        device3.setScene_name("综采工作面");
        device3.setRegion_id(3);
        device3.setRegion_name("南区");
        device3.setRtsp("");  // 离线设备，无 RTSP 流
        device3.setOnline(0);
        device3.setEnable(false);

        mockDevices.add(device1);
        mockDevices.add(device2);
        mockDevices.add(device3);

        return mockDevices;
    }

    /**
     * 更新设备列表
     */
    private void updateDeviceList(List<DeviceInfo> newDevices) {
        if (newDevices == null || newDevices.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerViewDevices.setVisibility(View.GONE);
            tvDeviceCount.setText("0 个设备");
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerViewDevices.setVisibility(View.VISIBLE);

            deviceList.clear();
            deviceList.addAll(newDevices);
            deviceAdapter.updateDevices(deviceList);

            // 更新设备计数
            int onlineCount = 0;
            for (DeviceInfo device : newDevices) {
                if (device.isOnline()) {
                    onlineCount++;
                }
            }
            tvDeviceCount.setText(onlineCount + "/" + newDevices.size() + " 在线");
        }
    }

    /**
     * 注册设备更新广播接收器
     */
    private void registerDeviceUpdateReceiver() {
        deviceUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.mineguard.DEVICE_UPDATE".equals(intent.getAction())) {
                    String pushJson = intent.getStringExtra("device_push");
                    if (pushJson != null) {
                        try {
                            DeviceInfoPush push = gson.fromJson(pushJson, DeviceInfoPush.class);
                            if (push.getDevice_info() != null && !push.getDevice_info().isEmpty()) {
                                updateDeviceList(push.getDevice_info());
                                Toast.makeText(context, "设备信息已更新", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "收到设备更新，设备数量: " + push.getDevice_info().size());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "解析设备推送失败", e);
                        }
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter("com.example.mineguard.DEVICE_UPDATE");

        // Android 14+ 需要指定 RECEIVER_EXPORTED 或 RECEIVER_NOT_EXPORTED
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(deviceUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(deviceUpdateReceiver, filter);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (deviceUpdateReceiver != null) {
            requireContext().unregisterReceiver(deviceUpdateReceiver);
        }
    }
}
