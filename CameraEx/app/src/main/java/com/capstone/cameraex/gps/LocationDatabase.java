package com.capstone.cameraex.gps;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {DetectLocation.class}, version = 1)
public abstract class LocationDatabase extends RoomDatabase {

    public abstract LocationDao locationDao();

    private static volatile LocationDatabase INSTANCE;

    public static LocationDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (LocationDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    LocationDatabase.class, "location_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

