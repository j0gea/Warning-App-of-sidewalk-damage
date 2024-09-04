package com.capstone.cameraex.gps;

import androidx.room.Dao;
import androidx.room.Insert;

@Dao
public interface LocationDao {

    @Insert
    void insertLocation(DetectLocation location);

}
