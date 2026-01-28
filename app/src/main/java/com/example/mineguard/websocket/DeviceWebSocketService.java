package com.example.mineguard.websocket;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.mineguard.MainActivity;
import com.example.mineguard.R;
import com.example.mineguard.api.ApiConfig;
import com.example.mineguard.preview.model.DeviceInfo;
import com.example.mineguard.preview.model.DeviceInfoPush;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * 设备信息 WebSocket 推送服务
 * 持续监控 WebSocket 通道，接收实时设备信息推送
 * 对应接口: /ws/mobile/push/infos/(user_id)/
 */
public class DeviceWebSocketService extends Service {
    private static final String TAG = "DeviceWebSocketService";
    private static final String CHANNEL_ID = "device_websocket_channel";
    private static final int NOTIFICATION_ID = 3001;

    private OkHttpClient client;
    private WebSocket webSocket;
    private Gson gson;
    private Handler handler;
    private SharedPreferences prefs;

    private String serverIp;
    private String userId;
    private String token;

    private boolean isReconnecting = false;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final long RECONNECT_DELAY = 5000; // 5秒后重连

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Device WebSocket 服务启动");

        gson = new Gson();
        handler = new Handler(Looper.getMainLooper());
        prefs = getSharedPreferences("mineguard_prefs", MODE_PRIVATE);

        // 获取配置
        serverIp = prefs.getString("server_ip", ApiConfig.DEFAULT_SERVER_IP);
        userId = String.valueOf(prefs.getInt("user_id", 0));
        token = prefs.getString("token", "");

        createNotificationChannel();

        // 启动前台服务
        startForeground(NOTIFICATION_ID, createForegroundNotification());

        // 初始化 OkHttpClient
        client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(30, TimeUnit.SECONDS)
                .build();

        // 连接 WebSocket
        connectWebSocket();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: 服务运行中");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Device WebSocket 服务销毁");
        disconnectWebSocket();
    }

    /**
     * 连接 WebSocket
     */
    private void connectWebSocket() {
        if (userId.equals("0")) {
            Log.w(TAG, "用户 ID 未设置，无法连接 WebSocket");
            return;
        }

        // 构建 WebSocket URL
        // 格式：ws://服务器ip:80/ws-api/ws/mobile/push/infos/{user_id}/
        String wsUrl = ApiConfig.getWsUrl(serverIp) + "/ws/mobile/push/infos/" + userId + "/";
        Log.d(TAG, "连接 WebSocket: " + wsUrl);

        Request request = new Request.Builder()
                .url(wsUrl)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket 连接成功");
                reconnectAttempts = 0;
                isReconnecting = false;
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "收到 WebSocket 消息: " + text);
                handleMessage(text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                Log.d(TAG, "收到 WebSocket 二进制消息");
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket 关闭中: " + code + " - " + reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket 已关闭: " + code + " - " + reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket 连接失败", t);
                scheduleReconnect();
            }
        };

        webSocket = client.newWebSocket(request, listener);
    }

    /**
     * 断开 WebSocket 连接
     */
    private void disconnectWebSocket() {
        if (webSocket != null) {
            webSocket.close(1000, "服务关闭");
            webSocket = null;
        }
    }

    /**
     * 处理接收到的消息
     */
    private void handleMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);

            // 检查消息类型标识
            if (json.has("type")) {
                String type = json.getString("type");

                if ("dev_infos".equals(type)) {
                    // 设备信息推送
                    handleDeviceInfoPush(json);
                }
            }

        } catch (JSONException e) {
            Log.e(TAG, "解析 WebSocket 消息失败", e);
        }
    }

    /**
     * 处理设备信息推送
     */
    private void handleDeviceInfoPush(JSONObject pushJson) {
        try {
            // 解析设备信息推送
            DeviceInfoPush push = gson.fromJson(pushJson.toString(), DeviceInfoPush.class);

            // 填充场景和区域名称
            push.fillDeviceInfoNames();

            // 在主线程广播更新事件
            handler.post(() -> {
                broadcastDeviceUpdate(push);
            });

        } catch (Exception e) {
            Log.e(TAG, "解析设备信息失败", e);
        }
    }

    /**
     * 广播设备更新事件
     */
    private void broadcastDeviceUpdate(DeviceInfoPush push) {
        Intent intent = new Intent("com.example.mineguard.DEVICE_UPDATE");
        intent.putExtra("device_push", gson.toJson(push));
        sendBroadcast(intent);
        Log.d(TAG, "已广播设备更新事件，设备数量: " +
                (push.getDevice_info() != null ? push.getDevice_info().size() : 0));
    }

    /**
     * 安排重连
     */
    private void scheduleReconnect() {
        if (isReconnecting || reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "重连次数已达上限，停止重连");
            return;
        }

        isReconnecting = true;
        reconnectAttempts++;

        Log.d(TAG, "将在 " + RECONNECT_DELAY / 1000 + " 秒后第 " + reconnectAttempts + " 次尝试重连");

        handler.postDelayed(() -> {
            connectWebSocket();
        }, RECONNECT_DELAY);
    }

    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "设备监控服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("实时监控设备状态");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * 创建前台服务通知
     */
    private android.app.Notification createForegroundNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("设备监控服务")
                .setContentText("正在监控设备状态...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}
