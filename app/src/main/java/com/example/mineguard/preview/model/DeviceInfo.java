package com.example.mineguard.preview.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 设备信息数据模型
 * 对应 WebSocket 接口: /ws/mobile/push/infos/(user_id)/
 * 标识字符: dev_infos
 */
public class DeviceInfo implements Serializable {
    private int device_id;
    private String device_name;
    private int scene_id;
    private int region_id;
    private String rtsp;
    private String rtsp2;  // 热成像等双通道设备
    private int online;     // 0-不在线, 1-在线
    private boolean enable; // 设防状态

    // 关联信息（用于显示）
    private String scene_name;
    private String region_name;

    // 构造函数
    public DeviceInfo() {
    }

    public DeviceInfo(int device_id, String device_name, int scene_id, int region_id,
                      String rtsp, int online, boolean enable) {
        this.device_id = device_id;
        this.device_name = device_name;
        this.scene_id = scene_id;
        this.region_id = region_id;
        this.rtsp = rtsp;
        this.online = online;
        this.enable = enable;
    }

    // Getter 和 Setter
    public int getDevice_id() {
        return device_id;
    }

    public void setDevice_id(int device_id) {
        this.device_id = device_id;
    }

    public String getDevice_name() {
        return device_name;
    }

    public void setDevice_name(String device_name) {
        this.device_name = device_name;
    }

    public int getScene_id() {
        return scene_id;
    }

    public void setScene_id(int scene_id) {
        this.scene_id = scene_id;
    }

    public int getRegion_id() {
        return region_id;
    }

    public void setRegion_id(int region_id) {
        this.region_id = region_id;
    }

    public String getRtsp() {
        return rtsp;
    }

    public void setRtsp(String rtsp) {
        this.rtsp = rtsp;
    }

    public String getRtsp2() {
        return rtsp2;
    }

    public void setRtsp2(String rtsp2) {
        this.rtsp2 = rtsp2;
    }

    public int getOnline() {
        return online;
    }

    public void setOnline(int online) {
        this.online = online;
    }

    public boolean isOnline() {
        return online == 1;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getScene_name() {
        return scene_name;
    }

    public void setScene_name(String scene_name) {
        this.scene_name = scene_name;
    }

    public String getRegion_name() {
        return region_name;
    }

    public void setRegion_name(String region_name) {
        this.region_name = region_name;
    }

    /**
     * 获取在线状态文本
     */
    public String getOnlineText() {
        return isOnline() ? "在线" : "离线";
    }

    /**
     * 获取设防状态文本
     */
    public String getEnableText() {
        return isEnable() ? "已设防" : "已撤防";
    }

    /**
     * 获取主 RTSP 流地址
     */
    public String getMainRtsp() {
        return rtsp != null && !rtsp.isEmpty() ? rtsp : rtsp2;
    }

    /**
     * 判断是否有可用的视频流
     */
    public boolean hasVideoStream() {
        String mainRtsp = getMainRtsp();
        return isOnline() && mainRtsp != null && !mainRtsp.isEmpty();
    }
}
