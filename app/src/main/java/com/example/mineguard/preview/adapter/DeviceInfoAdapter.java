package com.example.mineguard.preview.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mineguard.R;
import com.example.mineguard.preview.RtspPlayerActivity;
import com.example.mineguard.preview.model.DeviceInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备信息适配器（新版本）
 * 对应 WebSocket 设备推送接口
 */
public class DeviceInfoAdapter extends RecyclerView.Adapter<DeviceInfoAdapter.DeviceViewHolder> {

    private static final String TAG = "DeviceInfoAdapter";
    private Context context;
    private List<DeviceInfo> deviceList;

    public DeviceInfoAdapter(Context context) {
        this.context = context;
        this.deviceList = new ArrayList<>();
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_device_card_new, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        DeviceInfo device = deviceList.get(position);
        holder.bind(device);
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    /**
     * 更新设备列表
     */
    public void updateDevices(List<DeviceInfo> newDevices) {
        if (newDevices == null) {
            newDevices = new ArrayList<>();
        }

        deviceList.clear();
        deviceList.addAll(newDevices);
        notifyDataSetChanged();
    }

    /**
     * 添加设备
     */
    public void addDevice(DeviceInfo device) {
        deviceList.add(device);
        notifyItemInserted(deviceList.size() - 1);
    }

    /**
     * 清空列表
     */
    public void clear() {
        deviceList.clear();
        notifyDataSetChanged();
    }

    /**
     * ViewHolder
     */
    class DeviceViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDeviceName;
        private TextView tvOnlineStatus;
        private TextView tvSceneName;
        private TextView tvRegionName;
        private TextView tvEnableStatus;
        private ImageView ivPreview;
        private FrameLayout previewContainer;
        private LinearLayout offlineOverlay;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvOnlineStatus = itemView.findViewById(R.id.tvOnlineStatus);
            tvSceneName = itemView.findViewById(R.id.tvSceneName);
            tvRegionName = itemView.findViewById(R.id.tvRegionName);
            tvEnableStatus = itemView.findViewById(R.id.tvEnableStatus);
            ivPreview = itemView.findViewById(R.id.ivPreview);
            previewContainer = itemView.findViewById(R.id.previewContainer);
            offlineOverlay = itemView.findViewById(R.id.offlineOverlay);
        }

        public void bind(DeviceInfo device) {
            // 设备名称
            tvDeviceName.setText(device.getDevice_name() != null ? device.getDevice_name() : "未知设备");

            // 在线状态
            if (device.isOnline()) {
                tvOnlineStatus.setText("在线");
                tvOnlineStatus.setBackgroundResource(R.drawable.bg_status_processed);
                tvOnlineStatus.setTextColor(context.getResources().getColor(android.R.color.white));
                offlineOverlay.setVisibility(View.GONE);
            } else {
                tvOnlineStatus.setText("离线");
                tvOnlineStatus.setBackgroundResource(R.drawable.bg_status_default);
                tvOnlineStatus.setTextColor(context.getResources().getColor(R.color.text_secondary));
                offlineOverlay.setVisibility(View.VISIBLE);
            }

            // 场景名称
            tvSceneName.setText(device.getScene_name() != null ? device.getScene_name() : "未设置");

            // 区域名称
            tvRegionName.setText(device.getRegion_name() != null ? device.getRegion_name() : "未设置");

            // 设防状态
            if (device.isEnable()) {
                tvEnableStatus.setText("已设防");
                tvEnableStatus.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
            } else {
                tvEnableStatus.setText("已撤防");
                tvEnableStatus.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
            }

            // 点击预览容器打开视频播放
            previewContainer.setOnClickListener(v -> {
                if (device.isOnline() && device.hasVideoStream()) {
                    openRtspPlayer(device);
                } else {
                    if (!device.isOnline()) {
                        android.widget.Toast.makeText(context, "设备离线，无法播放", android.widget.Toast.LENGTH_SHORT).show();
                    } else {
                        android.widget.Toast.makeText(context, "该设备无可用视频流", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        /**
         * 打开 RTSP 播放器
         */
        private void openRtspPlayer(DeviceInfo device) {
            Intent intent = new Intent(context, RtspPlayerActivity.class);
            intent.putExtra("device_id", device.getDevice_id());
            intent.putExtra("device_name", device.getDevice_name());
            intent.putExtra("rtsp_url", device.getMainRtsp());
            intent.putExtra("rtsp2_url", device.getRtsp2());
            intent.putExtra("scene_name", device.getScene_name());
            intent.putExtra("region_name", device.getRegion_name());
            context.startActivity(intent);
        }
    }
}
