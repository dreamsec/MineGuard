package com.example.mineguard.home;

/**
 * 报警排行榜数据类
 */
public class AlarmRankingData {
    private String name;
    private int value;

    public AlarmRankingData(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }
}
