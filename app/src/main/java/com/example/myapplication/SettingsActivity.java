// SettingsActivity.java
package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private EditText personalInfoEditText;
    private EditText caregiverContactEditText;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        personalInfoEditText = findViewById(R.id.personalInfo);
        caregiverContactEditText = findViewById(R.id.caregiverContact);
        sharedPreferences = getSharedPreferences("OurChairSettings", MODE_PRIVATE);

        // 이전에 저장된 값을 로드
        personalInfoEditText.setText(sharedPreferences.getString("personalInfo", ""));
        caregiverContactEditText.setText(sharedPreferences.getString("caregiverContact", ""));

        findViewById(R.id.saveSettingsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveSettings();
            }
        });
    }

    private void saveSettings() {
        String personalInfo = personalInfoEditText.getText().toString();
        String caregiverContact = caregiverContactEditText.getText().toString();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("personalInfo", personalInfo);
        editor.putString("caregiverContact", caregiverContact);
        editor.apply();
    }
}
