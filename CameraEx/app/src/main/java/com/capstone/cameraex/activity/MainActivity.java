package com.capstone.cameraex.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;

import com.capstone.cameraex.R;
import com.capstone.cameraex.StartView;
import com.capstone.cameraex.detect.Detector;
import com.capstone.cameraex.detect.FullImageAnalyse;
import com.capstone.cameraex.utils.CameraProcess;
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
    private PreviewView cameraPreviewWrap;
    private ImageView boxLabel;
    private Detector detector;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private CameraProcess cameraProcess = new CameraProcess();

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

        AutoPermissions.Companion.loadAllPermissions(this, 101);

        // TTS 초기화
        initializeTextToSpeech();

        // 전체화면 설정
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        cameraPreviewMatch = findViewById(R.id.camera_preview_match);
        cameraPreviewMatch.setScaleType(PreviewView.ScaleType.FILL_START);

        cameraPreviewWrap = findViewById(R.id.camera_preview_wrap);

        boxLabel = findViewById(R.id.box_label);

        TextView textView = findViewById(R.id.textView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Log.i("image", "rotation: " + rotation);

        cameraProcess.showCameraSupportSize(MainActivity.this);

        loadModel("best-fp16");
        cameraPreviewMatch.removeAllViews();
        FullImageAnalyse fullImageAnalyse = new FullImageAnalyse(MainActivity.this,
                cameraPreviewWrap,
                boxLabel,
                rotation,
                detector);
        cameraProcess.startCamera(MainActivity.this, fullImageAnalyse, cameraPreviewWrap);

        // 종료 버튼
        findViewById(R.id.endButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 시작화면으로 돌아가기
                startActivity(new Intent(MainActivity.this, StartView.class));
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
        this.detector.initModel(this);
    }

    /**
     * 이미지 저장 기능 필요시 추가
     **/
    /*
        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean success) {
                if (success) {
                    displayImage();

                    BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
                    Bitmap bitmap = drawable.getBitmap();
                    float[][][] result = detector.detect(bitmap);

                    //3차원 배열
                    String res = Arrays.deepToString(result);

                    Log.d("result",res);
//                    textView.setText(res);
                    saveImage();
//                    uploadImage();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to capture image", Toast.LENGTH_SHORT).show();
                }
            }
        });

        checkStorageDir();
        createImageFile();

        AutoPermissions.Companion.loadAllPermissions(this, 101);
    }

    //이미지 저장
    private void saveImage() {

        try {
            if(photoFile == null){
                Toast.makeText(this, "사진 파일이 생성되지 않았습니다.",Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d("MainActivity", "Saving image to: " + photoFile.getAbsolutePath());

            FileOutputStream outputStream = null;

            try {
                outputStream = new FileOutputStream(photoFile);
                BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
                Bitmap bitmap = drawable.getBitmap();

                if(bitmap == null) {
                    Toast.makeText(this,"저장할 사진이 없습니다.", Toast.LENGTH_SHORT).show();
                }
                else {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream); //JPEG 형식으로 압축
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Toast.makeText(this, "사진 저장 완료", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "저장 실패", Toast.LENGTH_SHORT).show();
        }

    }

//    private void uploadImage() {
//
//    }

    private void checkStorageDir() {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (!storageDir.exists()){
            boolean mkdirs = storageDir.mkdirs();
            if (!mkdirs) {
                Toast.makeText(this, "저장소 폴더 생성 실패",  Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void displayImage() {
        if (photoUri != null) {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getFilesDir();
        try {
            photoFile = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );
            photoUri = FileProvider.getUriForFile(MainActivity.this,
                    "com.capstone.cameraex.fileprovider",
                    photoFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
*/


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

                // 기울기 로그 출력
                if (Math.toDegrees(pitch) > -50 && Math.toDegrees(pitch) < 0) {
                    Log.d("Test", "위험");

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
}
