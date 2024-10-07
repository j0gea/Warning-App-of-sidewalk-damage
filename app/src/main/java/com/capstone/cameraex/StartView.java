// MainActivityView.java
package com.capstone.cameraex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import com.capstone.cameraex.activity.ActivitySetting;
import com.capstone.cameraex.activity.MainActivity;

public class StartView extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_view); // 주의: activity_main이 아닌 activity_main_view로 수정

        findViewById(R.id.startButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 탐지 화면으로 이동 (MainActivity로 이동)
                startActivity(new Intent(StartView.this, MainActivity.class));
            }
        });

        findViewById(R.id.settingsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 환경설정 화면으로 이동
                startActivity(new Intent(StartView.this, ActivitySetting.class));
            }
        });
    }
}