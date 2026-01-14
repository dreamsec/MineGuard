package com.example.mineguard.api;

import android.util.Log;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp 客户端封装
 * 提供统一的网络请求客户端和 Token 拦截器
 */
public class ApiClient {
    private static final String TAG = "ApiClient";
    private static OkHttpClient instance;
    private static String currentToken;

    /**
     * 获取 OkHttpClient 实例（单例模式）
     * @return OkHttpClient 实例
     */
    public static OkHttpClient getClient() {
        if (instance == null) {
            synchronized (ApiClient.class) {
                if (instance == null) {
                    instance = createClient();
                }
            }
        }
        return instance;
    }

    /**
     * 创建 OkHttpClient 实例
     * @return OkHttpClient 实例
     */
    private static OkHttpClient createClient() {
        // 添加日志拦截器
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
                Log.d(TAG, message);
            }
        });
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(new TokenInterceptor())
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 设置当前 Token
     * @param token Token 字符串
     */
    public static void setToken(String token) {
        currentToken = token;
    }

    /**
     * 获取当前 Token
     * @return Token 字符串
     */
    public static String getToken() {
        return currentToken;
    }

    /**
     * 清除 Token
     */
    public static void clearToken() {
        currentToken = null;
    }

    /**
     * Token 拦截器
     * 自动为所有请求添加 Authorization 请求头
     */
    private static class TokenInterceptor implements okhttp3.Interceptor {
        @Override
        public okhttp3.Response intercept(okhttp3.Interceptor.Chain chain) throws IOException {
            Request originalRequest = chain.request();

            // 如果有 Token，添加到请求头
            if (currentToken != null && !currentToken.isEmpty()) {
                Request requestWithToken = originalRequest.newBuilder()
                        .header(ApiConfig.AUTH_HEADER, ApiConfig.AUTH_PREFIX + currentToken)
                        .build();
                return chain.proceed(requestWithToken);
            }

            // 没有 Token，直接执行请求
            return chain.proceed(originalRequest);
        }
    }

    /**
     * 创建 FormBody 构建器
     * @return FormBody.Builder 实例
     */
    public static FormBody.Builder createFormBuilder() {
        return new FormBody.Builder();
    }
}
