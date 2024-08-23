package com.capstone.cameraex.utils;

import android.graphics.RectF;

public class DetectObject {

    private Integer labelId;
    private String labelName;
    private float labelScore;
    private Float confidence; //신뢰도
    private RectF location; //바운딩 박스 위치

    public DetectObject(int labelId, String labelName, Float labelScore, Float confidence, RectF location) {
        this.labelId = labelId;
        this.labelScore = labelScore;
        this.labelName = labelName;
        this.confidence = confidence;
        this.location = location;
    }

    public Integer getLabelId() {
        return labelId;
    }

    public String getLabelName() {
        return labelName;
    }

    public float getLabelScore() {
        return labelScore;
    }

    public Float getConfidence() {
        return confidence;
    }

    public RectF getLocation() {
        return location;
    }

    public void setLabelId(Integer labelId) {
        this.labelId = labelId;
    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }

    public void setLabelScore(float labelScore) {
        this.labelScore = labelScore;
    }

    public void setConfidence(Float confidence) {
        this.confidence = confidence;
    }

    public void setLocation(RectF location) {
        this.location = location;
    }
}
