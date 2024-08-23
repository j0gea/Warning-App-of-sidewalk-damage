package com.capstone.cameraex.detect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;

import com.capstone.cameraex.utils.DetectObject;
import com.capstone.cameraex.utils.ImageProcess;

import java.util.ArrayList;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class FullImageAnalyse implements ImageAnalysis.Analyzer {

    public static class Result{

        public Result(long costTime, Bitmap bitmap) {
            this.costTime = costTime;
            this.bitmap = bitmap;
        }
        long costTime;
        Bitmap bitmap;
    }

    ImageView boxLabelCanvas;
    PreviewView previewView;
    int rotation;
    ImageProcess imageProcess;
    private Detector detector;

    public FullImageAnalyse(Context context,
                            PreviewView previewView,
                            ImageView boxLabelCanvas,
                            int rotation,
                            Detector detector) {
        this.previewView = previewView;
        this.boxLabelCanvas = boxLabelCanvas;
        this.rotation = rotation;
        this.imageProcess = new ImageProcess();
        this.detector = detector;
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        int previewHeight = previewView.getHeight();
        int previewWidth = previewView.getWidth();

        Observable.create( (ObservableEmitter<Result> emitter) -> {
                    long start = System.currentTimeMillis();

                    byte[][] yuvBytes = new byte[3][];
                    ImageProxy.PlaneProxy[] planes = image.getPlanes();
                    int imageHeight = image.getHeight();
                    int imagewWidth = image.getWidth();

                    imageProcess.fillBytes(planes, yuvBytes);
                    int yRowStride = planes[0].getRowStride();
                    final int uvRowStride = planes[1].getRowStride();
                    final int uvPixelStride = planes[1].getPixelStride();

                    int[] rgbBytes = new int[imageHeight * imagewWidth];
                    imageProcess.YUV420ToARGB8888(
                            yuvBytes[0],
                            yuvBytes[1],
                            yuvBytes[2],
                            imagewWidth,
                            imageHeight,
                            yRowStride,
                            uvRowStride,
                            uvPixelStride,
                            rgbBytes);

                    // 원본 이미지 bitmap
                    Bitmap imageBitmap = Bitmap.createBitmap(imagewWidth, imageHeight, Bitmap.Config.ARGB_8888);
                    imageBitmap.setPixels(rgbBytes, 0, imagewWidth, 0, 0, imagewWidth, imageHeight);

                    // 화면에 맞게 조정된 fill_start 형식의 bitmap
                    double scale = Math.max(
                            previewHeight / (double) (rotation % 180 == 0 ? imagewWidth : imageHeight),
                            previewWidth / (double) (rotation % 180 == 0 ? imageHeight : imagewWidth)
                    );
                    Matrix fullScreenTransform = imageProcess.getTransformationMatrix(
                            imagewWidth, imageHeight,
                            (int) (scale * imageHeight), (int) (scale * imagewWidth),
                            rotation % 180 == 0 ? 90 : 0, false
                    );

                    // preview에 맞게 전체 크기로 조정된 bitmap
                    Bitmap fullImageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imagewWidth, imageHeight, fullScreenTransform, false);
                    // 화면에 표시되는 preview 크기만큼 잘라낸 bitmap
                    Bitmap cropImageBitmap = Bitmap.createBitmap(fullImageBitmap, 0, 0, previewWidth, previewHeight);

                    // 모델 입력에 사용할 bitmap
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

                    // 테두리용 Paint 객체
                    Paint boxPaint = new Paint();
                    boxPaint.setStrokeWidth(5);
                    boxPaint.setStyle(Paint.Style.STROKE);
                    boxPaint.setColor(Color.RED);
                    // 텍스트용 Paint 객체
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
                    }
                    long end = System.currentTimeMillis();
                    long costTime = (end - start);
                    image.close();
                    emitter.onNext(new Result(costTime, emptyCropSizeBitmap));


                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((Result result) -> {
                    boxLabelCanvas.setImageBitmap(result.bitmap);
                });

    }
}

