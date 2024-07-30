package com.capstone.cameraex;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class Detector {

    Context context;
    private static final String MODEL_NAME = "best-fp16.tflite";

    int modelInputChannel, modelInputWidth, modelInputHeight; // 모델 입출력 크기 확인용 변수

    public Detector(Context context) {
        this.context = context;
    }

    //tensorflowlite 파일을 읽어오는 함수
    private ByteBuffer loadModel(String modelName) throws IOException {
        //assets 폴더에 저장된 리소스에 접근하기 위해 assetManager 얻음
        AssetManager assetManager = context.getAssets();

        //tflite 파일명을 전달하고 assetFileDescriptor 얻음
        AssetFileDescriptor afd = assetManager.openFd(modelName);

        //getFileDescriptor로 파일의 FileDescriptor 얻어 읽기/쓰기 권한 받기
        FileInputStream fis = new FileInputStream(afd.getFileDescriptor());
        FileChannel fileChannel = fis.getChannel();
        long startOffer = afd.getStartOffset();
        long declaredLength = afd.getDeclaredLength();

        return  fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffer, declaredLength);
    }

    //모델에 데이터를 입력하고 추론 결과를 전달받을 수 있는 interpreter 클래스
    Interpreter interpreter = null;

    //모델 초기화
    public void init() throws IOException {
        ByteBuffer model = loadModel(MODEL_NAME);
        model.order(ByteOrder.nativeOrder());
        interpreter = new Interpreter(model);

        initModelShape();
    }

    //모델 입출력 크기 확인
    private void initModelShape() {
        Tensor inputTensor = interpreter.getInputTensor(0);
        int[] inputShape = inputTensor.shape();
        modelInputChannel = inputShape[0];
        modelInputWidth = inputShape[1];
        modelInputHeight = inputShape[2];
    }

    //사진 크기 변환
    private Bitmap resizeBitmap(Bitmap bitmap) {
        return Bitmap.createScaledBitmap(bitmap, modelInputWidth, modelInputHeight, false);
    }

    //입력 이미지의 채널과 포맷 변환
    private ByteBuffer convertBitmapToGrayByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bitmap.getByteCount());
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[bitmap.getWidth()*bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int pixel : pixels) {
            int r = pixel >> 16 & 0xFF;
            int g = pixel >> 8 & 0xFF;
            int b = pixel & 0xFF;

            float avgPixelValue = (r + g + b) / 3.0f;
            float normalizedPixelValue = avgPixelValue / 255.0f;

            byteBuffer.putFloat(normalizedPixelValue);
        }
        return byteBuffer;
    }

}
