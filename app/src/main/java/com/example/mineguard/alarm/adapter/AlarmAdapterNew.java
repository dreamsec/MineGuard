package com.example.mineguard.alarm.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.PopupWindow;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.mineguard.R;
import com.example.mineguard.alarm.model.AlarmItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 新版报警列表适配器 - 适配API数据结构
 */
public class AlarmAdapterNew extends RecyclerView.Adapter<AlarmAdapterNew.AlarmViewHolder> {

    private List<AlarmItem> alarmList;
    private OnAlarmClickListener listener;
    private SimpleDateFormat dateTimeFormat;

    public interface OnAlarmClickListener {
        void onAlarmClick(AlarmItem alarm);
        void onAlarmLongClick(AlarmItem alarm);
        void onStatusChanged(int position, AlarmItem alarm);
    }

    public AlarmAdapterNew(List<AlarmItem> alarmList, OnAlarmClickListener listener) {
        this.alarmList = alarmList;
        this.listener = listener;
        this.dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    public void updateData(List<AlarmItem> newList) {
        this.alarmList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alarm_card_new, parent, false);
        return new AlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        AlarmItem alarm = alarmList.get(position);
        holder.bind(alarm, position);
    }

    @Override
    public int getItemCount() {
        return alarmList != null ? alarmList.size() : 0;
    }

    class AlarmViewHolder extends RecyclerView.ViewHolder {
        private View viewStatusIndicator;
        private ImageView imageView;
        private ImageView ivVideoIcon;
        private TextView tvStatusBadge;
        private TextView tvAlarmType;
        private TextView tvDeviceName;
        private TextView tvLocation;
        private TextView tvTime;
        private TextView tvProcessUser;

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            viewStatusIndicator = itemView.findViewById(R.id.viewStatusIndicator);
            imageView = itemView.findViewById(R.id.imageView);
            ivVideoIcon = itemView.findViewById(R.id.ivVideoIcon);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvAlarmType = itemView.findViewById(R.id.tvAlarmType);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvProcessUser = itemView.findViewById(R.id.tvProcessUser);
        }

        public void bind(AlarmItem alarm, int position) {
            Context context = itemView.getContext();

            // 1. 设置报警类型
            String detectTarget = alarm.getDetect_target();
            tvAlarmType.setText(detectTarget != null && !detectTarget.isEmpty() ? detectTarget : "未知报警类型");

            // 2. 设置设备名称
            String deviceName = alarm.getDevice_name();
            tvDeviceName.setText(deviceName != null && !deviceName.isEmpty() ? deviceName : "未知设备");

            // 3. 设置场景和区域
            String sceneName = alarm.getScene_name();
            String regionName = alarm.getRegion_name();
            StringBuilder location = new StringBuilder();
            if (sceneName != null && !sceneName.isEmpty()) {
                location.append(sceneName);
            }
            if (regionName != null && !regionName.isEmpty()) {
                if (location.length() > 0) location.append(" - ");
                location.append(regionName);
            }
            tvLocation.setText(location.length() > 0 ? location.toString() : "未知位置");

            // 4. 设置报警时间
            String occurTime = alarm.getOccur_time();
            if (occurTime != null && !occurTime.isEmpty()) {
                // 尝试解析并格式化时间
                try {
                    // 如果时间包含T，可能是ISO格式
                    String displayTime = occurTime.replace("T", " ");
                    if (displayTime.length() > 19) {
                        displayTime = displayTime.substring(0, 19);
                    }
                    tvTime.setText(displayTime);
                } catch (Exception e) {
                    tvTime.setText(occurTime);
                }
            } else {
                tvTime.setText("未知时间");
            }

            // 5. 设置处理人信息
            String processUser = alarm.getProcess_user();
            int processStatus = alarm.getProcess_status();

            if (processStatus == 0) {
                // 未处理
                tvProcessUser.setText("待处理");
                tvProcessUser.setTextColor(Color.parseColor("#FF9800"));
            } else if (processUser != null && !processUser.isEmpty()) {
                // 已处理或有处理人
                tvProcessUser.setText("处理人: " + processUser);
                tvProcessUser.setTextColor(Color.parseColor("#4CAF50"));
            } else {
                tvProcessUser.setText("已处理");
                tvProcessUser.setTextColor(Color.parseColor("#4CAF50"));
            }

            // 6. 设置视频图标
            String alarmVideo = alarm.getAlarm_video();
            if (alarmVideo != null && !alarmVideo.isEmpty()) {
                ivVideoIcon.setVisibility(View.VISIBLE);
            } else {
                ivVideoIcon.setVisibility(View.GONE);
            }

            // 7. 加载图片
            String picUrl = alarm.getAlarm_pic_url();
            if (picUrl != null && !picUrl.isEmpty()) {
                Glide.with(context)
                        .load(picUrl)
                        .centerCrop()
                        .placeholder(R.drawable.ic_alarm_empty)
                        .error(R.drawable.ic_alarm_empty)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.ic_alarm_empty);
            }

