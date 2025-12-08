package com.example.mineguard.alarm.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

// 如果你的项目中已经集成了 Glide，请取消下面这行的注释
// import com.bumptech.glide.Glide;

import com.example.mineguard.R;
import com.example.mineguard.alarm.model.AlarmItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 报警列表适配器 - 配合美化后的 item_alarm_card.xml
 */
public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {

    private List<AlarmItem> alarmList;
    private OnAlarmClickListener listener;
    private SimpleDateFormat dateFormat;

    public interface OnAlarmClickListener {
        void onAlarmClick(AlarmItem alarm);
        void onAlarmLongClick(AlarmItem alarm);
    }

    public AlarmAdapter(List<AlarmItem> alarmList, OnAlarmClickListener listener) {
        this.alarmList = alarmList;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 确保这里加载的是我们要修改的那个 layout 文件
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alarm_card, parent, false);
        return new AlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        AlarmItem alarm = alarmList.get(position);
        holder.bind(alarm);
    }

    @Override
    public int getItemCount() {
        return alarmList.size();
    }

    class AlarmViewHolder extends RecyclerView.ViewHolder {
        // 声明新布局中的控件
        private View viewLevelIndicator; // 左侧颜色条
        private ImageView imageView;     // 图片
        private TextView tvTitle;        // 标题 (类型 + 名称)
        private TextView tvScene;        // 场景/位置
        private TextView tvStatusBadge;  // 右上角状态胶囊
        private TextView tvTime;         // 底部时间

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            // 绑定 item_alarm_card.xml 中的新 ID
            viewLevelIndicator = itemView.findViewById(R.id.viewLevelIndicator);
            imageView = itemView.findViewById(R.id.imageView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvScene = itemView.findViewById(R.id.tvScene);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        public void bind(AlarmItem alarm) {
            Context context = itemView.getContext();

            // --- 1. 设置文本信息 ---

            // 标题：组合显示 "报警类型 #ID"，看起来更专业
            String type = alarm.getType() != null ? alarm.getType() : "未知报警";
            String title = type + " #" + alarm.getId();
            tvTitle.setText(title);

            // 场景/位置：组合位置和IP，增加信息密度
            String location = alarm.getLocation() != null ? alarm.getLocation() : "未知位置";
            String ip = (alarm.getIp() != null && !alarm.getIp().isEmpty()) ? alarm.getIp() : "";
            if (!ip.isEmpty()) {
                tvScene.setText(location + " | " + ip);
            } else {
                tvScene.setText(location);
            }

            // 时间：优先使用 format 格式化当前时间，实际开发建议 AlarmItem 中增加 getTime() 方法
            tvTime.setText(dateFormat.format(new Date()));

            // --- 2. 设置图片 (Glide 部分) ---

            // 假设你的 AlarmItem 有一个 getImageUrl() 方法。如果没有，我们默认显示本地图标。
            // 这里的 R.drawable.ic_alarm_empty 请替换为你项目中真实的默认图

            /* * 如果你已添加 Glide 依赖，请使用以下代码：

            if (alarm.getImageUrl() != null && !alarm.getImageUrl().isEmpty()) {
                Glide.with(context)
                     .load(alarm.getImageUrl())
                     .centerCrop()
                     .placeholder(R.drawable.ic_alarm_empty)
                     .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.ic_alarm_empty);
            }
            */

            // 暂时使用的标准代码 (不依赖 Glide，直接设置资源，保证代码不报错)
            imageView.setImageResource(R.drawable.ic_alarm_empty);


            // --- 3. 美化样式逻辑 (核心部分) ---
            setupStyle(alarm);

            // --- 4. 点击事件 ---
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onAlarmClick(alarm);
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onAlarmLongClick(alarm);
                return true;
            });
        }

        /**
         * 专门处理颜色和状态的方法，保持 bind 代码整洁
         */
        private void setupStyle(AlarmItem alarm) {
            boolean isProcessed = alarm.isProcessed();
            // 获取报警等级颜色，如果 AlarmItem 没有 getLevelColor，可以自己定义逻辑
            int levelColor = alarm.getLevelColor();
            // 如果 getLevelColor 返回 0 或无效值，给个默认红色
            if (levelColor == 0) levelColor = 0xFFD32F2F;

            // A. 设置左侧指示条颜色
            viewLevelIndicator.setBackgroundColor(levelColor);

            // B. 设置状态标签 (Badge)
            if (isProcessed) {
                tvStatusBadge.setText("已处理");
                // 已处理：使用绿色背景
                setStatusBadgeStyle(0xFF43A047); // Green
            } else {
                tvStatusBadge.setText("未处理");
                // 未处理：背景色跟随报警等级 (严重则红，警告则橙)
                setStatusBadgeStyle(levelColor);
            }
        }

        /**
         * 动态修改状态标签的圆角背景颜色
         */
        private void setStatusBadgeStyle(int color) {
            // 获取 XML 中定义的背景 drawable
            android.graphics.drawable.Drawable background = tvStatusBadge.getBackground();

            if (background instanceof GradientDrawable) {
                // 如果是 GradientDrawable，直接修改颜色保留圆角
                ((GradientDrawable) background).setColor(color);
            } else {
                // 如果背景丢失，创建一个新的
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(dpToPx(12)); // 设置圆角 12dp
                drawable.setColor(color);
                tvStatusBadge.setBackground(drawable);
            }
        }

        // dp 转 px 辅助工具
        private int dpToPx(int dp) {
            return (int) (itemView.getContext().getResources().getDisplayMetrics().density * dp);
        }
    }
}