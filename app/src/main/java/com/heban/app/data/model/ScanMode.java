package com.heban.app.data.model;

/** 拍照识别模式（D02）。 */
public enum ScanMode {
    PEST("病虫害"),
    CROP("作物 / 杂草");

    public final String label;

    ScanMode(String label) {
        this.label = label;
    }
}
