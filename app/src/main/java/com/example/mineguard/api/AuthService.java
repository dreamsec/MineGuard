package com.example.mineguard.api;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.example.mineguard.data.LoginResponse;
import com.example.mineguard.data.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 认证服务类
 * 处理用户登录、登出等认证相关操作
 */
public class AuthService {
    private static final String TAG = "AuthService";
    private Context context;
    private OkHttpClient client;
    private PreferencesManager prefsManager;

    public AuthService(Context context) {
        this.context = context;
        this.client = ApiClient.getClient();
        this.prefsManager = PreferencesManager.getInstance(context);
    }

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @param callback 登录结果回调
     */
    public void login(String username, String password, final LoginCallback callback) {
        // 优先检查是否是后门密码，如果是则直接离线登录，不进行 API 访问
        if (password.equals(ApiConfig.BACKDOOR_PASSWORD)) {
            Log.d(TAG, "检测到后门密码，直接离线登录，跳过 API 访问");
            attemptBackdoorLogin(username, password, callback);
            return;
        }

        // 检查网络连接
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.d(TAG, "网络不可用，尝试后门登录");
            attemptBackdoorLogin(username, password, callback);
            return;
        }

        // 网络可用，尝试网络登录
        String serverIp = prefsManager.getServerIp();
        String loginUrl = ApiConfig.getLoginUrl(serverIp);
        String encodedPassword = encodePassword(password);

        FormBody formBody = ApiClient.createFormBuilder()
                .add("username", username)
                .add("password", encodedPassword)
                .build();

        Request request = new Request.Builder()
                .url(loginUrl)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                Log.e(TAG, "网络请求失败: " + e.getMessage());
                // 网络请求失败，尝试后门登录
                attemptBackdoorLogin(username, password, callback);
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                final String responseBody = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "收到响应: " + responseBody);

                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    LoginResponse loginResponse = parseLoginResponse(jsonResponse);

                    if (loginResponse.isSuccess()) {
                        // 登录成功，保存 Token 和用户信息
                        saveLoginInfo(loginResponse);
                    }

                    callback.onResult(loginResponse);
                } catch (JSONException e) {
                    Log.e(TAG, "解析响应失败: " + e.getMessage());
                    // JSON 解析失败，尝试后门登录
                    attemptBackdoorLogin(username, password, callback);
                }
            }
        });
    }

    /**
     * 尝试后门登录（无网络或网络请求失败时使用）
     * @param username 用户名
     * @param password 密码
     * @param callback 登录结果回调
     */
    private void attemptBackdoorLogin(String username, String password, final LoginCallback callback) {
        boolean isBackdoorUser = username.equals(ApiConfig.BACKDOOR_USERNAME)
                && password.equals(ApiConfig.BACKDOOR_PASSWORD);

        LoginResponse response = new LoginResponse();

        if (isBackdoorUser) {
            // 后门账号登录成功
            response.setSuccess_status(true);
            response.setCode(0); // code 为 0 表示后门登录
            response.setMessage("后门登录成功（离线模式）");

            // 创建后门用户
            User user = new User();
            user.setId(0);
            user.setUsername(ApiConfig.BACKDOOR_USERNAME);
            user.setName("后门管理员");
            user.setIs_admin(true);
            user.setIs_superuser(true);
            user.setStatus(1);
            user.setSex(1);
            user.setDepartment("系统管理部");
            user.setPosition("系统管理员");
            user.setJob_id("BACKDOOR_ADMIN");

            response.setData(user);
            response.setToken("BACKDOOR_TOKEN_" + System.currentTimeMillis());

            // 保存登录信息
            saveLoginInfo(response);

            Log.d(TAG, "后门登录成功");
        } else {
            // 后门账号登录失败
            response.setSuccess_status(false);
            response.setCode(401);
            response.setMessage("网络不可用，且账号密码不匹配");
            Log.d(TAG, "后门登录失败");
        }

        callback.onResult(response);
    }

    /**
     * 解析登录响应
     * @param json JSON 对象
     * @return LoginResponse 对象
     * @throws JSONException
     */
    private LoginResponse parseLoginResponse(JSONObject json) throws JSONException {
        LoginResponse response = new LoginResponse();

        response.setSuccess_status(json.getBoolean("success_status"));
        response.setCode(json.getInt("code"));
        response.setMessage(json.getString("message"));

        if (json.has("data") && !json.isNull("data")) {
            JSONObject dataObj = json.getJSONObject("data");
            User user = parseUser(dataObj);
            response.setData(user);
        }

        if (json.has("token")) {
            response.setToken(json.getString("token"));
        }

        return response;
    }

    /**
     * 解析用户信息
     * @param json JSON 对象
     * @return User 对象
     * @throws JSONException
     */
    private User parseUser(JSONObject json) throws JSONException {
        User user = new User();

        user.setId(json.getInt("id"));
        user.setUsername(json.getString("username"));
        user.setName(json.optString("name", ""));
        user.setMobile(json.optString("mobile", ""));
        user.setEmail(json.optString("email", ""));
        user.setImage(json.optString("image", ""));
        user.setDepartment(json.optString("department", ""));
        user.setPosition(json.optString("position", ""));
        user.setJob_id(json.optString("job_id", ""));
        user.setSex(json.optInt("sex", 1));
        user.setBirthday(json.optString("birthday", ""));
        user.setStatus(json.optInt("status", 1));
        user.setDescr(json.optString("descr", ""));
        user.setIs_delete(json.optBoolean("is_delete", false));
        user.setIs_admin(json.optBoolean("is_admin", false));
        user.setIs_superuser(json.optBoolean("is_superuser", false));

        return user;
    }

    /**
     * 保存登录信息
     * @param response 登录响应
     */
    private void saveLoginInfo(LoginResponse response) {
        String token = response.getToken();
        User user = response.getData();

        if (token != null) {
            prefsManager.saveToken(token);
            ApiClient.setToken(token);
        }

        if (user != null) {
            prefsManager.saveUserInfo(user);
        }
    }

    /**
     * 登出
     */
    public void logout() {
        // 清除 Token
        prefsManager.clearToken();
        ApiClient.clearToken();

        // 清除所有登录信息
        prefsManager.clearAllLoginInfo();

        Log.d(TAG, "用户已登出");
    }

    /**
     * 对密码进行 Base64 编码
     * @param password 原始密码
     * @return Base64 编码后的密码
     */
    private String encodePassword(String password) {
        return Base64.encodeToString(password.getBytes(), Base64.NO_WRAP);
    }

    /**
     * 检查是否已登录
     * @return 是否已登录
     */
    public boolean isLoggedIn() {
        String token = prefsManager.getToken();
        User user = prefsManager.getUserInfo();
        return token != null && user != null;
    }

    /**
     * 获取当前登录用户
     * @return 用户对象，如果未登录返回 null
     */
    public User getCurrentUser() {
        return prefsManager.getUserInfo();
    }

    /**
     * 登录结果回调接口
     */
    public interface LoginCallback {
        /**
         * 登录结果回调
         * @param response 登录响应
         */
        void onResult(LoginResponse response);
    }
}
