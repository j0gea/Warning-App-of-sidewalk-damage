package com.capstone.cameraex;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;


import com.pedro.library.AutoPermissions;
import com.pedro.library.AutoPermissionsListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements AutoPermissionsListener {

    ImageView imageView;
    File photoFile;
    Uri photoUri;
    Detector detector;

    ActivityResultLauncher<Uri> takePictureLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView4);

        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePictureLauncher.launch(photoUri);
            }
        });

        //detector 생성하고 초기화
        detector = new Detector(this);
        try {
            detector.init();
        } catch (IOException e){
            Log.d("Detector", "failed to init Detector");
        }

        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean success) {
                if (success) {
                    displayImage();
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
                imageView.setImageBitmap(bitmap);
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
