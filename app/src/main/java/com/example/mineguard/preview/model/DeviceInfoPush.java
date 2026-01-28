package com.example.mineguard.preview.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 设备信息推送数据模型
 * 对应标识字符: dev_infos
 */
public class DeviceInfoPush implements Serializable {
    private Map<Integer, String> scene_info;   // 场景信息: {scene_id: scene_name}
    private Map<Integer, String> region_info;  // 区域信息: {region_id: region_name}
    private List<DeviceInfo> device_info;      // 设备信息列表

    public DeviceInfoPush() {
    }

    public Map<Integer, String> getScene_info() {
        return scene_info;
    }

    public void setScene_info(Map<Integer, String> scene_info) {
        this.scene_info = scene_info;
    }

    public Map<Integer, String> getRegion_info() {
        return region_info;
    }

    public void setRegion_info(Map<Integer, String> region_info) {
        this.region_info = region_info;
    }

    public List<DeviceInfo> getDevice_info() {
        return device_info;
    }

    public void setDevice_info(List<DeviceInfo> device_info) {
        this.device_info = device_info;
    }

    /**
     * 为设备信息填充场景和区域名称
     */
    public void fillDeviceInfoNames() {
        if (device_info == null || device_info.isEmpty()) {
            return;
        }

        for (DeviceInfo device : device_info) {
            // 填充场景名称
            if (scene_info != null && scene_info.containsKey(device.getScene_id())) {
                device.setScene_name(scene_info.get(device.getScene_id()));
            }

            // 填充区域名称
            if (region_info != null && region_info.containsKey(device.getRegion_id())) {
                device.setRegion_name(region_info.get(device.getRegion_id()));
            }
        }
    }
}
