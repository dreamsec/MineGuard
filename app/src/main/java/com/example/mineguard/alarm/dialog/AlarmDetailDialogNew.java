package com.example.mineguard.alarm.dialog;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.mineguard.R;
import com.example.mineguard.alarm.model.AlarmItem;
import com.example.mineguard.api.AlarmApiService;
import com.example.mineguard.api.ApiConfig;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.content.ContextCompat;

/**
 * 新版报警详情对话框 - 适配API数据结构
 */
public class AlarmDetailDialogNew extends DialogFragment {

    private static final String TAG = "AlarmDetailDialogNew";
    private static final int REQUEST_STORAGE_PERMISSION = 1;

    private ImageView imageView;
    private TextView tvStatus;
    private TextView tvAlarmId;
    private TextView tvAlarmType;
    private TextView tvDeviceName;
    private TextView tvScene;
    private TextView tvArea;
    private TextView tvOccurTime;
    private TextView tvProcessTime;
    private TextView tvResponsiblePerson;
    private TextView tvResponsibleUnit;
    private TextView tvProcessInfo;
    private TextView tvProcessUser;
    private EditText etProcessInfo;
    private Button btnSave;
    private Button btnExport;
    private Button btnViewVideo;
    private Button btnClose;

    private AlarmItem alarm;
    private AlarmApiService alarmApiService;

    public static AlarmDetailDialogNew newInstance(AlarmItem alarm) {
        AlarmDetailDialogNew dialog = new AlarmDetailDialogNew();
        Bundle args = new Bundle();
        args.putSerializable("alarm", alarm);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            alarm = (AlarmItem) args.getSerializable("alarm");
        }

