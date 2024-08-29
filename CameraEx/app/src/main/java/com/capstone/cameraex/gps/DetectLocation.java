package com.capstone.cameraex.gps;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class DetectLocation {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private double latitude; //위도
    private double longitude; //경도

    public DetectLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }


}
