package com.capstone.cameraex.detect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;

import com.capstone.cameraex.activity.MainActivity;
import com.capstone.cameraex.utils.DetectObject;
import com.capstone.cameraex.utils.ImageProcess;

import java.util.ArrayList;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;

// tts 추가
import android.speech.tts.TextToSpeech;

import javax.xml.transform.Result;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

// FullImageAnalyse 클래스의 일부분

public class FullImageAnalyse implements ImageAnalysis.Analyzer {

    public static class Result {

        long costTime;
        Bitmap bitmap;

        public Result(long costTime, Bitmap bitmap) {
            this.costTime = costTime;
            this.bitmap = bitmap;
        }

        public long getCostTime() {
            return costTime;
        }

        public Bitmap getBitmap() {
            return bitmap;
        }
    }

    // 기존 필드
    private final Context context;
    private TextToSpeech tts;
    ImageView boxLabelCanvas;
    PreviewView previewView;
    TextView inferenceTimeTextView;
    TextView gradientTextView;
    int rotation;
    ImageProcess imageProcess;
    private Detector detector;
    MainActivity mainActivity;

    // 새로 추가된 필드
    private long lastSpokenTime = 0; // 마지막으로 TTS가 실행된 시간
    private static final int TTS_DELAY_MS = 2000; // 2초(2000 밀리초) 지연 시간

    // 기본 생성자
    public FullImageAnalyse(Context context) {
        this.context = context;
        initializeTextToSpeech();
    }

    public FullImageAnalyse(Context context,
                            MainActivity mainActivity,
                            PreviewView previewView,
                            ImageView boxLabelCanvas,
                            int rotation,
                            TextView inferenceTimeTextView,
                            TextView gradientTextView,
                            Detector detector) {
        this.context = context;
        this.mainActivity = mainActivity; // MainActivity 초기화
        this.previewView = previewView;
        this.boxLabelCanvas = boxLabelCanvas;
        this.rotation = rotation;
        this.imageProcess = new ImageProcess();
        this.inferenceTimeTextView = inferenceTimeTextView;
        this.gradientTextView = gradientTextView;
        this.detector = detector;
        initializeTextToSpeech();
    }

    private void initializeTextToSpeech() {
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.KOREAN);
                }
            }
        });
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        int previewHeight = previewView.getHeight();
        int previewWidth = previewView.getWidth();

        Observable.create((ObservableEmitter<Result> emitter) -> {
                    long start = System.currentTimeMillis();

                    byte[][] yuvBytes = new byte[3][];
                    ImageProxy.PlaneProxy[] planes = image.getPlanes();
                    int imageHeight = image.getHeight();
                    int imageWidth = image.getWidth();

                    imageProcess.fillBytes(planes, yuvBytes);
                    int yRowStride = planes[0].getRowStride();
                    final int uvRowStride = planes[1].getRowStride();
                    final int uvPixelStride = planes[1].getPixelStride();

                    int[] rgbBytes = new int[imageHeight * imageWidth];
                    imageProcess.YUV420ToARGB8888(
                            yuvBytes[0],
                            yuvBytes[1],
                            yuvBytes[2],
                            imageWidth,
                            imageHeight,
                            yRowStride,
                            uvRowStride,
                            uvPixelStride,
                            rgbBytes);

                    Bitmap imageBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
                    imageBitmap.setPixels(rgbBytes, 0, imageWidth, 0, 0, imageWidth, imageHeight);
                    double scale = Math.max(
                            previewHeight / (double) (rotation % 180 == 0 ? imageWidth : imageHeight),
                            previewWidth / (double) (rotation % 180 == 0 ? imageHeight : imageWidth)
                    );
                    Matrix fullScreenTransform = imageProcess.getTransformationMatrix(
                            imageWidth, imageHeight,
                            (int) (scale * imageHeight), (int) (scale * imageWidth),
                            rotation % 180 == 0 ? 90 : 0, false
                    );

                    Bitmap fullImageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageWidth, imageHeight, fullScreenTransform, false);
                    Bitmap cropImageBitmap = Bitmap.createBitmap(fullImageBitmap, 0, 0, previewWidth, previewHeight);

                    Matrix previewToModelTransform =
                            imageProcess.getTransformationMatrix(
                                    cropImageBitmap.getWidth(), cropImageBitmap.getHeight(),
                                    detector.getInputSize().getWidth(),
                                    detector.getInputSize().getHeight(),
                                    0, false);
                    Bitmap modelInputBitmap = Bitmap.createBitmap(cropImageBitmap, 0, 0,
                            cropImageBitmap.getWidth(), cropImageBitmap.getHeight(),
                            previewToModelTransform, false);

                    Matrix modelToPreviewTransform = new Matrix();
                    previewToModelTransform.invert(modelToPreviewTransform);

                    ArrayList<DetectObject> recognitions = detector.detect(modelInputBitmap);

                    Bitmap emptyCropSizeBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
                    Canvas cropCanvas = new Canvas(emptyCropSizeBitmap);

                    Paint boxPaint = new Paint();
                    boxPaint.setStrokeWidth(5);
                    boxPaint.setStyle(Paint.Style.STROKE);
                    boxPaint.setColor(Color.RED);
                    Paint textPain = new Paint();
                    textPain.setTextSize(50);
                    textPain.setColor(Color.RED);
                    textPain.setStyle(Paint.Style.FILL);

                    for (DetectObject res : recognitions) {
                        RectF location = res.getLocation();
                        String label = res.getLabelName();
                        float confidence = res.getConfidence();
                        modelToPreviewTransform.mapRect(location);
                        cropCanvas.drawRect(location, boxPaint);
                        cropCanvas.drawText(label + ":" + String.format("%.2f", confidence), location.left, location.top, textPain);

                        // TTS 처리 부분
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastSpokenTime > TTS_DELAY_MS) { // 2초 지연 체크
                            Log.d("TestDetector", label);
                            String totalSpeak = "전방에 " + label + "이 있습니다.";
                            tts.setPitch(1.5f);
                            tts.setSpeechRate(1.0f);
                            tts.speak(totalSpeak, TextToSpeech.QUEUE_FLUSH, null);
                            lastSpokenTime = currentTime; // 마지막 실행 시간을 갱신
                        }

                        if (mainActivity != null) {
                            mainActivity.getLocation();

                        }
                    }

                    long end = System.currentTimeMillis();
                    long costTime = (end - start);
                    image.close();
                    emitter.onNext(new Result(costTime, emptyCropSizeBitmap));
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((Result result) -> {
                    boxLabelCanvas.setImageBitmap(result.bitmap);
                    inferenceTimeTextView.setText(Long.toString(result.costTime) + "ms");
                });
    }
}