        alarmApiService = AlarmApiService.getInstance(requireContext());

        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_alarm_detail_new, container, false);

        initViews(view);
        setupData();
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        imageView = view.findViewById(R.id.imageView);
        tvStatus = view.findViewById(R.id.tvStatus);
        tvAlarmId = view.findViewById(R.id.tvAlarmId);
        tvAlarmType = view.findViewById(R.id.tvAlarmType);
        tvDeviceName = view.findViewById(R.id.tvDeviceName);
        tvScene = view.findViewById(R.id.tvScene);
        tvArea = view.findViewById(R.id.tvArea);
        tvOccurTime = view.findViewById(R.id.tvOccurTime);
        tvProcessTime = view.findViewById(R.id.tvProcessTime);
        tvResponsiblePerson = view.findViewById(R.id.tvResponsiblePerson);
        tvResponsibleUnit = view.findViewById(R.id.tvResponsibleUnit);
        tvProcessInfo = view.findViewById(R.id.tvProcessInfo);
        tvProcessUser = view.findViewById(R.id.tvProcessUser);
        etProcessInfo = view.findViewById(R.id.etProcessInfo);
        btnSave = view.findViewById(R.id.btnSave);
        btnExport = view.findViewById(R.id.btnExport);
        btnViewVideo = view.findViewById(R.id.btnViewVideo);
        btnClose = view.findViewById(R.id.btnClose);
    }

    private void setupData() {
        if (alarm == null) return;

        // 1. 加载图片
        String picUrl = alarm.getAlarm_pic_url();
        if (picUrl != null && !picUrl.isEmpty()) {
            Glide.with(this)
                    .load(picUrl)
                    .fitCenter()
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.placeholder);
        }

        // 2. 设置状态
        int processStatus = alarm.getProcess_status();
        switch (processStatus) {
            case 1:
                tvStatus.setText("已处理");
                tvStatus.setTextColor(0xFF43A047);
                tvStatus.setBackgroundResource(R.drawable.bg_status_processed);
                break;
            case 2:
                tvStatus.setText("误报");
                tvStatus.setTextColor(0xFFFF9800);
                tvStatus.setBackgroundResource(R.drawable.bg_status_falsealarm);
                break;
            case 0:
            default:
                tvStatus.setText("未处理");
                tvStatus.setTextColor(0xFFD32F2F);
                tvStatus.setBackgroundResource(R.drawable.bg_status_unprocessed);
                break;
        }

        // 3. 基本信息
        tvAlarmId.setText(String.valueOf(alarm.getId()));

        String detectTarget = alarm.getDetect_target();
        tvAlarmType.setText(detectTarget != null && !detectTarget.isEmpty() ? detectTarget : "未知类型");

        String deviceName = alarm.getDevice_name();
        tvDeviceName.setText(deviceName != null && !deviceName.isEmpty() ? deviceName : "未知设备");

        String sceneName = alarm.getScene_name();
        tvScene.setText(sceneName != null && !sceneName.isEmpty() ? sceneName : "未知场景");

        String regionName = alarm.getRegion_name();
        tvArea.setText(regionName != null && !regionName.isEmpty() ? regionName : "未知区域");

        // 4. 时间信息
        String occurTime = alarm.getOccur_time();
        if (occurTime != null && !occurTime.isEmpty()) {
            tvOccurTime.setText(formatDateTime(occurTime));
        } else {
            tvOccurTime.setText("未知时间");
        }

        String processTime = alarm.getProcess_time();
        if (processTime != null && !processTime.isEmpty()) {
            tvProcessTime.setText(formatDateTime(processTime));
            tvProcessTime.setVisibility(View.VISIBLE);
        } else {
            tvProcessTime.setVisibility(View.GONE);
        }

        // 5. 责任信息
        String responsiblePerson = alarm.getResponsible_person();
        if (responsiblePerson != null && !responsiblePerson.isEmpty()) {
            tvResponsiblePerson.setText(responsiblePerson);
            tvResponsiblePerson.setVisibility(View.VISIBLE);
        } else {
            tvResponsiblePerson.setVisibility(View.GONE);
        }

        String responsibleUnit = alarm.getResponsible_unit();
        if (responsibleUnit != null && !responsibleUnit.isEmpty()) {
            tvResponsibleUnit.setText(responsibleUnit);
            tvResponsibleUnit.setVisibility(View.VISIBLE);
        } else {
            tvResponsibleUnit.setVisibility(View.GONE);
        }

        // 6. 处理信息
        String processDesc = alarm.getProcess_desc();
        if (processDesc != null && !processDesc.isEmpty()) {
            tvProcessInfo.setText(processDesc);
            tvProcessInfo.setVisibility(View.VISIBLE);
            etProcessInfo.setVisibility(View.GONE);
            btnSave.setText("修改处理信息");
        } else {
            tvProcessInfo.setVisibility(View.GONE);
            etProcessInfo.setVisibility(View.VISIBLE);
            btnSave.setText("保存处理信息");
        }

        String processUser = alarm.getProcess_user();
        if (processUser != null && !processUser.isEmpty()) {
            tvProcessUser.setText("处理人: " + processUser);
            tvProcessUser.setVisibility(View.VISIBLE);
        } else {
            tvProcessUser.setVisibility(View.GONE);
        }

        // 7. 显示视频按钮（如果有视频）
        String alarmVideo = alarm.getAlarm_video();
        if (alarmVideo != null && !alarmVideo.isEmpty()) {
            btnViewVideo.setVisibility(View.VISIBLE);
        } else {
            btnViewVideo.setVisibility(View.GONE);
        }
    }

    private String formatDateTime(String dateTime) {
        try {
            // 移除T字符（ISO格式）
            String formatted = dateTime.replace("T", " ");
            if (formatted.length() > 19) {
                formatted = formatted.substring(0, 19);
            }
            return formatted;
        } catch (Exception e) {
            return dateTime;
        }
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> saveProcessInfo());
        btnExport.setOnClickListener(v -> exportImage());
        btnClose.setOnClickListener(v -> dismiss());

        if (btnViewVideo != null) {
            btnViewVideo.setOnClickListener(v -> viewVideo());
        }

        // 点击图片查看大图
        imageView.setOnClickListener(v -> {
            Toast.makeText(getContext(), "图片查看功能", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveProcessInfo() {
        String processInfo = etProcessInfo.getText().toString().trim();
        if (processInfo.isEmpty()) {
            Toast.makeText(getContext(), "请输入处理信息", Toast.LENGTH_SHORT).show();
            return;
        }

        alarm.setProcess_desc(processInfo);
        alarm.setProcess_time(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        alarm.setProcess_status(AlarmItem.STATUS_PROCESSED);

        setupData();
        Toast.makeText(getContext(), "处理信息已保存", Toast.LENGTH_SHORT).show();
    }

    private void exportImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasStoragePermission()) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
            return;
        }

        alarmApiService.getAlarmPicture(alarm.getId(), new AlarmApiService.AlarmApiCallback<AlarmApiService.PictureResponse>() {
            @Override
            public void onSuccess(AlarmApiService.PictureResponse response) {
                if (response.data != null && !response.data.isEmpty()) {
                    downloadImage(response.data.get(0).address);
                } else {
                    String picUrl = alarm.getAlarm_pic_url();
                    if (picUrl != null && !picUrl.isEmpty()) {
                        downloadImage(picUrl);
                    } else {
                        Toast.makeText(getContext(), "没有可导出的图片", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                String picUrl = alarm.getAlarm_pic_url();
                if (picUrl != null && !picUrl.isEmpty()) {
                    downloadImage(picUrl);
                } else {
                    Toast.makeText(getContext(), "获取图片失败: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void downloadImage(String imageUrl) {
        try {
            DownloadManager downloadManager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
            String fileName = "alarm_" + alarm.getId() + "_" + System.currentTimeMillis() + ".jpg";

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageUrl));
            request.setTitle("报警图片导出");
            request.setDescription("导出报警图片到本地");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, fileName);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setMimeType("image/jpeg");

            downloadManager.enqueue(request);
            Toast.makeText(getContext(), "图片导出中，请查看通知栏", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "导出图片失败", e);
            Toast.makeText(getContext(), "图片导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void viewVideo() {
        if (alarm == null || alarm.getAlarm_video() == null || alarm.getAlarm_video().isEmpty()) {
            Toast.makeText(getContext(), "没有可查看的视频", Toast.LENGTH_SHORT).show();
            return;
        }

        alarmApiService.getAlarmVideo(alarm.getId(), new AlarmApiService.AlarmApiCallback<AlarmApiService.VideoResponse>() {
            @Override
            public void onSuccess(AlarmApiService.VideoResponse response) {
                if (response.data != null && !response.data.isEmpty()) {
                    String videoUrl = response.data.get(0).address;
                    playVideo(videoUrl);
                } else {
                    Toast.makeText(getContext(), "视频地址获取失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(getContext(), "获取视频失败: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void playVideo(String videoUrl) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(videoUrl), "video/*");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "播放视频失败", e);
            Toast.makeText(getContext(), "无法播放视频", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            exportImage();
        } else {
            Toast.makeText(getContext(), "需要存储权限才能导出图片", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.95);
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
