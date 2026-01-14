package com.example.mineguard.data;
/**
 * 登录响应数据模型
 */
public class LoginResponse {
    private boolean success_status;
    private int code;
    private String message;
    private User data;
    private String token;

    public LoginResponse() {}

    // Getters and Setters
    public boolean isSuccess_status() {
        return success_status;
    }

    public void setSuccess_status(boolean success_status) {
        this.success_status = success_status;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public User getData() {
        return data;
    }

    public void setData(User data) {
        this.data = data;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    /**
     * 判断登录是否成功
     */
    public boolean isSuccess() {
        return success_status && code == 200;
    }

    /**
     * 判断是否为后门登录（无网络时的本地登录）
     */
    public boolean isBackdoorLogin() {
        return code == 0; // code 为 0 表示后门登录
    }
}
