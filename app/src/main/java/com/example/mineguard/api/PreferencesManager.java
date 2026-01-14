package com.example.mineguard.api;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.mineguard.data.User;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * SharedPreferences 管理类
 * 用于管理应用的持久化数据
 */
public class PreferencesManager {
    private static PreferencesManager instance;
    private SharedPreferences prefs;

    private PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(ApiConfig.PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 获取单例实例
     * @param context 上下文
     * @return PreferencesManager 实例
     */
    public static synchronized PreferencesManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferencesManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * 保存 Token
     * @param token Token 字符串
     */
    public void saveToken(String token) {
        prefs.edit().putString(ApiConfig.KEY_TOKEN, token).apply();
    }

    /**
     * 获取 Token
     * @return Token 字符串
     */
    public String getToken() {
        return prefs.getString(ApiConfig.KEY_TOKEN, null);
    }

    /**
     * 清除 Token
     */
    public void clearToken() {
        prefs.edit().remove(ApiConfig.KEY_TOKEN).apply();
    }

    /**
     * 保存用户名
     * @param username 用户名
     */
    public void saveUsername(String username) {
        prefs.edit().putString(ApiConfig.KEY_USERNAME, username).apply();
    }

    /**
     * 获取用户名
     * @return 用户名
     */
    public String getUsername() {
        return prefs.getString(ApiConfig.KEY_USERNAME, "");
    }

    /**
     * 保存密码（仅用于记住密码功能）
     * @param password 密码
     */
    public void savePassword(String password) {
        prefs.edit().putString(ApiConfig.KEY_PASSWORD, password).apply();
    }

    /**
     * 获取密码
     * @return 密码
     */
    public String getPassword() {
        return prefs.getString(ApiConfig.KEY_PASSWORD, "");
    }

    /**
     * 清除密码
     */
    public void clearPassword() {
        prefs.edit().remove(ApiConfig.KEY_PASSWORD).apply();
    }

    /**
     * 设置记住密码
     * @param remember 是否记住
     */
    public void setRememberPassword(boolean remember) {
        prefs.edit().putBoolean(ApiConfig.KEY_REMEMBER_PASSWORD, remember).apply();
    }

    /**
     * 是否记住密码
     * @return 是否记住
     */
    public boolean isRememberPassword() {
        return prefs.getBoolean(ApiConfig.KEY_REMEMBER_PASSWORD, false);
    }

    /**
     * 设置自动登录
     * @param autoLogin 是否自动登录
     */
    public void setAutoLogin(boolean autoLogin) {
        prefs.edit().putBoolean(ApiConfig.KEY_AUTO_LOGIN, autoLogin).apply();
    }

    /**
     * 是否自动登录
     * @return 是否自动登录
     */
    public boolean isAutoLogin() {
        return prefs.getBoolean(ApiConfig.KEY_AUTO_LOGIN, false);
    }

    /**
     * 保存服务器 IP
     * @param ip IP 地址
     */
    public void saveServerIp(String ip) {
        prefs.edit().putString(ApiConfig.KEY_SERVER_IP, ip).apply();
    }

    /**
     * 获取服务器 IP
     * @return IP 地址
     */
    public String getServerIp() {
        return prefs.getString(ApiConfig.KEY_SERVER_IP, ApiConfig.DEFAULT_SERVER_IP);
    }

    /**
     * 保存用户信息
     * @param user 用户对象
     */
    public void saveUserInfo(User user) {
        try {
            JSONObject json = new JSONObject();
            json.put("id", user.getId());
            json.put("username", user.getUsername());
            json.put("name", user.getName());
            json.put("mobile", user.getMobile());
            json.put("email", user.getEmail());
            json.put("image", user.getImage());
            json.put("department", user.getDepartment());
            json.put("position", user.getPosition());
            json.put("job_id", user.getJob_id());
            json.put("sex", user.getSex());
            json.put("birthday", user.getBirthday());
            json.put("status", user.getStatus());
            json.put("is_admin", user.isIs_admin());
            json.put("is_superuser", user.isIs_superuser());

            prefs.edit().putString(ApiConfig.KEY_USER_INFO, json.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取用户信息
     * @return 用户对象，如果不存在返回 null
     */
    public User getUserInfo() {
        String jsonStr = prefs.getString(ApiConfig.KEY_USER_INFO, null);
        if (jsonStr == null) {
            return null;
        }

        try {
            JSONObject json = new JSONObject(jsonStr);
            User user = new User();
            user.setId(json.getInt("id"));
            user.setUsername(json.getString("username"));
            user.setName(json.getString("name"));
            user.setMobile(json.optString("mobile", ""));
            user.setEmail(json.optString("email", ""));
            user.setImage(json.optString("image", ""));
            user.setDepartment(json.optString("department", ""));
            user.setPosition(json.optString("position", ""));
            user.setJob_id(json.optString("job_id", ""));
            user.setSex(json.optInt("sex", 1));
            user.setBirthday(json.optString("birthday", ""));
            user.setStatus(json.optInt("status", 1));
            user.setIs_admin(json.optBoolean("is_admin", false));
            user.setIs_superuser(json.optBoolean("is_superuser", false));

            return user;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 清除用户信息
     */
    public void clearUserInfo() {
        prefs.edit().remove(ApiConfig.KEY_USER_INFO).apply();
    }

    /**
     * 清除所有登录信息
     */
    public void clearAllLoginInfo() {
        clearToken();
        clearPassword();
        clearUserInfo();
        setAutoLogin(false);
    }
}