            // 8. 设置状态样式
            setupStatusBadge(alarm, position);

            // 9. 点击事件
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onAlarmClick(alarm);
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onAlarmLongClick(alarm);
                return true;
            });
        }

        private void setupStatusBadge(AlarmItem alarm, int position) {
            int status = alarm.getProcess_status();
            int color;
            String text;

            switch (status) {
                case 1: // 已处理
                    color = 0xFF43A047; // 绿色
                    text = "已处理";
                    tvStatusBadge.setOnClickListener(null);
                    break;
                case 2: // 误报
                    color = 0xFFFF9800; // 橙色
                    text = "误报";
                    tvStatusBadge.setOnClickListener(null);
                    break;
                case 0: // 未处理
                default:
                    color = 0xFFD32F2F; // 红色
                    text = "未处理 ▼";
                    tvStatusBadge.setOnClickListener(v -> showStatusPopup(alarm, position));
                    break;
            }

            tvStatusBadge.setText(text);
            setStatusBadgeColor(color);
            viewStatusIndicator.setBackgroundColor(color);
        }

        private void showStatusPopup(AlarmItem alarm, int position) {
            Context context = itemView.getContext();
            View popupView = LayoutInflater.from(context).inflate(R.layout.layout_status_popup, null);

            PopupWindow popupWindow = new PopupWindow(popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true);
            popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            popupWindow.setElevation(10);
            popupWindow.setOutsideTouchable(true);

            TextView tvProcessed = popupView.findViewById(R.id.tvMenuProcessed);
            TextView tvFalseAlarm = popupView.findViewById(R.id.tvMenuFalseAlarm);

            String currentTime = dateTimeFormat.format(new Date());

            tvProcessed.setOnClickListener(v -> {
                alarm.setProcess_status(AlarmItem.STATUS_PROCESSED);
                alarm.setProcess_time(currentTime);
                if (listener != null) listener.onStatusChanged(position, alarm);
                popupWindow.dismiss();
            });

            tvFalseAlarm.setOnClickListener(v -> {
                alarm.setProcess_status(AlarmItem.STATUS_FALSE_ALARM);
                alarm.setProcess_time(currentTime);
                if (listener != null) listener.onStatusChanged(position, alarm);
                popupWindow.dismiss();
            });

            popupWindow.showAsDropDown(tvStatusBadge, 0, 4);
        }

        private void setStatusBadgeColor(int color) {
            android.graphics.drawable.Drawable background = tvStatusBadge.getBackground();
            if (background instanceof GradientDrawable) {
                ((GradientDrawable) background).setColor(color);
            } else {
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(dpToPx(12));
                drawable.setColor(color);
                tvStatusBadge.setBackground(drawable);
            }
        }

        private int dpToPx(int dp) {
            return (int) (itemView.getContext().getResources().getDisplayMetrics().density * dp);
        }
    }
}
