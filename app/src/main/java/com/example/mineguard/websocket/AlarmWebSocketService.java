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
import com.example.mineguard.alarm.AlarmDetailActivity;
import com.example.mineguard.alarm.model.AlarmItem;
import com.example.mineguard.api.ApiConfig;
import com.google.gson.Gson;

import java.util.Locale;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * 报警 WebSocket 推送服务
 * 持续监控 WebSocket 通道，接收实时报警推送
 */
public class AlarmWebSocketService extends Service {
    private static final String TAG = "AlarmWebSocketService";
    private static final String CHANNEL_ID = "alarm_websocket_channel";
    private static final int NOTIFICATION_ID = 2001;

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
        Log.d(TAG, "WebSocket 服务启动");

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
        return START_STICKY; // 被杀死后自动重启
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // 非绑定服务
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "WebSocket 服务销毁");
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
        // 格式：ws://服务器ip:80/ws-api/ws/mobile/push/info/{user_id}/
        String wsUrl = ApiConfig.getWsUrl(serverIp) + "/ws/mobile/push/info/" + userId + "/";
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

                if ("new_alarm".equals(type)) {
                    // 新报警推送
                    handleNewAlarm(json);
                }
            }

        } catch (JSONException e) {
            Log.e(TAG, "解析 WebSocket 消息失败", e);
        }
    }

    /**
     * 处理新报警推送
     */
    private void handleNewAlarm(JSONObject alarmJson) {
        try {
            // 解析报警数据
            AlarmItem alarm = new AlarmItem();

            if (alarmJson.has("id")) {
                alarm.setId(alarmJson.getInt("id"));
            }

            if (alarmJson.has("device_name")) {
                alarm.setDevice_name(alarmJson.getString("device_name"));
            }

            if (alarmJson.has("detect_target")) {
                alarm.setDetect_target(alarmJson.getString("detect_target"));
            }

            if (alarmJson.has("occur_time")) {
                alarm.setOccur_time(alarmJson.getString("occur_time"));
            }

            if (alarmJson.has("scene_name")) {
                alarm.setScene_name(alarmJson.getString("scene_name"));
            }

            if (alarmJson.has("region_name")) {
                alarm.setRegion_name(alarmJson.getString("region_name"));
            }

            if (alarmJson.has("alarm_pic_url")) {
                alarm.setAlarm_pic_url(alarmJson.getString("alarm_pic_url"));
            }

            if (alarmJson.has("alarm_video")) {
                alarm.setAlarm_video(alarmJson.getString("alarm_video"));
            }

            alarm.setProcess_status(0); // 新报警默认未处理

            // 在主线程发送通知
            handler.post(() -> {
                sendAlarmNotification(alarm);
                broadcastAlarmUpdate(alarm);
            });

        } catch (JSONException e) {
            Log.e(TAG, "解析报警数据失败", e);
        }
    }

    /**
     * 发送报警通知
     */
    private void sendAlarmNotification(AlarmItem alarm) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // 创建点击通知的 Intent
        Intent intent = new Intent(this, AlarmDetailActivity.class);
        intent.putExtra(AlarmDetailActivity.EXTRA_ALARM_DATA, alarm);
        intent.putExtra(AlarmDetailActivity.EXTRA_FROM_NOTIFICATION, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // 构建通知
        String contentText = alarm.getDevice_name() + " - " + alarm.getDetect_target();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("新报警通知")
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(contentText + "\n时间: " + alarm.getOccur_time()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_ALARM);

        // 发送通知（使用报警 ID 作为通知 ID，避免重复）
        notificationManager.notify(alarm.getId(), builder.build());

        Log.d(TAG, "已发送报警通知: " + alarm.getDevice_name());
    }

    /**
     * 广播报警更新事件，通知报警界面刷新
     */
    private void broadcastAlarmUpdate(AlarmItem alarm) {
        Intent intent = new Intent("com.example.mineguard.ALARM_UPDATE");
        intent.putExtra("alarm_data", gson.toJson(alarm));
        sendBroadcast(intent);
        Log.d(TAG, "已广播报警更新事件");
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
     * 创建通知渠道（Android 8.0+ 需要）
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "报警推送服务",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("实时监控报警推送通知");
            channel.enableVibration(true);
            channel.enableLights(true);

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
                .setContentTitle("报警监控服务")
                .setContentText("正在监控报警信息...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    /**
     * 触发离线模拟通知（用于离线登录场景）
     */
    public static void triggerOfflineAlarmNotification(Context context) {
        Log.d(TAG, "触发离线模拟报警通知");

        // 创建模拟报警数据
        AlarmItem mockAlarm = new AlarmItem();
        mockAlarm.setId((int) System.currentTimeMillis());
        mockAlarm.setDevice_name("监控设备 #0" + (int) (Math.random() * 9 + 1));
        mockAlarm.setDetect_target("人员入侵检测");
        mockAlarm.setOccur_time(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                .format(new java.util.Date()));
        mockAlarm.setScene_name("主运输巷");
        mockAlarm.setRegion_name("东区");
        mockAlarm.setAlarm_pic_url("http://via.placeholder.com/400x300/FF5252/FFFFFF?text=报警");
        mockAlarm.setProcess_status(0);

        // 发送通知
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // 确保通知渠道存在
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "报警推送服务",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("实时监控报警推送通知");
            channel.enableVibration(true);
            channel.enableLights(true);
            notificationManager.createNotificationChannel(channel);
        }

        // 创建点击通知的 Intent
        Intent intent = new Intent(context, AlarmDetailActivity.class);
        intent.putExtra(AlarmDetailActivity.EXTRA_ALARM_DATA, mockAlarm);
        intent.putExtra(AlarmDetailActivity.EXTRA_FROM_NOTIFICATION, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // 构建通知
        String contentText = mockAlarm.getDevice_name() + " - " + mockAlarm.getDetect_target();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("【演示】新报警通知")
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(contentText + "\n时间: " + mockAlarm.getOccur_time()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_ALARM);

        notificationManager.notify(mockAlarm.getId(), builder.build());

        // 广播报警更新事件
        Intent broadcastIntent = new Intent("com.example.mineguard.ALARM_UPDATE");
        broadcastIntent.putExtra("alarm_data", new Gson().toJson(mockAlarm));
        context.sendBroadcast(broadcastIntent);

        Log.d(TAG, "已发送离线模拟报警通知");
    }
}
