package com.capstone.cameraex;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

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
    int modelOutputClasses;

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

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffer, declaredLength);
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

        //입력 크기 확인
        Tensor inputTensor = interpreter.getInputTensor(0);
        int[] inputShape = inputTensor.shape();
        modelInputChannel = inputShape[0];
        modelInputWidth = inputShape[1];
        modelInputHeight = inputShape[2];

        //출력 형태 확인
        Tensor outputTensor = interpreter.getOutputTensor(0);
        int [] outputShape = outputTensor.shape();
        modelOutputClasses = outputShape[1];

    }


    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        int inputSize = 640; // 모델이 기대하는 입력 크기
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[inputSize * inputSize];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.putFloat(((val >> 16) & 0xFF) / 255.0f);
                byteBuffer.putFloat(((val >> 8) & 0xFF) / 255.0f);
                byteBuffer.putFloat((val & 0xFF) / 255.0f);
            }
        }
        return byteBuffer;

    }

    //도보 파손 여부 탐지
    public float[][][] detect(Bitmap bitmap) {
        int modelInputWidth = 640;
        int modelInputHeight = 640;

        // 입력 이미지를 모델의 기대 크기로 조정
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, modelInputWidth, modelInputHeight, true);

        // 이미지 데이터를 ByteBuffer로 변환
        ByteBuffer byteBuffer = convertBitmapToByteBuffer(resizedBitmap);
        float[][][] result = new float[1][25200][6];
        interpreter.run(byteBuffer, result);

        return result;
        }
    }
