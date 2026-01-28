package com.example.mineguard.preview;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mineguard.R;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IVLCVout;

import java.util.ArrayList;

/**
 * RTSP 视频播放 Activity
 * 使用 LibVLC 播放 RTSP 实时流
 * 支持离线模拟播放
 */
public class RtspPlayerActivity extends AppCompatActivity {
    private static final String TAG = "RtspPlayerActivity";

    // UI 组件
    private FrameLayout videoContainer;
    private TextView tvDeviceInfo;
    private TextView tvStatus;
    private Button btnBack;
    private Button btnSwitchChannel;
    private Button btnScreenshot;
    private LinearLayout controlPanel;

    // VLC 播放器
    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;
    private String currentRtspUrl;
    private String rtspUrl;
    private String rtsp2Url;
    private boolean useChannel2 = false;
    private TextureView textureView;

    // 设备信息
    private int deviceId;
    private String deviceName;
    private String sceneName;
    private String regionName;

    // 离线模式
    private boolean isOfflineMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rtsp_player);

        // 获取传递的数据
        Intent intent = getIntent();
        deviceId = intent.getIntExtra("device_id", 0);
        deviceName = intent.getStringExtra("device_name");
        rtspUrl = intent.getStringExtra("rtsp_url");
        rtsp2Url = intent.getStringExtra("rtsp2_url");
        sceneName = intent.getStringExtra("scene_name");
        regionName = intent.getStringExtra("region_name");

        // 检查是否为离线模式（RTSP URL 为空或无效）
        if (rtspUrl == null || rtspUrl.isEmpty() || !rtspUrl.startsWith("rtsp://")) {
            isOfflineMode = true;
        }

        initializeViews();
        setupPlayer();
        updateUI();
    }

    private void initializeViews() {
        videoContainer = findViewById(R.id.videoContainer);
        tvDeviceInfo = findViewById(R.id.tvDeviceInfo);
        tvStatus = findViewById(R.id.tvStatus);
        btnBack = findViewById(R.id.btnBack);
        btnSwitchChannel = findViewById(R.id.btnSwitchChannel);
        btnScreenshot = findViewById(R.id.btnScreenshot);
        controlPanel = findViewById(R.id.controlPanel);

        // 返回按钮
        btnBack.setOnClickListener(v -> finish());

        // 切换通道按钮（如果有双通道）
        if (rtsp2Url != null && !rtsp2Url.isEmpty()) {
            btnSwitchChannel.setVisibility(View.VISIBLE);
            btnSwitchChannel.setOnClickListener(v -> switchChannel());
        } else {
            btnSwitchChannel.setVisibility(View.GONE);
        }

        // 截图按钮
        btnScreenshot.setOnClickListener(v -> takeScreenshot());
    }

    private void setupPlayer() {
        if (isOfflineMode) {
            // 离线模式：显示模拟界面
            showOfflineMode();
            return;
        }

        try {
            // 创建 LibVLC 实例
            libVLC = new LibVLC(this);
            mediaPlayer = new MediaPlayer(libVLC);

            // 创建 TextureView 用于视频显示
            textureView = new TextureView(this);
            videoContainer.addView(textureView);

            IVLCVout vout = mediaPlayer.getVLCVout();
            vout.setVideoSurface(textureView.getSurfaceTexture());
            vout.attachViews();

            // 创建媒体并播放
            currentRtspUrl = rtspUrl;
            Media media = new Media(libVLC, Uri.parse(currentRtspUrl));
            mediaPlayer.setMedia(media);

            mediaPlayer.setEventListener(new MediaPlayer.EventListener() {
                @Override
                public void onEvent(MediaPlayer.Event event) {
                    switch (event.type) {
                        case MediaPlayer.Event.Playing:
                            Log.d(TAG, "视频开始播放");
                            runOnUiThread(() -> tvStatus.setText("正在播放"));
                            break;
                        case MediaPlayer.Event.Paused:
                            Log.d(TAG, "视频暂停");
                            break;
                        case MediaPlayer.Event.Stopped:
                            Log.d(TAG, "视频停止");
                            break;
                        case MediaPlayer.Event.EncounteredError:
                            Log.e(TAG, "播放出错");
                            runOnUiThread(() -> {
                                tvStatus.setText("播放失败");
                                Toast.makeText(RtspPlayerActivity.this, "视频流加载失败", Toast.LENGTH_SHORT).show();
                            });
                            break;
                    }
                }
            });

            mediaPlayer.play();

        } catch (Exception e) {
            Log.e(TAG, "初始化播放器失败", e);
            showOfflineMode();
        }
    }

    /**
     * 显示离线模式（模拟播放）
     */
    private void showOfflineMode() {
        runOnUiThread(() -> {
            tvStatus.setText("离线模式（演示）");
            Toast.makeText(this, "离线模式：显示模拟视频画面", Toast.LENGTH_SHORT).show();

            // 在视频容器中显示占位图
            View offlineView = View.inflate(this, R.layout.layout_offline_video, null);
            videoContainer.removeAllViews();
            videoContainer.addView(offlineView);
        });
    }

    /**
     * 更新 UI 显示
     */
    private void updateUI() {
        StringBuilder info = new StringBuilder();
        info.append(deviceName != null ? deviceName : "未知设备");
        if (sceneName != null && !sceneName.isEmpty()) {
            info.append(" | ").append(sceneName);
        }
        if (regionName != null && !regionName.isEmpty()) {
            info.append(" | ").append(regionName);
        }
        tvDeviceInfo.setText(info.toString());

        if (isOfflineMode) {
            tvStatus.setText("离线模式");
            btnSwitchChannel.setVisibility(View.GONE);
            btnScreenshot.setEnabled(false);
        } else {
            tvStatus.setText("正在连接...");
        }
    }

    /**
     * 切换视频通道（主通道/热成像通道）
     */
    private void switchChannel() {
        if (isOfflineMode) {
            Toast.makeText(this, "离线模式无法切换通道", Toast.LENGTH_SHORT).show();
            return;
        }

        useChannel2 = !useChannel2;
        String newUrl = useChannel2 ? rtsp2Url : rtspUrl;

        if (newUrl == null || newUrl.isEmpty()) {
            Toast.makeText(this, "该通道不可用", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            mediaPlayer.stop();
            Media media = new Media(libVLC, Uri.parse(newUrl));
            mediaPlayer.setMedia(media);
            mediaPlayer.play();

            currentRtspUrl = newUrl;
            btnSwitchChannel.setText(useChannel2 ? "切换到主通道" : "切换到热成像");
            Toast.makeText(this, "已切换到" + (useChannel2 ? "热成像通道" : "主通道"), Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "切换通道失败", e);
            Toast.makeText(this, "切换失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 截图功能
     */
    private void takeScreenshot() {
        if (isOfflineMode) {
            Toast.makeText(this, "离线模式无法截图", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: 实现截图功能
        Toast.makeText(this, "截图功能开发中", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null && !isOfflineMode) {
            mediaPlayer.play();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (libVLC != null) {
            libVLC.release();
            libVLC = null;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
