package com.example.mineguard.alarm;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.bumptech.glide.Glide;
import com.example.mineguard.MainActivity;
import com.example.mineguard.R;
import com.example.mineguard.alarm.model.AlarmItem;
import android.graphics.drawable.Drawable;

/**
 * 报警详情 Activity
 * 用于从通知跳转查看报警详细信息
 */
public class AlarmDetailActivity extends AppCompatActivity {
    private static final String TAG = "AlarmDetailActivity";
    public static final String EXTRA_ALARM_DATA = "alarm_data";
    public static final String EXTRA_FROM_NOTIFICATION = "from_notification";

    private AlarmItem alarmItem;

    // UI 组件
    private LinearLayout headerContainer;
    private ImageView ivAlarmImage;
    private TextView tvDeviceName;
    private TextView tvAlarmType;
    private TextView tvOccurTime;
    private TextView tvSceneName;
    private TextView tvRegionName;
    private TextView tvProcessStatus;
    private TextView tvProcessTime;
    private TextView tvProcessUser;
    private TextView tvProcessDesc;
    private TextView tvResponsiblePerson;
    private TextView tvResponsibleUnit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_detail);

        // 获取传递的报警数据
        alarmItem = (AlarmItem) getIntent().getSerializableExtra(EXTRA_ALARM_DATA);

        if (alarmItem == null) {
            Toast.makeText(this, "报警数据加载失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        displayAlarmDetails();
    }

    private void initializeViews() {
        headerContainer = findViewById(R.id.headerContainer);
        ivAlarmImage = findViewById(R.id.ivAlarmImage);
        tvDeviceName = findViewById(R.id.tvDeviceName);
        tvAlarmType = findViewById(R.id.tvAlarmType);
        tvOccurTime = findViewById(R.id.tvOccurTime);
        tvSceneName = findViewById(R.id.tvSceneName);
        tvRegionName = findViewById(R.id.tvRegionName);
        tvProcessStatus = findViewById(R.id.tvProcessStatus);
        tvProcessTime = findViewById(R.id.tvProcessTime);
        tvProcessUser = findViewById(R.id.tvProcessUser);
        tvProcessDesc = findViewById(R.id.tvProcessDesc);
        tvResponsiblePerson = findViewById(R.id.tvResponsiblePerson);
        tvResponsibleUnit = findViewById(R.id.tvResponsibleUnit);

        // 返回按钮
        findViewById(R.id.btnBack).setOnClickListener(v -> navigateBack());

        // 动态适配状态栏高度
        setupStatusBarPadding();
    }

    /**
     * 返回处理
     * 如果从通知进入，返回到主页；否则正常返回
     */
    @Override
    public void onBackPressed() {
        navigateBack();
    }

    /**
     * 导航返回逻辑
     */
    private void navigateBack() {
        // 检查是否是从通知启动的（通过检查任务栈）
        if (isStartedFromNotification()) {
            // 从通知启动的，返回到主页
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        } else {
            // 正常流程，直接返回
            finish();
        }
    }

    /**
     * 检查是否从通知启动
     */
    private boolean isStartedFromNotification() {
        // 检查是否包含从通知启动的标志
        return getIntent().getBooleanExtra(EXTRA_FROM_NOTIFICATION, false);
    }

    /**
     * 动态适配状态栏高度
     */
    private void setupStatusBarPadding() {
        if (headerContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(headerContainer, (v, windowInsets) -> {
                // 获取系统状态栏的高度
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());

                // 动态设置 Padding:
                // 左(不变), 上(状态栏高度 + 原本的16dp), 右(不变), 下(不变)
                v.setPadding(
                        v.getPaddingLeft(),
                        insets.top + dp2px(16),
                        v.getPaddingRight(),
                        v.getPaddingBottom()
                );

                return windowInsets;
            });
        }
    }

    /**
     * dp 转 px
     */
    private int dp2px(float dpValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private void displayAlarmDetails() {
        try {
            // 报警图片
            if (alarmItem.getAlarm_pic_url() != null && !alarmItem.getAlarm_pic_url().isEmpty()) {
                Glide.with(this)
                        .load(alarmItem.getAlarm_pic_url())
                        .placeholder(R.drawable.placeholder)
                        .into(ivAlarmImage);
            } else {
                // 没有图片时显示占位符
                ivAlarmImage.setImageResource(R.drawable.placeholder);
            }

            // 设备名称
            tvDeviceName.setText(alarmItem.getDevice_name() != null ? alarmItem.getDevice_name() : "未知设备");

            // 报警类型
            tvAlarmType.setText(alarmItem.getDetect_target() != null ? alarmItem.getDetect_target() : "未知类型");

            // 发生时间
            tvOccurTime.setText(alarmItem.getOccur_time() != null ? alarmItem.getOccur_time() : "--");

            // 场景名称
            tvSceneName.setText(alarmItem.getScene_name() != null ? alarmItem.getScene_name() : "--");

            // 区域名称
            tvRegionName.setText(alarmItem.getRegion_name() != null ? alarmItem.getRegion_name() : "--");

            // 处理状态
            String statusText = getProcessStatusText(alarmItem.getProcess_status());
            tvProcessStatus.setText(statusText);

            // 处理时间
            if (alarmItem.getProcess_time() != null && !alarmItem.getProcess_time().isEmpty()) {
                tvProcessTime.setText(alarmItem.getProcess_time());
                tvProcessTime.setVisibility(View.VISIBLE);
            } else {
                tvProcessTime.setVisibility(View.GONE);
            }

            // 处理人
            if (alarmItem.getProcess_user() != null && !alarmItem.getProcess_user().isEmpty()) {
                tvProcessUser.setText(alarmItem.getProcess_user());
                tvProcessUser.setVisibility(View.VISIBLE);
            } else {
                tvProcessUser.setVisibility(View.GONE);
            }

            // 处理说明
            if (alarmItem.getProcess_desc() != null && !alarmItem.getProcess_desc().isEmpty()) {
                tvProcessDesc.setText(alarmItem.getProcess_desc());
                tvProcessDesc.setVisibility(View.VISIBLE);
            } else {
                tvProcessDesc.setVisibility(View.GONE);
            }

            // 责任人
            if (alarmItem.getResponsible_person() != null && !alarmItem.getResponsible_person().isEmpty()) {
                tvResponsiblePerson.setText(alarmItem.getResponsible_person());
                tvResponsiblePerson.setVisibility(View.VISIBLE);
            } else {
                tvResponsiblePerson.setVisibility(View.GONE);
            }

            // 责任单位
            if (alarmItem.getResponsible_unit() != null && !alarmItem.getResponsible_unit().isEmpty()) {
                tvResponsibleUnit.setText(alarmItem.getResponsible_unit());
                tvResponsibleUnit.setVisibility(View.VISIBLE);
            } else {
                tvResponsibleUnit.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            Log.e(TAG, "显示报警详情失败", e);
            Toast.makeText(this, "报警详情显示异常", Toast.LENGTH_SHORT).show();
        }
    }

    private String getProcessStatusText(int status) {
        switch (status) {
            case 0:
                return "未处理";
            case 1:
                return "已处理";
            case 2:
                return "误报";
            default:
                return "未知";
        }
    }
}
