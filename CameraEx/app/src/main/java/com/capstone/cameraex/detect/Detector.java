package com.capstone.cameraex.detect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;
import android.util.Size;

import com.capstone.cameraex.utils.DetectObject;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class Detector {

    private String MODEL_FILE;

    //모델 정보 설정
    private final String MODEL_NAME = "best-fp16-g3-1.tflite";
    private final Size INPUT_SIZE = new Size(640, 640);
    private final int[] OUTPUT_SIZE = new int[]{1, 25200, 9};
    private final String LABEL_FILE_NAME = "label.txt";

    //탐지 임계점 설정
    private final float DETECT_THRESHOLD = 0.45f;
    private final float IOU_THRESHOLD = 0.45f;
    private final float IOU_CLASS_DUPLICATED_THRESHOLD = 0.7f;

    private Interpreter tflite;
    private List<String> labels;
    Interpreter.Options options = new Interpreter.Options();

    public Size getInputSize() {
        return this.INPUT_SIZE;
    }


    //모델 초기화
    public void initModel(Context activity) {

        try {
            ByteBuffer model = FileUtil.loadMappedFile(activity, MODEL_NAME);
            tflite = new Interpreter(model, options);
            labels = FileUtil.loadLabels(activity, LABEL_FILE_NAME);

        } catch (IOException e) {
            Log.e("error", "모델 혹은 라벨 읽기에 실패했습니다.", e);
        }
    }

    public void setModelFile(String modelName) {
        switch(modelName) {
            case "best-fp" :
                MODEL_FILE = MODEL_NAME;
        }
    }


    //파손 여부 탐지
    public ArrayList<DetectObject> detect(Bitmap bitmap) {

        TensorImage inputImage;
        ImageProcessor imageProcessor;
        TensorBuffer tensorBuffer;

        imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(INPUT_SIZE.getHeight(), INPUT_SIZE.getWidth(), ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(0, 255))
                .build();
        inputImage = new TensorImage(DataType.FLOAT32);

        inputImage.load(bitmap);
        inputImage = imageProcessor.process(inputImage); //이미지 전처리후 반환

        tensorBuffer = TensorBuffer.createFixedSize(OUTPUT_SIZE, DataType.FLOAT32);


        if (tflite != null) {
            tflite.run(inputImage.getBuffer(), tensorBuffer.getBuffer());
        }

        //output을 1차원 배열로 평탄화
        float[] detectObjectArray = tensorBuffer.getFloatArray();

        ArrayList<DetectObject> allDetectObjectArray = new ArrayList<>();

        for (int i = 0; i < OUTPUT_SIZE[1]; i++) {
            int gridStride = i * OUTPUT_SIZE[2];
            //yolov5 모델이 tflite로 변환될 때 출력값이 이미지 크기로 나누어지므로 다시 곱해줘야함
            float x = detectObjectArray[0 + gridStride] * INPUT_SIZE.getWidth();
            float y = detectObjectArray[1 + gridStride] * INPUT_SIZE.getHeight();
            float w = detectObjectArray[2 + gridStride] * INPUT_SIZE.getWidth();
            float h = detectObjectArray[3 + gridStride] * INPUT_SIZE.getHeight();
            int xmin = (int) Math.max(0, x - w / 2.);
            int ymin = (int) Math.max(0, y - h / 2.);
            int xmax = (int) Math.min(INPUT_SIZE.getWidth(), x + w / 2.);
            int ymax = (int) Math.min(INPUT_SIZE.getHeight(), y + h / 2.);
            float confidence = detectObjectArray[4 + gridStride];
            float[] classScores = Arrays.copyOfRange(detectObjectArray, 5 + gridStride, this.OUTPUT_SIZE[2] + gridStride);

            int labelId = 0;
            float maxLabelScores = 0.f;
            for (int j = 0; j < classScores.length; j++) {
                if (classScores[j] > maxLabelScores) {
                    maxLabelScores = classScores[j];
                    labelId = j;
                }
            }

            DetectObject detectObject = new DetectObject(
                    labelId,
                    "",
                    maxLabelScores,
                    confidence,
                    new RectF(xmin, ymin, xmax, ymax));

            allDetectObjectArray.add(detectObject);
        }

        ArrayList<DetectObject> nmsDetectObjects = nms(allDetectObjectArray);
        ArrayList<DetectObject> nmsDuplicationBoxDetectObjects = nmsAllClass(nmsDetectObjects);

        for(DetectObject detectObject : nmsDuplicationBoxDetectObjects) {
            int labelId = detectObject.getLabelId();
            String labelName = labels.get(labelId);
            detectObject.setLabelName(labelName);
        }

        return nmsDuplicationBoxDetectObjects;
    }

    protected ArrayList<DetectObject> nms(ArrayList<DetectObject> allRecognitions) {
        ArrayList<DetectObject> nmsRecognitions = new ArrayList<DetectObject>();

        //클래스별 NMS
        for (int i = 0; i < OUTPUT_SIZE[2]-5; i++) {
            //각 클래스 별 큐를 생성하여 labelScore 높은 순으로 정렬
            PriorityQueue<DetectObject> pq =
                    new PriorityQueue<DetectObject>(
                            10647,
                            new Comparator<DetectObject>() {
                                @Override
                                public int compare(final DetectObject l, final DetectObject r) {
                                    // Intentionally reversed to put high confidence at the head of the queue.
                                    return Float.compare(r.getConfidence(), l.getConfidence());
                                }
                            });

            //동일한 클래스에 대한 결과 필터링, 신뢰도가 임계값 초과여야함
            for (int j = 0; j < allRecognitions.size(); ++j) {
                if (allRecognitions.get(j).getLabelId() == i && allRecognitions.get(j).getConfidence() > DETECT_THRESHOLD) {
                    pq.add(allRecognitions.get(j));
                }
            }

            //NMS
            while (pq.size() > 0) {
                //확률이 가장 높은 결과 먼저 처리
                DetectObject[] a = new DetectObject[pq.size()];
                DetectObject[] detections = pq.toArray(a);
                DetectObject max = detections[0];
                nmsRecognitions.add(max);
                pq.clear();

                for (int k = 1; k < detections.length; k++) {
                    DetectObject detection = detections[k];
                    if (boxIou(max.getLocation(), detection.getLocation()) < IOU_THRESHOLD) {
                        pq.add(detection);
                    }
                }
            }
        }
        return nmsRecognitions;
    }

    protected ArrayList<DetectObject> nmsAllClass(ArrayList<DetectObject> allRecognitions) {
        ArrayList<DetectObject> nmsRecognitions = new ArrayList<>();

        PriorityQueue<DetectObject> pq =
                new PriorityQueue<DetectObject>(
                        100,
                        new Comparator<DetectObject>() {
                            @Override
                            public int compare(final DetectObject l, final DetectObject r) {
                                return Float.compare(r.getConfidence(), l.getConfidence());
                            }
                        });

        //동일한 클래스에 대한 결과 필터링, 신뢰도 임계값 초과하는 것만 포함
        for (int j = 0; j < allRecognitions.size(); ++j) {
            if (allRecognitions.get(j).getConfidence() > DETECT_THRESHOLD) {
                pq.add(allRecognitions.get(j));
            }
        }

        while (pq.size() > 0) {
            //확률 높은 것 먼저 처리
            DetectObject[] a = new DetectObject[pq.size()];
            DetectObject[] detections = pq.toArray(a);
            DetectObject max = detections[0];
            nmsRecognitions.add(max);
            pq.clear();

            for (int k = 1; k < detections.length; k++) {
                DetectObject detection = detections[k];
                if (boxIou(max.getLocation(), detection.getLocation()) < IOU_CLASS_DUPLICATED_THRESHOLD) {
                    pq.add(detection);
                }
            }
        }
        return nmsRecognitions;
    }


    protected float boxIou(RectF a, RectF b) {
        float intersection = boxIntersection(a, b);
        float union = boxUnion(a, b);
        if (union <= 0) return 1;
        return intersection / union;
    }


    protected float boxIntersection(RectF a, RectF b) {
        float maxLeft = a.left > b.left ? a.left : b.left;
        float maxTop = a.top > b.top ? a.top : b.top;
        float minRight = a.right < b.right ? a.right : b.right;
        float minBottom = a.bottom < b.bottom ? a.bottom : b.bottom;
        float w = minRight -  maxLeft;
        float h = minBottom - maxTop;

        if (w < 0 || h < 0) return 0;
        float area = w * h;
        return area;
    }

    protected float boxUnion(RectF a, RectF b) {
        float i = boxIntersection(a, b);
        float u = (a.right - a.left) * (a.bottom - a.top) + (b.right - b.left) * (b.bottom - b.top) - i;
        return u;
    }
}