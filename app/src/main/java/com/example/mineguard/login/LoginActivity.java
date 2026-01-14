package com.example.mineguard.login;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mineguard.MainActivity;
import com.example.mineguard.R;
import com.example.mineguard.api.ApiConfig;
import com.example.mineguard.api.AuthService;
import com.example.mineguard.api.PreferencesManager;
import com.example.mineguard.data.LoginResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * 登录页面
 * 支持网络登录和后门登录（无网络时）
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private CheckBox cbRememberPassword;
    private CheckBox cbAutoLogin;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private View tvServerSettings;

    private PreferencesManager prefsManager;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        initManagers();
        loadSavedData();
        setupListeners();
        checkAutoLogin();
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        cbRememberPassword = findViewById(R.id.cb_remember_password);
        cbAutoLogin = findViewById(R.id.cb_auto_login);
        btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_bar);
        tvServerSettings = findViewById(R.id.tv_server_settings);
    }

    /**
     * 初始化管理器
     */
    private void initManagers() {
        prefsManager = PreferencesManager.getInstance(this);
        authService = new AuthService(this);
    }

    /**
     * 加载保存的数据
     */
    private void loadSavedData() {
        // 加载用户名
        String savedUsername = prefsManager.getUsername();
        if (!TextUtils.isEmpty(savedUsername)) {
            etUsername.setText(savedUsername);
        }

        // 加载记住密码状态
        boolean rememberPassword = prefsManager.isRememberPassword();
        cbRememberPassword.setChecked(rememberPassword);

        // 如果记住密码，加载保存的密码
        if (rememberPassword) {
            String savedPassword = prefsManager.getPassword();
            if (!TextUtils.isEmpty(savedPassword)) {
                etPassword.setText(savedPassword);
            }
        }

        // 加载自动登录状态
        boolean autoLogin = prefsManager.isAutoLogin();
        cbAutoLogin.setChecked(autoLogin);
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 登录按钮点击事件
        btnLogin.setOnClickListener(v -> attemptLogin());

        // 服务器设置点击事件
        tvServerSettings.setOnClickListener(v -> showServerSettingsDialog());

        // 密码框的回车键监听
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            attemptLogin();
            return true;
        });

        // 自动登录状态变化时，自动勾选记住密码
        cbAutoLogin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                cbRememberPassword.setChecked(true);
            }
        });
    }

    /**
     * 检查是否自动登录
     */
    private void checkAutoLogin() {
        boolean autoLogin = prefsManager.isAutoLogin();
        String username = prefsManager.getUsername();
        String password = prefsManager.getPassword();

        if (autoLogin && !TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            // 延迟一点时间，让用户看到登录界面
            btnLogin.postDelayed(() -> {
                etUsername.setText(username);
                etPassword.setText(password);
                attemptLogin();
            }, 500);
        }
    }

    /**
     * 尝试登录
     */
    private void attemptLogin() {
        // 隐藏键盘
        hideKeyboard();

        // 获取输入
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 验证输入
        if (TextUtils.isEmpty(username)) {
            showToast("请输入用户名");
            etUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            showToast("请输入密码");
            etPassword.requestFocus();
            return;
        }

        // 显示加载状态
        showLoading(true);

        // 执行登录
        authService.login(username, password, new AuthService.LoginCallback() {
            @Override
            public void onResult(LoginResponse response) {
                runOnUiThread(() -> {
                    showLoading(false);

                    if (response.isSuccess() || response.isBackdoorLogin()) {
                        // 登录成功
                        handleLoginSuccess(username, password, response);
                    } else {
                        // 登录失败
                        handleLoginFailure(response);
                    }
                });
            }
        });
    }

    /**
     * 处理登录成功
     */
    private void handleLoginSuccess(String username, String password, LoginResponse response) {
        // 保存登录状态
        boolean rememberPassword = cbRememberPassword.isChecked();
        boolean autoLogin = cbAutoLogin.isChecked();

        prefsManager.setRememberPassword(rememberPassword);
        prefsManager.setAutoLogin(autoLogin);
        prefsManager.saveUsername(username);

        if (rememberPassword) {
            prefsManager.savePassword(password);
        } else {
            prefsManager.clearPassword();
        }

        // 显示登录成功消息
        if (response.isBackdoorLogin()) {
            showToast("登录成功（离线模式）");
        } else {
            showToast("登录成功");
        }

        // 跳转到主界面
        navigateToMain();
    }

    /**
     * 处理登录失败
     */
    private void handleLoginFailure(LoginResponse response) {
        String message = response.getMessage();
        if (TextUtils.isEmpty(message)) {
            message = "登录失败，请检查用户名和密码";
        }
        showToast(message);
    }

    /**
     * 跳转到主界面
     */
    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * 显示服务器设置对话框
     */
    private void showServerSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_server_settings, null);
        builder.setView(dialogView);

        TextInputEditText etServerIp = dialogView.findViewById(R.id.et_server_ip);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        // 加载当前服务器 IP
        String currentIp = prefsManager.getServerIp();
        etServerIp.setText(currentIp);

        AlertDialog dialog = builder.create();

        // 取消按钮
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // 确定按钮
        btnConfirm.setOnClickListener(v -> {
            String newIp = etServerIp.getText().toString().trim();
            if (TextUtils.isEmpty(newIp)) {
                showToast("请输入服务器 IP 地址");
                return;
            }

            // 保存服务器 IP
            prefsManager.saveServerIp(newIp);
            showToast("服务器地址已保存");
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * 显示/隐藏加载状态
     */
    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
        }
    }

    /**
     * 隐藏键盘
     */
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * 显示 Toast 提示
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
