package com.example.mineguard.alarm.model;

import java.io.Serializable;

/**
 * 报警数据模型
 * 对应API: /api/get/index/alarm/info/list/
 */
public class AlarmItem implements Serializable {
    public static final int STATUS_UNPROCESSED = 0;  // 未处理
    public static final int STATUS_PROCESSED = 1;    // 已处理
    public static final int STATUS_FALSE_ALARM = 2;  // 误报
    public static final String LEVEL_WARNING = "0";  // 警告级别
    public static final String LEVEL_CRITICAL = "1"; // 严重级别

    // API返回字段
    private int id;                      // 报警id
    private String occur_time;           // 报警时间
    private int device_id;               // 设备id
    private String detect_target;        // 报警类型
    private String process_time;         // 处理时间
    private int scene;                   // 所属场景id
    private int region;                  // 所属区域id
    private int process_status;          // 处理状态 0-未处理，1-已处理，2-误报
    private String process_user;         // 处理用户
    private String alarm_pic;            // 报警图片名称
    private String alarm_video;          // 报警视频名称
    private String process_desc;         // 处理说明
    private String responsible_person;   // 责任人
    private String responsible_unit;     // 责任单位
    private int alarm_type;              // 报警类型id
    private String device_name;          // 设备名称
    private String scene_name;           // 场景名称
    private String region_name;          // 区域名称
    private String alarm_pic_url;        // 报警图片路径

    // 兼容旧字段的保留（用于UI展示）
    private String level;         // 报警等级（从detect_target推断）
    private String path;          // 图片路径（alarm_pic或alarm_pic_url）
    private String video_path;    // 视频路径（alarm_video）
    private String processInfo;   // 处理信息（process_desc）

    public AlarmItem() {
    }

    // Getters and Setters

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOccur_time() {
        return occur_time;
    }

    public void setOccur_time(String occur_time) {
        this.occur_time = occur_time;
    }

    public int getDevice_id() {
        return device_id;
    }

    public void setDevice_id(int device_id) {
        this.device_id = device_id;
    }

    public String getDetect_target() {
        return detect_target;
    }

    public void setDetect_target(String detect_target) {
        this.detect_target = detect_target;
    }

    public String getProcess_time() {
        return process_time;
    }

    public void setProcess_time(String process_time) {
        this.process_time = process_time;
    }

    public int getScene() {
        return scene;
    }

    public void setScene(int scene) {
        this.scene = scene;
    }

    public int getRegion() {
        return region;
    }

    public void setRegion(int region) {
        this.region = region;
    }

    public int getProcess_status() {
        return process_status;
    }

    public void setProcess_status(int process_status) {
        this.process_status = process_status;
    }

    public String getProcess_user() {
        return process_user;
    }

    public void setProcess_user(String process_user) {
        this.process_user = process_user;
    }

    public String getAlarm_pic() {
        return alarm_pic;
    }

    public void setAlarm_pic(String alarm_pic) {
        this.alarm_pic = alarm_pic;
    }

    public String getAlarm_video() {
        return alarm_video;
    }

    public void setAlarm_video(String alarm_video) {
        this.alarm_video = alarm_video;
    }

    public String getProcess_desc() {
        return process_desc;
    }

    public void setProcess_desc(String process_desc) {
        this.process_desc = process_desc;
    }

    public String getResponsible_person() {
        return responsible_person;
    }

    public void setResponsible_person(String responsible_person) {
        this.responsible_person = responsible_person;
    }

    public String getResponsible_unit() {
        return responsible_unit;
    }

    public void setResponsible_unit(String responsible_unit) {
        this.responsible_unit = responsible_unit;
    }

    public int getAlarm_type() {
        return alarm_type;
    }

    public void setAlarm_type(int alarm_type) {
        this.alarm_type = alarm_type;
    }

    public String getDevice_name() {
        return device_name;
    }

    public void setDevice_name(String device_name) {
        this.device_name = device_name;
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

    public String getAlarm_pic_url() {
        return alarm_pic_url;
    }

    public void setAlarm_pic_url(String alarm_pic_url) {
        this.alarm_pic_url = alarm_pic_url;
    }

    // 兼容旧UI的getter/setter
    public String getType() {
        return detect_target;
    }

    public void setType(String type) {
        this.detect_target = type;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getPath() {
        return path != null ? path : alarm_pic_url;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getVideo_path() {
        return video_path != null ? video_path : alarm_video;
    }

    public void setVideo_path(String video_path) {
        this.video_path = video_path;
    }

    public int getStatus() {
        return process_status;
    }

    public void setStatus(int status) {
        this.process_status = status;
    }

    public Integer getCamera_id() {
        return device_id;
    }

    public void setCamera_id(Integer camera_id) {
        this.device_id = camera_id != null ? camera_id : 0;
    }

    public String getSolve_time() {
        return process_time;
    }

    public void setSolve_time(String solve_time) {
        this.process_time = solve_time;
    }

    public String getName() {
        return device_name;
    }

    public void setName(String name) {
        this.device_name = name;
    }

    public String getLocation() {
        return region_name;
    }

    public void setLocation(String location) {
        this.region_name = location;
    }

    public String getProcessInfo() {
        return processInfo != null ? processInfo : process_desc;
    }

    public void setProcessInfo(String processInfo) {
        this.processInfo = processInfo;
    }

    // 移除不再使用的字段
    public String getChannel() { return ""; }
    public void setChannel(String channel) {}
    public String getUrl() { return ""; }
    public void setUrl(String url) {}
    public String getIp() { return ""; }
    public void setIp(String ip) {}
    public String getFlow() { return ""; }
    public void setFlow(String flow) {}
    public String[] getVideo_paths() { return null; }
    public void setVideo_paths(String[] video_paths) {}

    /**
     * 获取级别描述
     */
    public String getLevelDescription() {
        return LEVEL_CRITICAL.equals(level) ? "严重" : "警告";
    }

    /**
     * 获取级别颜色
     */
    public int getLevelColor() {
        return LEVEL_CRITICAL.equals(level) ? 0xFFFF4444 : 0xFFFFA726; // 红色或橙色
    }

    /**
     * 是否为严重报警
     */
    public boolean isCritical() {
        return LEVEL_CRITICAL.equals(level);
    }

    /**
     * 是否已处理
     */
    public boolean isProcessed() {
        return process_status == STATUS_PROCESSED;
    }

    /**
     * 是否未处理
     */
    public boolean isUnprocessed() {
        return process_status == STATUS_UNPROCESSED;
    }

    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        return process_status == STATUS_PROCESSED ? "已处理" : "未处理";
    }
    /**
     * 是否为误报
     */
    public boolean isFalseAlarm() {
        return process_status == STATUS_FALSE_ALARM;
    }

    /**
     * 获取状态对应的颜色 (将颜色逻辑封装在 Model 层)
     */
    public int getStatusColor() {
        switch (process_status) {
            case STATUS_PROCESSED:
                return 0xFF43A047; // 绿色
            case STATUS_FALSE_ALARM:
                return 0xFFFF9800; // 橙色
            default:
                return 0xFFD32F2F; // 红色
        }
    }
}