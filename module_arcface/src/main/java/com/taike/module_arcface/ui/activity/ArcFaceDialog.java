package com.taike.module_arcface.ui.activity;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.os.CountDownTimer;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.VersionInfo;
import com.arcsoft.face.enums.DetectFaceOrientPriority;
import com.arcsoft.face.enums.DetectMode;
import com.blankj.utilcode.util.ToastUtils;
import com.taike.lib_utils.DpPxUtils;
import com.taike.module_arcface.arcface.CompareResult;
import com.taike.module_arcface.arcface.FacePreviewInfo;
import com.taike.module_arcface.arcface.FaceServer;
import com.taike.module_arcface.arcface.camera.CameraHelper;
import com.taike.module_arcface.arcface.camera.CameraListener;
import com.taike.module_arcface.arcface.draw.DrawHelper;
import com.taike.module_arcface.arcface.draw.DrawInfo;
import com.taike.module_arcface.arcface.draw.FaceRectView;
import com.taike.module_arcface.arcface.face.ConfigUtil;
import com.taike.module_arcface.arcface.face.FaceHelper;
import com.taike.module_arcface.arcface.face.FaceListener;
import com.taike.module_arcface.arcface.face.LivenessType;
import com.taike.module_arcface.arcface.face.RecognizeColor;
import com.taike.module_arcface.arcface.face.RequestFeatureStatus;
import com.taike.module_arcface.arcface.face.RequestLivenessStatus;
import com.taike.module_test.R;
import com.taike.module_test.databinding.DialogArcfaceBinding;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ArcFaceDialog extends BaseDialog<DialogArcfaceBinding> implements ViewTreeObserver.OnGlobalLayoutListener {

    private OnArcFaceCallback callback;
    private FaceHelper faceHelper;
    private List<CompareResult> compareResultList;
    private CountDownTimer cdTimer;
    private TextView tip;
    private ImageView scanImage;
    private String stuId;
    private Boolean isRegistered = false;//需要识别的人脸是否完成注册

    public ArcFaceDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected int initContentView() {
        return R.layout.dialog_arcface;
    }

    @Override
    protected void initView() {
        tip = binding.tip;
        previewView = binding.singleCameraTexturePreview;
        faceRectView = binding.singleCameraFaceRectView;
        scanImage = binding.scanImage;
        FaceServer.getInstance().init(getContext());
        compareResultList = new ArrayList<>();
        cdTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                //tvCount.setText((millisUntilFinished / 1000) + " s");
                tip.setText("请在" + (millisUntilFinished / 1000) + "秒内完成身份认证!");
            }

            @Override
            public void onFinish() {
                tip.setText("识别失败！");
                tip.setTextColor(Color.RED);
                if (callback != null) {
                    callback.onFail(stuId);
                }
            }
        };
        cdTimer.start();
        previewView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        startAnima(scanImage);

    }

    public void setRegistered(Boolean registered) {
        isRegistered = registered;
    }

    public void setCallback(OnArcFaceCallback callback, String stuId, String stuName) {
        this.callback = callback;
        this.stuId = stuId;
        binding.tvName.setText(stuName);
    }



    @Override
    public void onGlobalLayout() {
        previewView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        initEngine();
        initCamera();
    }

    public interface OnArcFaceCallback {

        void onSuccess(String id);

        void onFail(String id);

        void onDismiss();

    }

    private void initCamera() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindow().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        final FaceListener faceListener = new FaceListener() {
            @Override
            public void onFail(Exception e) {
                //XLog.d("onFail: " + e.getMessage());
            }

            //请求FR的回调
            @Override
            public void onFaceFeatureInfoGet(@Nullable final FaceFeature faceFeature, final Integer requestId, final Integer errorCode) {
                //FR成功
                if (faceFeature != null) {
//                    Log.i(TAG, "onPreview: fr end = " + System.currentTimeMillis() + " trackId = " + requestId);
                    Integer liveness = livenessMap.get(requestId);
                    //不做活体检测的情况，直接搜索
                    if (!livenessDetect) {
                        searchFace(faceFeature, requestId);
                    }
                    //活体检测通过，搜索特征
                    else if (liveness != null && liveness == LivenessInfo.ALIVE) {
                        searchFace(faceFeature, requestId);
                    }
                    //活体检测未出结果，或者非活体，延迟执行该函数
                    else {
                        if (requestFeatureStatusMap.containsKey(requestId)) {
                            Observable.timer(WAIT_LIVENESS_INTERVAL, TimeUnit.MILLISECONDS)
                                    .subscribe(new Observer<Long>() {
                                        Disposable disposable;

                                        @Override
                                        public void onSubscribe(Disposable d) {
                                            disposable = d;
                                            getFeatureDelayedDisposables.add(disposable);
                                        }

                                        @Override
                                        public void onNext(Long aLong) {
                                            onFaceFeatureInfoGet(faceFeature, requestId, errorCode);
                                        }

                                        @Override
                                        public void onError(Throwable e) {

                                        }

                                        @Override
                                        public void onComplete() {
                                            getFeatureDelayedDisposables.remove(disposable);
                                        }
                                    });
                        }
                    }

                }
                //特征提取失败
                else {
                    if (increaseAndGetValue(extractErrorRetryMap, requestId) > MAX_RETRY_TIME) {
                        extractErrorRetryMap.put(requestId, 0);
                        String msg;
                        // 传入的FaceInfo在指定的图像上无法解析人脸，此处使用的是RGB人脸数据，一般是人脸模糊
                        if (errorCode != null && errorCode == ErrorInfo.MERR_FSDK_FACEFEATURE_LOW_CONFIDENCE_LEVEL) {
                            msg = getContext().getString(R.string.low_confidence_level);
                        } else {
                            msg = "ExtractCode:" + errorCode;
                        }
                        //  faceHelper.setName(requestId, getContext().getString(R.string.recognize_failed_notice, msg));
                        // 在尝试最大次数后，特征提取仍然失败，则认为识别未通过
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                        retryRecognizeDelayed(requestId);
                    } else {
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.TO_RETRY);
                    }
                }
            }

            @Override
            public void onFaceLivenessInfoGet(@Nullable LivenessInfo livenessInfo, final Integer requestId, Integer errorCode) {
                if (livenessInfo != null) {
                    int liveness = livenessInfo.getLiveness();
                    livenessMap.put(requestId, liveness);
                    // 非活体，重试
                    if (liveness == LivenessInfo.NOT_ALIVE) {
                        //   faceHelper.setName(requestId, getContext().getString(R.string.recognize_failed_notice, "NOT_ALIVE"));
                        // 延迟 FAIL_RETRY_INTERVAL 后，将该人脸状态置为UNKNOWN，帧回调处理时会重新进行活体检测
                        retryLivenessDetectDelayed(requestId);
                    }
                } else {
                    if (increaseAndGetValue(livenessErrorRetryMap, requestId) > MAX_RETRY_TIME) {
                        livenessErrorRetryMap.put(requestId, 0);
                        String msg;
                        // 传入的FaceInfo在指定的图像上无法解析人脸，此处使用的是RGB人脸数据，一般是人脸模糊
                        if (errorCode != null && errorCode == ErrorInfo.MERR_FSDK_FACEFEATURE_LOW_CONFIDENCE_LEVEL) {
                            msg = getContext().getString(R.string.low_confidence_level);
                        } else {
                            msg = "ProcessCode:" + errorCode;
                        }
                        //  faceHelper.setName(requestId, getContext().getString(R.string.recognize_failed_notice, msg));
                        retryLivenessDetectDelayed(requestId);
                    } else {
                        livenessMap.put(requestId, LivenessInfo.UNKNOWN);
                    }
                }
            }
        };

        CameraListener cameraListener = new CameraListener() {
            @Override
            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {
                Camera.Size lastPreviewSize = previewSize;
                previewSize = camera.getParameters().getPreviewSize();
                drawHelper = new DrawHelper(previewSize.width, previewSize.height, previewView.getWidth(), previewView.getHeight(), displayOrientation
                        , cameraId, isMirror, false, false);
                //XLog.d("onCameraOpened: " + drawHelper.toString());
                // 切换相机的时候可能会导致预览尺寸发生变化
                if (faceHelper == null ||
                        lastPreviewSize == null ||
                        lastPreviewSize.width != previewSize.width || lastPreviewSize.height != previewSize.height) {
                    Integer trackedFaceCount = null;
                    // 记录切换时的人脸序号
                    if (faceHelper != null) {
                        trackedFaceCount = faceHelper.getTrackedFaceCount();
                        faceHelper.release();
                    }
                    faceHelper = new FaceHelper.Builder()
                            .ftEngine(ftEngine)
                            .frEngine(frEngine)
                            .flEngine(flEngine)
                            .frQueueSize(MAX_DETECT_NUM)
                            .flQueueSize(MAX_DETECT_NUM)
                            .previewSize(previewSize)
                            .faceListener(faceListener)
                            .trackedFaceCount(trackedFaceCount == null ? ConfigUtil.getTrackedFaceCount(getContext()) : trackedFaceCount)
                            .build();
                }
            }


            @Override
            public void onPreview(final byte[] nv21, Camera camera) {
                if (faceRectView != null) {
                    faceRectView.clearFaceInfo();
                }
                List<FacePreviewInfo> facePreviewInfoList = faceHelper.onPreviewFrame(nv21);
                if (facePreviewInfoList != null && faceRectView != null && drawHelper != null) {
                    drawPreviewInfo(facePreviewInfoList);//绘制人脸前框
                }
                if (!isRegistered) {
                    return;
                }
                registerFace(nv21, facePreviewInfoList);
                clearLeftFace(facePreviewInfoList);

                if (facePreviewInfoList != null && facePreviewInfoList.size() > 0 && previewSize != null) {
                    for (int i = 0; i < facePreviewInfoList.size(); i++) {
                        Integer status = requestFeatureStatusMap.get(facePreviewInfoList.get(i).getTrackId());
                        /**
                         * 在活体检测开启，在人脸识别状态不为成功或人脸活体状态不为处理中（ANALYZING）且不为处理完成（ALIVE、NOT_ALIVE）时重新进行活体检测
                         */
                        if (livenessDetect && (status == null || status != RequestFeatureStatus.SUCCEED)) {
                            Integer liveness = livenessMap.get(facePreviewInfoList.get(i).getTrackId());
                            if (liveness == null
                                    || (liveness != LivenessInfo.ALIVE && liveness != LivenessInfo.NOT_ALIVE && liveness != RequestLivenessStatus.ANALYZING)) {
                                livenessMap.put(facePreviewInfoList.get(i).getTrackId(), RequestLivenessStatus.ANALYZING);
                                faceHelper.requestFaceLiveness(nv21, facePreviewInfoList.get(i).getFaceInfo(), previewSize.width, previewSize.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList.get(i).getTrackId(), LivenessType.RGB);
                            }
                        }
                        /**
                         * 对于每个人脸，若状态为空或者为失败，则请求特征提取（可根据需要添加其他判断以限制特征提取次数），
                         * 特征提取回传的人脸特征结果在{@link FaceListener#onFaceFeatureInfoGet(FaceFeature, Integer, Integer)}中回传
                         */
                        if (status == null || status == RequestFeatureStatus.TO_RETRY) {
                            requestFeatureStatusMap.put(facePreviewInfoList.get(i).getTrackId(), RequestFeatureStatus.SEARCHING);
                            faceHelper.requestFaceFeature(nv21, facePreviewInfoList.get(i).getFaceInfo(), previewSize.width, previewSize.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList.get(i).getTrackId());
//                            Log.i(TAG, "onPreview: fr start = " + System.currentTimeMillis() + " trackId = " + facePreviewInfoList.get(i).getTrackedFaceCount());
                        }
                    }
                }
            }

            @Override
            public void onCameraClosed() {
                //XLog.d("onCameraClosed: ");
            }

            @Override
            public void onCameraError(Exception e) {
                //XLog.d("onCameraError: " + e.getMessage());
            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {
                if (drawHelper != null) {
                    drawHelper.setCameraDisplayOrientation(displayOrientation);
                }
                //XLog.d("onCameraConfigurationChanged: " + cameraID + "  " + displayOrientation);
            }
        };

        cameraHelper = new CameraHelper.Builder()
                .previewViewSize(new Point(previewView.getMeasuredWidth(), previewView.getMeasuredHeight()))
                .rotation(getWindow().getWindowManager().getDefaultDisplay().getRotation())
                .specificCameraId(rgbCameraID != null ? rgbCameraID : Camera.CameraInfo.CAMERA_FACING_FRONT)
                .isMirror(false)
                .previewOn(previewView)
                .cameraListener(cameraListener)
                .build();
        cameraHelper.init();
        cameraHelper.start();
    }

    private void searchFace(final FaceFeature frFace, final Integer requestId) {
        Observable.create(new ObservableOnSubscribe<CompareResult>() {
            @Override
            public void subscribe(ObservableEmitter<CompareResult> emitter) {
                CompareResult compareResult = FaceServer.getInstance().getTopOfFaceLib(frFace);
                emitter.onNext(compareResult);

            }
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<CompareResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(CompareResult compareResult) {

                        try {
                            if (compareResult == null || compareResult.getUserName() == null) {
                                requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                                //faceHelper.setName(requestId, "VISITOR " + requestId);
                                return;
                            }
                            if (compareResult.getSimilar() > SIMILAR_THRESHOLD) {
                                boolean isAdded = false;
                                if (compareResultList == null) {
                                    requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                                    //  faceHelper.setName(requestId, "VISITOR " + requestId);
                                    return;
                                }
                                for (CompareResult compareResult1 : compareResultList) {
                                    if (compareResult1.getTrackId() == requestId) {
                                        isAdded = true;
                                        break;
                                    }
                                }
                                if (!isAdded) {
                                    //对于多人脸搜索，假如最大显示数量为 MAX_DETECT_NUM 且有新的人脸进入，则以队列的形式移除
                                    if (compareResultList.size() >= MAX_DETECT_NUM) {
                                        compareResultList.remove(0);
                                        //adapter.notifyItemRemoved(0);
                                    }
                                    //添加显示人员时，保存其trackId
                                    compareResult.setTrackId(requestId);
                                    compareResultList.add(compareResult);
                                    //adapter.notifyItemInserted(compareResultList.size() - 1);
                                }
                                requestFeatureStatusMap.put(requestId, RequestFeatureStatus.SUCCEED);
                                //XLog.d("onFaceFeatureInfoGet--->" + "    trackId = " + requestId + "   " + getString(R.string.recognize_success_notice));
                                if (callback != null) {
                                    callback.onSuccess(stuId);
                                }
                                //  faceHelper.setName(requestId, getContext().getString(R.string.recognize_success_notice, compareResult.getUserName()));

                            } else {
                                //XLog.d("onFaceFeatureInfoGet--->" + "    trackId = " + requestId + "   " + getString(R.string.recognize_failed_notice));
                                // faceHelper.setName(requestId, getContext().getString(R.string.recognize_failed_notice, "NOT_REGISTERED"));
                                retryRecognizeDelayed(requestId);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public void onError(Throwable e) {
                        try {
                            //faceHelper.setName(requestId, getContext().getString(R.string.recognize_failed_notice, "NOT_REGISTERED"));
                            retryRecognizeDelayed(requestId);
                        } catch (Exception ex) {
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    /**
     * 删除已经离开的人脸
     *
     * @param facePreviewInfoList 人脸和trackId列表
     */
    private void clearLeftFace(List<FacePreviewInfo> facePreviewInfoList) {
        try {
            if (compareResultList != null) {
                for (int i = compareResultList.size() - 1; i >= 0; i--) {
                    if (!requestFeatureStatusMap.containsKey(compareResultList.get(i).getTrackId())) {
                        compareResultList.remove(i);
                        //adapter.notifyItemRemoved(i);
                    }
                }
            }
            if (facePreviewInfoList == null || facePreviewInfoList.size() == 0) {
                requestFeatureStatusMap.clear();
                livenessMap.clear();
                livenessErrorRetryMap.clear();
                extractErrorRetryMap.clear();
                if (getFeatureDelayedDisposables != null) {
                    getFeatureDelayedDisposables.clear();
                }
                return;
            }
            Enumeration<Integer> keys = requestFeatureStatusMap.keys();
            while (keys.hasMoreElements()) {
                int key = keys.nextElement();
                boolean contained = false;
                for (FacePreviewInfo facePreviewInfo : facePreviewInfoList) {
                    if (facePreviewInfo.getTrackId() == key) {
                        contained = true;
                        break;
                    }
                }
                if (!contained) {
                    requestFeatureStatusMap.remove(key);
                    livenessMap.remove(key);
                    livenessErrorRetryMap.remove(key);
                    extractErrorRetryMap.remove(key);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void registerFace(final byte[] nv21, final List<FacePreviewInfo> facePreviewInfoList) {

        if (registerStatus == REGISTER_STATUS_READY && facePreviewInfoList != null && facePreviewInfoList.size() > 0) {
            registerStatus = REGISTER_STATUS_PROCESSING;
            Observable.create(new ObservableOnSubscribe<Boolean>() {
                @Override
                public void subscribe(ObservableEmitter<Boolean> emitter) {


                    try {
                        boolean success = FaceServer.getInstance().registerNv21(getContext(), nv21.clone(), previewSize.width, previewSize.height,
                                facePreviewInfoList.get(0).getFaceInfo(), "registered " + faceHelper.getTrackedFaceCount());
                        emitter.onNext(success);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            })
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Boolean>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(Boolean success) {
                            try {
                                String result = success ? "register success!" : "register failed!";
                                showToast(result);
                                registerStatus = REGISTER_STATUS_DONE;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }

                        @Override
                        public void onError(Throwable e) {
                            try {
                                showToast("register failed!");
                                registerStatus = REGISTER_STATUS_DONE;
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }

    private void drawPreviewInfo(List<FacePreviewInfo> facePreviewInfoList) {
        List<DrawInfo> drawInfoList = new ArrayList<>();
        for (int i = 0; i < facePreviewInfoList.size(); i++) {
            String name = faceHelper.getName(facePreviewInfoList.get(i).getTrackId());
            Integer liveness = livenessMap.get(facePreviewInfoList.get(i).getTrackId());
            Integer recognizeStatus = requestFeatureStatusMap.get(facePreviewInfoList.get(i).getTrackId());

            // 根据识别结果和活体结果设置颜色
            int color = RecognizeColor.COLOR_UNKNOWN;
            if (recognizeStatus != null) {
                if (recognizeStatus == RequestFeatureStatus.FAILED) {
                    color = RecognizeColor.COLOR_FAILED;
                }
                if (recognizeStatus == RequestFeatureStatus.SUCCEED) {
                    color = RecognizeColor.COLOR_SUCCESS;
                }
            }
            if (liveness != null && liveness == LivenessInfo.NOT_ALIVE) {
                color = RecognizeColor.COLOR_FAILED;
            }

            drawInfoList.add(new DrawInfo(drawHelper.adjustRect(facePreviewInfoList.get(i).getFaceInfo().getRect()),
                    GenderInfo.UNKNOWN, AgeInfo.UNKNOWN_AGE, liveness == null ? LivenessInfo.UNKNOWN : liveness, color,
                    name == null ? String.valueOf(facePreviewInfoList.get(i).getTrackId()) : name));
        }
        drawHelper.draw(faceRectView, drawInfoList);
    }

    /**
     * 将map中key对应的value增1回传
     *
     * @param countMap map
     * @param key      key
     * @return 增1后的value
     */
    public int increaseAndGetValue(Map<Integer, Integer> countMap, int key) {
        if (countMap == null) {
            return 0;
        }
        Integer value = countMap.get(key);
        if (value == null) {
            value = 0;
        }
        countMap.put(key, ++value);
        return value;
    }

    /**
     * 延迟 FAIL_RETRY_INTERVAL 重新进行活体检测
     *
     * @param requestId 人脸ID
     */
    private void retryLivenessDetectDelayed(final Integer requestId) {
        Observable.timer(FAIL_RETRY_INTERVAL, TimeUnit.MILLISECONDS)
                .subscribe(new Observer<Long>() {
                    Disposable disposable;

                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                        delayFaceTaskCompositeDisposable.add(disposable);
                    }

                    @Override
                    public void onNext(Long aLong) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        // 将该人脸状态置为UNKNOWN，帧回调处理时会重新进行活体检测
                        if (livenessDetect) {
                            //  faceHelper.setName(requestId, Integer.toString(requestId));
                        }
                        livenessMap.put(requestId, LivenessInfo.UNKNOWN);
                        delayFaceTaskCompositeDisposable.remove(disposable);
                    }
                });
    }

    /**
     * 延迟 FAIL_RETRY_INTERVAL 重新进行人脸识别
     *
     * @param requestId 人脸ID
     */
    private void retryRecognizeDelayed(final Integer requestId) {
        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
        Observable.timer(FAIL_RETRY_INTERVAL, TimeUnit.MILLISECONDS)
                .subscribe(new Observer<Long>() {
                    Disposable disposable;

                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                        delayFaceTaskCompositeDisposable.add(disposable);
                    }

                    @Override
                    public void onNext(Long aLong) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        // 将该人脸特征提取状态置为FAILED，帧回调处理时会重新进行活体检测
                        // faceHelper.setName(requestId, Integer.toString(requestId));
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.TO_RETRY);
                        delayFaceTaskCompositeDisposable.remove(disposable);
                    }
                });
    }

    private static final int MAX_DETECT_NUM = 10;
    /**
     * 当FR成功，活体未成功时，FR等待活体的时间
     */
    private static final int WAIT_LIVENESS_INTERVAL = 100;
    /**
     * 失败重试间隔时间（ms）
     */
    private static final long FAIL_RETRY_INTERVAL = 1000;
    /**
     * 出错重试最大次数
     */
    private static final int MAX_RETRY_TIME = 3;

    private CameraHelper cameraHelper;
    private DrawHelper drawHelper;
    private Camera.Size previewSize;
    /**
     * 优先打开的摄像头，本界面主要用于单目RGB摄像头设备，因此默认打开前置
     */
    private Integer rgbCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;

    /**
     * VIDEO模式人脸检测引擎，用于预览帧人脸追踪
     */
    private FaceEngine ftEngine;
    /**
     * 用于特征提取的引擎
     */
    private FaceEngine frEngine;

    /**
     * IMAGE模式活体检测引擎，用于预览帧人脸活体检测
     */
    private FaceEngine flEngine;
    private int ftInitCode = -1;
    private int frInitCode = -1;
    private int flInitCode = -1;

    /**
     * 活体检测的开关
     */
    private boolean livenessDetect = true;
    /**
     * 注册人脸状态码，准备注册
     */
    private static final int REGISTER_STATUS_READY = 0;
    /**
     * 注册人脸状态码，注册中
     */
    private static final int REGISTER_STATUS_PROCESSING = 1;
    /**
     * 注册人脸状态码，注册结束（无论成功失败）
     */
    private static final int REGISTER_STATUS_DONE = 2;
    private int registerStatus = REGISTER_STATUS_DONE;
    /**
     * 用于记录人脸识别相关状态
     */
    private ConcurrentHashMap<Integer, Integer> requestFeatureStatusMap = new ConcurrentHashMap<>();
    /**
     * 用于记录人脸特征提取出错重试次数
     */
    private ConcurrentHashMap<Integer, Integer> extractErrorRetryMap = new ConcurrentHashMap<>();
    /**
     * 用于存储活体值
     */
    private ConcurrentHashMap<Integer, Integer> livenessMap = new ConcurrentHashMap<>();
    /**
     * 用于存储活体检测出错重试次数
     */
    private ConcurrentHashMap<Integer, Integer> livenessErrorRetryMap = new ConcurrentHashMap<>();
    private CompositeDisposable getFeatureDelayedDisposables = new CompositeDisposable();
    private CompositeDisposable delayFaceTaskCompositeDisposable = new CompositeDisposable();
    /**
     * 相机预览显示的控件，可为SurfaceView或TextureView
     */
    private View previewView;
    /**
     * 绘制人脸框的控件
     */
    private FaceRectView faceRectView;
    /**
     * 识别阈值
     */
    private static final float SIMILAR_THRESHOLD = 0.8F;

    /**
     * 初始化引擎
     */
    private void initEngine() {
        ftEngine = new FaceEngine();
        ftInitCode = ftEngine.init(getContext(), DetectMode.ASF_DETECT_MODE_VIDEO, DetectFaceOrientPriority.ASF_OP_ALL_OUT,
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_DETECT);

        frEngine = new FaceEngine();
        frInitCode = frEngine.init(getContext(), DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_RECOGNITION);

        flEngine = new FaceEngine();
        flInitCode = flEngine.init(getContext(), DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                16, MAX_DETECT_NUM, FaceEngine.ASF_LIVENESS);


        //  VersionInfo versionInfo = new VersionInfo();
        //ftEngine.getVersion(versionInfo);
        //XLog.d("initEngine:  init: " + ftInitCode + "  version:" + versionInfo);

        if (ftInitCode != ErrorInfo.MOK) {
            String error = getContext().getString(R.string.specific_engine_init_failed, "ftEngine", ftInitCode);
            //XLog.d("initEngine: " + error);
            showToast(error);
        }
        if (frInitCode != ErrorInfo.MOK) {
            String error = getContext().getString(R.string.specific_engine_init_failed, "frEngine", frInitCode);
            //XLog.d("initEngine: " + error);
            showToast(error);
        }
        if (flInitCode != ErrorInfo.MOK) {
            String error = getContext().getString(R.string.specific_engine_init_failed, "flEngine", flInitCode);
            //XLog.d("initEngine: " + error);
            showToast(error);
        }
    }

    /**
     * 销毁引擎，faceHelper中可能会有特征提取耗时操作仍在执行，加锁防止crash
     */
    private void unInitEngine() {
        if (ftInitCode == ErrorInfo.MOK && ftEngine != null) {
            synchronized (ftEngine) {
                int ftUnInitCode = ftEngine.unInit();
                //XLog.d("unInitEngine: " + ftUnInitCode);
            }
        }
        if (frInitCode == ErrorInfo.MOK && frEngine != null) {
            synchronized (frEngine) {
                int frUnInitCode = frEngine.unInit();
                //XLog.d("unInitEngine: " + frUnInitCode);
            }
        }
        if (flInitCode == ErrorInfo.MOK && flEngine != null) {
            synchronized (flEngine) {
                int flUnInitCode = flEngine.unInit();
                //XLog.d("unInitEngine: " + flUnInitCode);
            }
        }
    }

    public void showToast(final String str) {
        ToastUtils.showShort(str);
    }


    private void startAnima(View view) {
        TranslateAnimation ta1 = new TranslateAnimation(0, 0, DpPxUtils.dip2px(getContext(), 0), DpPxUtils.dip2px(getContext(), 223));
        ta1.setDuration(3000);
        //ta1.setStartTime(0);
        //ta1.setRepeatCount(Integer.MAX_VALUE);
        ta1.setRepeatMode(Animation.RESTART);
        ta1.setRepeatCount(-1);
        view.startAnimation(ta1);
    }

    private void stopAnima(View view) {
        view.clearAnimation();
    }


    @Override
    protected void onStart() {
        super.onStart();
        fullScreenImmersive(getWindow().getDecorView());
    }

    @Override
    public void dismiss() {
        fullScreenImmersive(getWindow().getDecorView());
        super.dismiss();
        stopAnima(scanImage);
        //EventBus.getDefault().unregister(this);
        if (cdTimer != null) {
            cdTimer.cancel();
            cdTimer = null;
        }

        if (cameraHelper != null) {
            cameraHelper.release();
            cameraHelper = null;
        }
        unInitEngine();
        if (faceHelper != null) {
            ConfigUtil.setTrackedFaceCount(getContext(), faceHelper.getTrackedFaceCount());
            faceHelper.release();
            faceHelper = null;
        }
        if (getFeatureDelayedDisposables != null) {
            getFeatureDelayedDisposables.clear();
        }
        if (delayFaceTaskCompositeDisposable != null) {
            delayFaceTaskCompositeDisposable.clear();
        }
        FaceServer.getInstance().clearAllFaces(getContext());
        FaceServer.getInstance().unInit();
        if (callback != null) {
            callback.onDismiss();
        }

    }

    private void fullScreenImmersive(View view) {
        view.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

}
