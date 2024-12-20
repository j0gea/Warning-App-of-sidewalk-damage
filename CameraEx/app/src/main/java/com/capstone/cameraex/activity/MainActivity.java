package com.capstone.cameraex.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.renderscript.RenderScript;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;

import com.capstone.cameraex.R;
import com.capstone.cameraex.StartView;
import com.capstone.cameraex.detect.Detector;
import com.capstone.cameraex.detect.FullImageAnalyse;
import com.capstone.cameraex.gps.DetectLocation;
import com.capstone.cameraex.gps.GpsService;
import com.capstone.cameraex.gps.LocationDatabase;
import com.capstone.cameraex.utils.CameraProcess;
import com.capstone.cameraex.utils.DetectObject;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.pedro.library.AutoPermissions;
import com.pedro.library.AutoPermissionsListener;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements AutoPermissionsListener, SensorEventListener {

    private PreviewView cameraPreviewMatch;
    private ImageView boxLabel;
    private TextView inferenceTimeText;
    private TextView gradientText;
    private Detector detector;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private CameraProcess cameraProcess = new CameraProcess();

    //DB
    private LocationDatabase db;

    //GPS
    private LocationManager locationManager;
    private LocationListener locationListener;
    private double latitude; //위도
    private double longitude; //경도
    private FusedLocationProviderClient fusedLocationClient;
    private GpsService gpsService = new GpsService();

    // 기울기 센서 관련 변수
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] gravity;
    private float[] geomagnetic;

    // TextToSpeech 변수
    private TextToSpeech tts;


    // 새로 추가된 필드
    private long lastSpokenTime = 0; // 마지막으로 TTS가 실행된 시간
    private static final int TTS_DELAY_MS = 2000; // 2초(2000 밀리초) 지연 시간

    private void initializeTextToSpeech() {
        tts = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.KOREAN);
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //세로 모드 고정

        AutoPermissions.Companion.loadAllPermissions(this, 101);

        // TTS 초기화
        initializeTextToSpeech();

        // 전체화면 설정
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        cameraPreviewMatch = findViewById(R.id.camera_preview_match);
        cameraPreviewMatch.setScaleType(PreviewView.ScaleType.FILL_START);

        boxLabel = findViewById(R.id.box_label);

        inferenceTimeText = findViewById(R.id.inferenceTimeTextView);

        gradientText = findViewById(R.id.gradientTextView);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        db = LocationDatabase.getDatabase(this);

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Log.i("image", "rotation: " + rotation);

        cameraProcess.showCameraSupportSize(MainActivity.this);

        loadModel("best-fp");
        FullImageAnalyse fullImageAnalyse = new FullImageAnalyse(
                MainActivity.this,  // MainActivity 인스턴스 전달
                MainActivity.this,
                cameraPreviewMatch,
                boxLabel,
                rotation,
                inferenceTimeText,
                gradientText,
                detector
        );
        cameraProcess.startCamera(MainActivity.this, fullImageAnalyse, cameraPreviewMatch);

        // 종료 버튼
        findViewById(R.id.endButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 시작화면으로 돌아가기
                startActivity(new Intent(MainActivity.this, StartView.class));
            }
        });

        // 설정 버튼 리스너 추가
        findViewById(R.id.anotherButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 설정 버튼 클릭 시 ActivitySetting으로 이동
                Intent intent = new Intent(MainActivity.this, ActivitySetting.class);
                startActivity(intent);
            }
        });

        // 기울기 센서 초기화
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if (accelerometer != null && magnetometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Log.e("Sensor", "센서를 사용할 수 없습니다.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this); // 액티비티 종료 시 센서 해제

        if (tts != null) { // TTS 자원 해제
            tts.stop();
            tts.shutdown();
        }
    }

    private void loadModel(String modelName) {
        this.detector = new Detector();
        this.detector.setModelFile(modelName);
        this.detector.addGpuDelegate();
        this.detector.initModel(this);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AutoPermissions.Companion.parsePermissions(this, requestCode, permissions, this);
    }

    @Override
    public void onDenied(int requestCode, String[] permissions) {
        Toast.makeText(this, "Permissions denied: " + permissions.length, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGranted(int requestCode, String[] permissions) {
        Toast.makeText(this, "Permissions granted: " + permissions.length, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values;
        }

        if (gravity != null && geomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                float pitch = orientation[1]; // X축 회전 (피치)
                gradientText.setText(String.format("%f",Math.toDegrees(pitch)));

                // 기울기 로그 출력
                if (Math.toDegrees(pitch) > -50 && Math.toDegrees(pitch) < 0) {

                    // TTS 처리 부분
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastSpokenTime > TTS_DELAY_MS && tts != null) { // 2초 지연 체크
                        String totalSpeak = "경사 주의";
                        tts.setPitch(1.5f);
                        tts.setSpeechRate(1.0f);
                        tts.speak(totalSpeak, TextToSpeech.QUEUE_FLUSH, null);
                        lastSpokenTime = currentTime; // 마지막 실행 시간을 갱신
                    }
                }

            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 센서 정확도가 변경될 때 호출되지만, 여기서는 사용하지 않음
    }

    public void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
            return;
        }
        Log.d("LocationDetect","위치 로딩");
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            // 위치 데이터를 얻었다면 DB에 저장
                            Log.d("LocationDetect", "위치 감지: " + location.getLatitude() + ", " + location.getLongitude());
                            gpsService.saveDetectLocation(new DetectLocation(location.getLatitude(), location.getLongitude()));
                        }
                        else {
                            Log.d("LocationDetect", "위치가 null입니다.");
                        }
                    }
                });
    }



}