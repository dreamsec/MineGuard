package com.example.mineguard.home;

import static com.example.mineguard.MyApplication.globalIP;
import static com.example.mineguard.MyApplication.token;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 今日概览数据管理器
 */
public class PlatformStatisticsManager {
    private static final String TAG = "PlatformStatisticsManager";

    private JSONObject platformStats;
    private TextView tvProcessedPercent;
    private ProgressBar pbRate;
    private TextView tvTotalAlarmsHeader;
    private TextView tvUntreatedAlarmsHeader;
    private TextView tvTotalDevicesHeader;
    private TextView tvOnlineDevicesHeader;
    private Context context;

    public PlatformStatisticsManager(Context context) {
        this.context = context;
    }

    /**
     * 绑定视图
     */
    public void bindViews(TextView tvProcessedPercent, ProgressBar pbRate,
                         TextView tvTotalAlarmsHeader, TextView tvUntreatedAlarmsHeader,
                         TextView tvTotalDevicesHeader, TextView tvOnlineDevicesHeader) {
        this.tvProcessedPercent = tvProcessedPercent;
        this.pbRate = pbRate;
        this.tvTotalAlarmsHeader = tvTotalAlarmsHeader;
        this.tvUntreatedAlarmsHeader = tvUntreatedAlarmsHeader;
        this.tvTotalDevicesHeader = tvTotalDevicesHeader;
        this.tvOnlineDevicesHeader = tvOnlineDevicesHeader;
    }

    /**
     * 从API获取统计数据
     */
    public void fetchStatisticsData() {
        String baseUrl = "http://" + globalIP + ":80/prod-api";
        String url = baseUrl + "/api/get/index/statistic/data/";
        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder().build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "当日数据API请求失败: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(context, "网络不可用，显示模拟数据", Toast.LENGTH_LONG).show();
                    loadMockData();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "当日数据API响应，状态码: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    Log.d(TAG, "当日数据响应: " + jsonData);
                    try {
                        JSONObject jsonObject = new JSONObject(jsonData);
                        if (jsonObject.getBoolean("success_status")) {
                            JSONObject data = jsonObject.getJSONObject("data");
                            platformStats = data;
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
                    Toast.makeText(context, "数据解析失败，显示模拟数据", Toast.LENGTH_SHORT).show();
                    loadMockData();
                });
            }
        });
    }

    /**
     * 加载并显示平台统计数据
     */
    private void loadData() {
        if (platformStats != null) {
            try {
                int totalDevices = platformStats.getInt("dev_sum_count");
                int onlineDevices = platformStats.getInt("dev_online_count");
                int totalAlarms = platformStats.getInt("alarm_count_today");
                int newAlarms = platformStats.getInt("todo_count_today");

                updateUI(newAlarms, totalAlarms, totalDevices, onlineDevices);

                Log.d(TAG, "平台统计数据更新成功 - 设备总数:" + totalDevices +
                        " 在线设备:" + onlineDevices + " 今日报警:" + totalAlarms + " 新增:" + newAlarms);
            } catch (JSONException e) {
                Log.e(TAG, "解析平台统计数据错误: " + e.getMessage());
                loadMockData();
            }
        } else {
            loadMockData();
        }
    }

    /**
     * 加载模拟数据（离线时使用）
     */
    private void loadMockData() {
        int totalDevices = 24;
        int onlineDevices = 17;
        int totalAlarms = 36;
        int newAlarms = 8;

        updateUI(newAlarms, totalAlarms, totalDevices, onlineDevices);
        Log.d(TAG, "使用模拟数据 - 设备总数:" + totalDevices +
                " 在线设备:" + onlineDevices + " 今日报警:" + totalAlarms + " 新增:" + newAlarms);
    }

    /**
     * 更新UI
     */
    private void updateUI(int totalAlarms, int unprocessedAlarms, int totalDevices, int onlineDevices) {
        int processedAlarms = totalAlarms - unprocessedAlarms;
        int percent = 0;
        if (totalAlarms > 0) {
            percent = (int) (((float) processedAlarms / totalAlarms) * 100);
        }

        if (tvProcessedPercent != null) {
            tvProcessedPercent.setText(percent + "%");
        }
        if (pbRate != null) {
            pbRate.setProgress(percent);
        }
        if (tvTotalAlarmsHeader != null) {
            tvTotalAlarmsHeader.setText(String.valueOf(totalAlarms));
        }
        if (tvUntreatedAlarmsHeader != null) {
            tvUntreatedAlarmsHeader.setText(String.valueOf(unprocessedAlarms));
        }
        if (tvTotalDevicesHeader != null) {
            tvTotalDevicesHeader.setText(String.valueOf(totalDevices));
        }
        if (tvOnlineDevicesHeader != null) {
            tvOnlineDevicesHeader.setText(String.valueOf(onlineDevices));
        }
    }
}
