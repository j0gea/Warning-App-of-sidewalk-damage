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

public class MainActivity extends AppCompatActivity implements AutoPermissionsListener {

    private PreviewView cameraPreviewMatch;
    private PreviewView cameraPreviewWrap;
    private ImageView boxLabel;
    private Detector detector;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private CameraProcess cameraProcess = new CameraProcess();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AutoPermissions.Companion.loadAllPermissions(this, 101);

        //전체화면 설정
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
//        getWindow().setStatusBarColor(Color.TRANSPARENT); //상태표시줄 투명하게

        cameraPreviewMatch = findViewById(R.id.camera_preview_match);
        cameraPreviewMatch.setScaleType(PreviewView.ScaleType.FILL_START);

        cameraPreviewWrap = findViewById(R.id.camera_preview_wrap);

        boxLabel = findViewById(R.id.box_label);

        //탐지중
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

        //종료 버튼
        findViewById(R.id.endButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //시작화면으로 돌아가기
                startActivity(new Intent(MainActivity.this, StartView.class));
            }
        });
    }

    private void loadModel(String modelName){
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
}
