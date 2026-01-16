package com.example.mineguard.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.mineguard.alarm.model.AlarmItem;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 报警API服务类
 * 对应API文档 6-11
 */
public class AlarmApiService {
    private static final String TAG = "AlarmApiService";
    private static final String PREF_NAME = "alarm_cache";
    private static final String KEY_ALARM_LIST = "alarm_list_cache";
    private static final String KEY_DICT_SCENE = "dict_scene_cache";
    private static final String KEY_DICT_REGION = "dict_region_cache";
    private static final String KEY_DICT_ALGORITHM = "dict_algorithm_cache";

    private static AlarmApiService instance;
    private OkHttpClient client;
    private Gson gson;
    private Context context;

    private AlarmApiService(Context context) {
        this.context = context.getApplicationContext();
        this.client = ApiClient.getClient();
        this.gson = new Gson();
    }

    public static synchronized AlarmApiService getInstance(Context context) {
        if (instance == null) {
            instance = new AlarmApiService(context);
        }
        return instance;
    }

    /**
     * 6. 获取报警列表
     * URL: /api/get/index/alarm/info/list/
     */
    public void getAlarmList(AlarmListRequest request, AlarmApiCallback<AlarmListResponse> callback) {
        String serverIp = getServerIp();
        String url = ApiConfig.getApiBaseUrl(serverIp) + "/api/get/index/alarm/info/list/";

        FormBody.Builder formBuilder = ApiClient.createFormBuilder();
        formBuilder.add("offset", String.valueOf(request.offset));
        formBuilder.add("pageNum", String.valueOf(request.pageNum));
        formBuilder.add("limit", String.valueOf(request.limit));

        if (request.device_name != null && !request.device_name.isEmpty()) {
            formBuilder.add("device_name", request.device_name);
        }
        if (request.region > 0) {
            formBuilder.add("region", String.valueOf(request.region));
        }
        if (request.scene > 0) {
            formBuilder.add("scene", String.valueOf(request.scene));
        }
        if (request.algorithm > 0) {
            formBuilder.add("algorithm", String.valueOf(request.algorithm));
        }
        if (request.process_status >= 0) {
            formBuilder.add("process_status", String.valueOf(request.process_status));
        }
        if (request.begin_time != null && !request.begin_time.isEmpty()) {
            formBuilder.add("begin_time", request.begin_time);
        }
        if (request.end_time != null && !request.end_time.isEmpty()) {
            formBuilder.add("end_time", request.end_time);
        }

        Request httpRequest = new Request.Builder()
                .url(url)
                .post(formBuilder.build())
                .build();

        client.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取报警列表失败", e);
                callback.onError("网络请求失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonString = response.body() != null ? response.body().string() : "";
                    JsonObject jsonResponse = new JsonParser().parse(jsonString).getAsJsonObject();

                    if (jsonResponse.get("success_status").getAsBoolean()) {
                        AlarmListResponse listResponse = gson.fromJson(jsonResponse, AlarmListResponse.class);

                        // 缓存数据到本地
                        cacheAlarmList(listResponse);

                        callback.onSuccess(listResponse);
                    } else {
                        String message = jsonResponse.get("message").getAsString();
                        callback.onError(message);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析报警列表失败", e);
                    callback.onError("数据解析失败");
                }
            }
        });
    }

    /**
     * 7. 获取或下载报警图片
     * URL: /api/down/alarm/info/pic/
     */
    public void getAlarmPicture(int alarmId, AlarmApiCallback<PictureResponse> callback) {
        String serverIp = getServerIp();
        String url = ApiConfig.getApiBaseUrl(serverIp) + "/api/down/alarm/info/pic/";

        FormBody.Builder formBuilder = ApiClient.createFormBuilder();
        formBuilder.add("id", String.valueOf(alarmId));

        Request httpRequest = new Request.Builder()
                .url(url)
                .post(formBuilder.build())
                .build();

        client.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取报警图片失败", e);
                callback.onError("网络请求失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonString = response.body() != null ? response.body().string() : "";
                    JsonObject jsonResponse = new JsonParser().parse(jsonString).getAsJsonObject();

                    if (jsonResponse.get("success_status").getAsBoolean()) {
                        PictureResponse pictureResponse = gson.fromJson(jsonResponse, PictureResponse.class);
                        callback.onSuccess(pictureResponse);
                    } else {
                        String message = jsonResponse.get("message").getAsString();
                        callback.onError(message);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析报警图片失败", e);
                    callback.onError("数据解析失败");
                }
            }
        });
    }

    /**
     * 8. 获取或下载报警视频
     * URL: /api/down/alarm/info/video/
     */
    public void getAlarmVideo(int alarmId, AlarmApiCallback<VideoResponse> callback) {
        String serverIp = getServerIp();
        String url = ApiConfig.getApiBaseUrl(serverIp) + "/api/down/alarm/info/video/";

        FormBody.Builder formBuilder = ApiClient.createFormBuilder();
        formBuilder.add("id", String.valueOf(alarmId));

        Request httpRequest = new Request.Builder()
                .url(url)
                .post(formBuilder.build())
                .build();

        client.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取报警视频失败", e);
                callback.onError("网络请求失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonString = response.body() != null ? response.body().string() : "";
                    JsonObject jsonResponse = new JsonParser().parse(jsonString).getAsJsonObject();

                    if (jsonResponse.get("success_status").getAsBoolean()) {
                        VideoResponse videoResponse = gson.fromJson(jsonResponse, VideoResponse.class);
                        callback.onSuccess(videoResponse);
                    } else {
                        String message = jsonResponse.get("message").getAsString();
                        callback.onError(message);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析报警视频失败", e);
                    callback.onError("数据解析失败");
                }
            }
        });
    }

    /**
     * 9. 获取报警信息场景字典
     * URL: /api/get/device/scene/dict/
     */
    public void getSceneDict(AlarmApiCallback<Map<String, String>> callback) {
        String serverIp = getServerIp();
        String url = ApiConfig.getApiBaseUrl(serverIp) + "/api/get/device/scene/dict/";

        Request httpRequest = new Request.Builder()
                .url(url)
                .post(new FormBody.Builder().build())
                .build();

        client.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取场景字典失败", e);
                // 尝试从缓存读取
                Map<String, String> cached = getCachedSceneDict();
                if (cached != null && !cached.isEmpty()) {
                    callback.onSuccess(cached);
                } else {
                    callback.onError("网络请求失败: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonString = response.body() != null ? response.body().string() : "";
                    JsonObject jsonResponse = new JsonParser().parse(jsonString).getAsJsonObject();

                    if (jsonResponse.get("success_status").getAsBoolean()) {
                        JsonObject data = jsonResponse.getAsJsonObject("data");
                        Map<String, String> dict = new HashMap<>();
                        for (String key : data.keySet()) {
                            dict.put(key, data.get(key).getAsString());
                        }

                        // 缓存字典数据
                        cacheSceneDict(dict);

                        callback.onSuccess(dict);
                    } else {
                        String message = jsonResponse.get("message").getAsString();
                        callback.onError(message);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析场景字典失败", e);
                    callback.onError("数据解析失败");
                }
            }
        });
    }

    /**
     * 10. 获取报警信息区域字典
     * URL: /api/get/device/region/dict/
     */
    public void getRegionDict(AlarmApiCallback<Map<String, String>> callback) {
        String serverIp = getServerIp();
        String url = ApiConfig.getApiBaseUrl(serverIp) + "/api/get/device/region/dict/";

        Request httpRequest = new Request.Builder()
                .url(url)
                .post(new FormBody.Builder().build())
                .build();

        client.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取区域字典失败", e);
                // 尝试从缓存读取
                Map<String, String> cached = getCachedRegionDict();
                if (cached != null && !cached.isEmpty()) {
                    callback.onSuccess(cached);
                } else {
                    callback.onError("网络请求失败: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonString = response.body() != null ? response.body().string() : "";
                    JsonObject jsonResponse = new JsonParser().parse(jsonString).getAsJsonObject();

                    if (jsonResponse.get("success_status").getAsBoolean()) {
                        JsonObject data = jsonResponse.getAsJsonObject("data");
                        Map<String, String> dict = new HashMap<>();
                        for (String key : data.keySet()) {
                            dict.put(key, data.get(key).getAsString());
                        }

                        // 缓存字典数据
                        cacheRegionDict(dict);

                        callback.onSuccess(dict);
                    } else {
                        String message = jsonResponse.get("message").getAsString();
                        callback.onError(message);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析区域字典失败", e);
                    callback.onError("数据解析失败");
                }
            }
        });
    }

    /**
     * 11. 获取报警信息算法字典
     * URL: /api/get/device/algorithm/dict/
     */
    public void getAlgorithmDict(AlarmApiCallback<Map<String, String>> callback) {
        String serverIp = getServerIp();
        String url = ApiConfig.getApiBaseUrl(serverIp) + "/api/get/device/algorithm/dict/";

        Request httpRequest = new Request.Builder()
                .url(url)
                .post(new FormBody.Builder().build())
                .build();

        client.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取算法字典失败", e);
                // 尝试从缓存读取
                Map<String, String> cached = getCachedAlgorithmDict();
                if (cached != null && !cached.isEmpty()) {
                    callback.onSuccess(cached);
                } else {
                    callback.onError("网络请求失败: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonString = response.body() != null ? response.body().string() : "";
                    JsonObject jsonResponse = new JsonParser().parse(jsonString).getAsJsonObject();

                    if (jsonResponse.get("success_status").getAsBoolean()) {
                        JsonObject data = jsonResponse.getAsJsonObject("data");
                        Map<String, String> dict = new HashMap<>();
                        for (String key : data.keySet()) {
                            dict.put(key, data.get(key).getAsString());
                        }

                        // 缓存字典数据
                        cacheAlgorithmDict(dict);

                        callback.onSuccess(dict);
                    } else {
                        String message = jsonResponse.get("message").getAsString();
                        callback.onError(message);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析算法字典失败", e);
                    callback.onError("数据解析失败");
                }
            }
        });
    }

    // ========== 离线缓存相关方法 ==========

    private String getServerIp() {
        SharedPreferences prefs = context.getSharedPreferences(ApiConfig.PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(ApiConfig.KEY_SERVER_IP, ApiConfig.DEFAULT_SERVER_IP);
    }

    private void cacheAlarmList(AlarmListResponse response) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = gson.toJson(response);
        prefs.edit().putString(KEY_ALARM_LIST, json).apply();
    }

    private AlarmListResponse getCachedAlarmList() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_ALARM_LIST, null);
        if (json != null) {
            return gson.fromJson(json, AlarmListResponse.class);
        }
        return null;
    }

    private void cacheSceneDict(Map<String, String> dict) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = gson.toJson(dict);
        prefs.edit().putString(KEY_DICT_SCENE, json).apply();
    }

    private Map<String, String> getCachedSceneDict() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_DICT_SCENE, null);
        if (json != null) {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            return gson.fromJson(json, type);
        }
        return null;
    }

    private void cacheRegionDict(Map<String, String> dict) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = gson.toJson(dict);
        prefs.edit().putString(KEY_DICT_REGION, json).apply();
    }

    private Map<String, String> getCachedRegionDict() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_DICT_REGION, null);
        if (json != null) {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            return gson.fromJson(json, type);
        }
        return null;
    }

    private void cacheAlgorithmDict(Map<String, String> dict) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = gson.toJson(dict);
        prefs.edit().putString(KEY_DICT_ALGORITHM, json).apply();
    }

    private Map<String, String> getCachedAlgorithmDict() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_DICT_ALGORITHM, null);
        if (json != null) {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            return gson.fromJson(json, type);
        }
        return null;
    }

    /**
     * 获取缓存的报警列表（离线模式）
     */
    public AlarmListResponse getCachedData() {
        return getCachedAlarmList();
    }

    // ========== 数据模型类 ==========

    /**
     * 报警列表请求参数
     */
    public static class AlarmListRequest {
        public int offset;          // 查询起始位置 (pageNum-1)*limit
        public int pageNum;         // 页码
        public int limit;           // 每页条数
        public String device_name;  // 设备名称（可选）
        public int region;          // 区域id（可选）
        public int scene;           // 场景id（可选）
        public int algorithm;       // 算法id（可选）
        public int process_status;  // 处理状态 0-未处理，1-已处理，2-误报（可选）
        public String begin_time;   // 开始时间（可选）
        public String end_time;     // 结束时间（可选）

        public AlarmListRequest(int pageNum, int limit) {
            this.pageNum = pageNum;
            this.limit = limit;
            this.offset = (pageNum - 1) * limit;
            this.region = 0;
            this.scene = 0;
            this.algorithm = 0;
            this.process_status = -1; // -1表示不筛选
        }
    }

    /**
     * 报警列表响应
     */
    public static class AlarmListResponse {
        public boolean success_status;
        public int code;
        public String message;
        public AlarmListData data;
    }

    public static class AlarmListData {
        public int count;
        public List<AlarmItem> results;
    }

    /**
     * 图片响应
     */
    public static class PictureResponse {
        public boolean success_status;
        public int code;
        public String message;
        public List<PictureData> data;
    }

    public static class PictureData {
        public String address;
    }

    /**
     * 视频响应
     */
    public static class VideoResponse {
        public boolean success_status;
        public int code;
        public String message;
        public List<VideoData> data;
    }

    public static class VideoData {
        public String address;
    }

    /**
     * API回调接口
     */
    public interface AlarmApiCallback<T> {
        void onSuccess(T response);
        void onError(String errorMessage);
    }
}
