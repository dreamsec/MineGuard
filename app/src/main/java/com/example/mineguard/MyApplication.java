package com.example.mineguard;

import android.app.Application;

import com.example.mineguard.api.ApiClient;
import com.example.mineguard.api.ApiConfig;
import com.example.mineguard.api.PreferencesManager;

/**
 * 全局应用类
 * 管理全局变量和应用初始化
 */
public class MyApplication extends Application {
    // 定义全局变量（保留兼容性）
    public static String token;
    public static String globalIP;
    public static String globalIP1;
    public static String globalRtsp;

    private PreferencesManager prefsManager;

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化 PreferencesManager
        prefsManager = PreferencesManager.getInstance(this);

        // 加载保存的配置
        loadSavedConfig();

        // 初始化全局变量（保持向后兼容）
        initGlobalVariables();
    }

    /**
     * 加载保存的配置
     */
    private void loadSavedConfig() {
        // 加载服务器 IP
        String serverIp = prefsManager.getServerIp();
        if (serverIp != null && !serverIp.isEmpty()) {
            globalIP = serverIp;
        } else {
            globalIP = ApiConfig.DEFAULT_SERVER_IP;
        }

        // 加载 Token
        String savedToken = prefsManager.getToken();
        if (savedToken != null && !savedToken.isEmpty()) {
            token = savedToken;
            ApiClient.setToken(savedToken);
        }
    }

    /**
     * 初始化全局变量（向后兼容）
     */
    private void initGlobalVariables() {
        // globalIP1 保留用于其他用途
//        globalIP1 = "10.34.8.66";

        // globalRtsp 保留用于视频流
//        globalRtsp = "rtsp://admin:cs123456@192.168.1.108";
    }

    /**
     * 设置全局 IP（向后兼容）
     * @param globalIP 全局 IP 地址
     */
    public static void setGlobalIP(String globalIP) {
        MyApplication.globalIP = globalIP;
    }

    /**
     * 设置全局 IP1（向后兼容）
     * @param globalIP1 全局 IP1 地址
     */
    public static void setGlobalIP1(String globalIP1) {
        MyApplication.globalIP1 = globalIP1;
    }

    /**
     * 获取当前服务器 IP
     * @return 服务器 IP
     */
    public String getCurrentServerIp() {
        return globalIP;
    }

    /**
     * 获取当前 Token
     * @return Token 字符串
     */
    public String getCurrentToken() {
        return token;
    }

    /**
     * 设置 Token
     * @param token Token 字符串
     */
    public void setCurrentToken(String token) {
        this.token = token;
        prefsManager.saveToken(token);
        ApiClient.setToken(token);
    }

    /**
     * 清除 Token
     */
    public void clearCurrentToken() {
        this.token = null;
        prefsManager.clearToken();
        ApiClient.clearToken();
    }
}
