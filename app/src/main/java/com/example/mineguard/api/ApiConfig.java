package com.example.mineguard.api;

/**
 * API 配置类
 * 包含所有 API 相关的配置信息
 */
public class ApiConfig {
    // API 基础 URL
    public static final String API_BASE_URL = "http://%s:80/prod-api";
    public static final String WS_BASE_URL = "ws://%s:80/ws-api";

    // 端点路径
    public static final String LOGIN_ENDPOINT = "/api/account/user/login/";

    // Token 请求头
    public static final String AUTH_HEADER = "Authorization";
    public static final String AUTH_PREFIX = "Bearer ";

    // 后门账号密码（用于无网络时登录）
    public static final String BACKDOOR_USERNAME = "admin";
    public static final String BACKDOOR_PASSWORD = "admin123";

    // 默认服务器 IP
    public static final String DEFAULT_SERVER_IP = "192.168.31.225";

    // SharedPreferences 键名
    public static final String PREF_NAME = "MineGuardPrefs";
    public static final String KEY_TOKEN = "token";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_REMEMBER_PASSWORD = "remember_password";
    public static final String KEY_AUTO_LOGIN = "auto_login";
    public static final String KEY_SERVER_IP = "server_ip";
    public static final String KEY_USER_INFO = "user_info";

    /**
     * 获取完整的 API 基础 URL
     * @param serverIp 服务器 IP
     * @return 完整的 API 基础 URL
     */
    public static String getApiBaseUrl(String serverIp) {
        return String.format(API_BASE_URL, serverIp);
    }

    /**
     * 获取登录接口的完整 URL
     * @param serverIp 服务器 IP
     * @return 登录接口完整 URL
     */
    public static String getLoginUrl(String serverIp) {
        return getApiBaseUrl(serverIp) + LOGIN_ENDPOINT;
    }

    /**
     * 获取 WebSocket 的完整 URL
     * @param serverIp 服务器 IP
     * @return WebSocket 完整 URL
     */
    public static String getWsUrl(String serverIp) {
        return String.format(WS_BASE_URL, serverIp);
    }
}
