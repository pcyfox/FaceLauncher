package com.taike.module_arcface.ui.fragment

import android.graphics.Color
import android.graphics.Point
import android.hardware.Camera
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import androidx.fragment.app.Fragment
import com.arcsoft.face.*
import com.arcsoft.face.enums.DetectFaceOrientPriority
import com.arcsoft.face.enums.DetectMode
import com.blankj.utilcode.util.ToastUtils
import com.taike.lib_utils.DpPxUtils
import com.taike.module_arcface.arcface.CompareResult
import com.taike.module_arcface.arcface.FacePreviewInfo
import com.taike.module_arcface.arcface.FaceServer
import com.taike.module_arcface.arcface.camera.CameraHelper
import com.taike.module_arcface.arcface.camera.CameraListener
import com.taike.module_arcface.arcface.draw.DrawHelper
import com.taike.module_arcface.arcface.draw.DrawInfo
import com.taike.module_arcface.arcface.draw.FaceRectView
import com.taike.module_arcface.arcface.face.*
import com.taike.module_test.R
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.dialog_arcface.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class ArcFaceFragment : Fragment() {
    private var onFinish = false
    private var cameraHelper: CameraHelper? = null
    private var drawHelper: DrawHelper? = null
    private var previewSize: Camera.Size? = null

    /**
     * 优先打开的摄像头，本界面主要用于单目RGB摄像头设备，因此默认打开前置
     */
    private val rgbCameraID: Int? = Camera.CameraInfo.CAMERA_FACING_FRONT

    /**
     * VIDEO模式人脸检测引擎，用于预览帧人脸追踪
     */
    private var ftEngine: FaceEngine? = null

    /**
     * 用于特征提取的引擎
     */
    private var frEngine: FaceEngine? = null

    /**
     * IMAGE模式活体检测引擎，用于预览帧人脸活体检测
     */
    private var flEngine: FaceEngine? = null
    private var ftInitCode = -1
    private var frInitCode = -1
    private var flInitCode = -1

    /**
     * 活体检测的开关
     */
    private val livenessDetect = true
    private var registerStatus = REGISTER_STATUS_DONE

    /**
     * 用于记录人脸识别相关状态
     */
    private val requestFeatureStatusMap = ConcurrentHashMap<Int, Int>()

    /**
     * 用于记录人脸特征提取出错重试次数
     */
    private val extractErrorRetryMap = ConcurrentHashMap<Int?, Int?>()

    /**
     * 用于存储活体值
     */
    private val livenessMap = ConcurrentHashMap<Int, Int>()

    /**
     * 用于存储活体检测出错重试次数
     */
    private val livenessErrorRetryMap = ConcurrentHashMap<Int?, Int?>()
    private val getFeatureDelayedDisposables: CompositeDisposable? = CompositeDisposable()
    private val delayFaceTaskCompositeDisposable: CompositeDisposable? = CompositeDisposable()


    /**
     * 绘制人脸框的控件
     */
    private var faceRectView: FaceRectView? = null
    private var callback: OnArcFaceCallback? = null
    private var faceHelper: FaceHelper? = null
    private var compareResultList: MutableList<CompareResult>? = null
    private var cdTimer: CountDownTimer? = null

    private var stuId: String? = null
    private var isRegistered = false //需要识别的人脸是否完成注册


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_arcface, null)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initView()
        initEngine()
        start()
    }

    private fun initView() {
        faceRectView = single_camera_face_rect_view
        tv_tip?.setOnClickListener {
            callback?.onTipViewClick(tv_tip.text.toString())
            if (cameraHelper != null && cameraHelper!!.isStopped || onFinish) {
                start()
            }
        }
    }

    fun start() {
        requestFeatureStatusMap.clear()
        extractErrorRetryMap.clear()
        livenessMap.clear()
        livenessErrorRetryMap?.clear()
        tv_tip!!.setTextColor(Color.parseColor("#1A94FD"))
        compareResultList = ArrayList()
        stop()
        initCamera()
        cdTimer = object : CountDownTimer(30 * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                //tvCount.setText((millisUntilFinished / 1000) + " s");
                onFinish = false
                tv_tip.text = "请在" + millisUntilFinished / 1000 + "秒内完成身份认证!"
            }

            override fun onFinish() {
                onFinish = true
                tv_tip!!.text = "识别失败！"
                tv_tip!!.setTextColor(Color.RED)
                if (callback != null) {
                    callback!!.onFail(stuId)
                }
            }
        }
        cdTimer?.start()
        startAnimation(iv_scanImage!!)
    }

    fun stop(text: String = "", textColor: Int? = null) {
        stopAnimation(iv_scanImage)
        if (cdTimer != null) {
            cdTimer!!.cancel()
            cdTimer = null
        }
        cameraHelper?.stop()
        textColor?.run {
            tv_tip.setTextColor(textColor)
        }
        tv_tip!!.text = text
    }

    fun setRegistered(registered: Boolean) {
        isRegistered = registered
    }

    fun setCallback(callback: OnArcFaceCallback?, stuId: String?, stuName: String?) {
        this.callback = callback
        this.stuId = stuId
    }


    interface OnArcFaceCallback {
        fun onSuccess(id: String?)
        fun onFail(id: String?)
        fun onTipViewClick(text: String)
    }

    private fun initCamera() {
        val faceListener = object : FaceListener {
            override fun onFail(e: Exception) {
                //XLog.d("onFail: " + e.getMessage());
            }

            //请求FR的回调
            override fun onFaceFeatureInfoGet(faceFeature: FaceFeature?, requestId: Int, errorCode: Int) {
                //FR成功
                if (faceFeature != null) {
//                    Log.i(TAG, "onPreview: fr end = " + System.currentTimeMillis() + " trackId = " + requestId);
                    val liveness = livenessMap[requestId]
                    //不做活体检测的情况，直接搜索
                    if (liveness != null && liveness == LivenessInfo.ALIVE) {
                        searchFace(faceFeature, requestId)
                    } else {
                        if (requestFeatureStatusMap.containsKey(requestId)) {
                            Observable.timer(WAIT_LIVENESS_INTERVAL.toLong(), TimeUnit.MILLISECONDS)
                                    .subscribe(object : Observer<Long?> {
                                        var disposable: Disposable? = null
                                        override fun onSubscribe(d: Disposable) {
                                            disposable = d
                                            getFeatureDelayedDisposables!!.add(disposable!!)
                                        }

                                        override fun onNext(aLong: Long) {
                                            onFaceFeatureInfoGet(faceFeature, requestId, errorCode)
                                        }

                                        override fun onError(e: Throwable) {}
                                        override fun onComplete() {
                                            getFeatureDelayedDisposables!!.remove(disposable!!)
                                        }
                                    })
                        }
                    }
                } else {
                    if (increaseAndGetValue(extractErrorRetryMap, requestId) > MAX_RETRY_TIME) {
                        extractErrorRetryMap[requestId] = 0
                        // 传入的FaceInfo在指定的图像上无法解析人脸，此处使用的是RGB人脸数据，一般是人脸模糊
                        val msg: String = if (errorCode == ErrorInfo.MERR_FSDK_FACEFEATURE_LOW_CONFIDENCE_LEVEL) {
                            context!!.getString(R.string.low_confidence_level)
                        } else {
                            "ExtractCode:$errorCode"
                        }
                        //  faceHelper.setName(requestId, getContext().getString(R.string.recognize_failed_notice, msg));
                        // 在尝试最大次数后，特征提取仍然失败，则认为识别未通过
                        requestFeatureStatusMap[requestId] = RequestFeatureStatus.FAILED
                        retryRecognizeDelayed(requestId)
                    } else {
                        requestFeatureStatusMap[requestId] = RequestFeatureStatus.TO_RETRY
                    }
                }
            }

            override fun onFaceLivenessInfoGet(livenessInfo: LivenessInfo?, requestId: Int, errorCode: Int) {
                if (livenessInfo != null) {
                    val liveness = livenessInfo.liveness
                    livenessMap[requestId] = liveness
                    // 非活体，重试
                    if (liveness == LivenessInfo.NOT_ALIVE) {
                        //   faceHelper.setName(requestId, getContext().getString(R.string.recognize_failed_notice, "NOT_ALIVE"));
                        // 延迟 FAIL_RETRY_INTERVAL 后，将该人脸状态置为UNKNOWN，帧回调处理时会重新进行活体检测
                        retryLivenessDetectDelayed(requestId)
                    }
                } else {
                    if (increaseAndGetValue(livenessErrorRetryMap, requestId) > MAX_RETRY_TIME) {
                        livenessErrorRetryMap[requestId] = 0
                        // 传入的FaceInfo在指定的图像上无法解析人脸，此处使用的是RGB人脸数据，一般是人脸模糊
                        val msg: String = if (errorCode == ErrorInfo.MERR_FSDK_FACEFEATURE_LOW_CONFIDENCE_LEVEL) {
                            context!!.getString(R.string.low_confidence_level)
                        } else {
                            "ProcessCode:$errorCode"
                        }
                        //  faceHelper.setName(requestId, getContext().getString(R.string.recognize_failed_notice, msg));
                        retryLivenessDetectDelayed(requestId)
                    } else {
                        livenessMap[requestId] = LivenessInfo.UNKNOWN
                    }
                }
            }
        }
        val cameraListener = object : CameraListener {
            override fun onCameraOpened(camera: Camera, cameraId: Int, displayOrientation: Int, isMirror: Boolean) {
                val lastPreviewSize = previewSize
                previewSize = camera.parameters.previewSize
                if (previewSize == null) {
                    return
                }
                drawHelper = DrawHelper(previewSize!!.width, previewSize!!.height, single_camera_texture_preview!!.width, single_camera_texture_preview!!.height, displayOrientation
                        , cameraId, isMirror, false, false)
                //XLog.d("onCameraOpened: " + drawHelper.toString());
                // 切换相机的时候可能会导致预览尺寸发生变化
                if (faceHelper == null || lastPreviewSize == null || lastPreviewSize.width != previewSize?.width || lastPreviewSize.height != previewSize?.height) {
                    var trackedFaceCount: Int? = null
                    // 记录切换时的人脸序号
                    if (faceHelper != null) {
                        trackedFaceCount = faceHelper!!.trackedFaceCount
                        faceHelper!!.release()
                    }
                    faceHelper = FaceHelper.Builder()
                            .ftEngine(ftEngine)
                            .frEngine(frEngine)
                            .flEngine(flEngine)
                            .frQueueSize(MAX_DETECT_NUM)
                            .flQueueSize(MAX_DETECT_NUM)
                            .previewSize(previewSize)
                            .faceListener(faceListener)
                            .trackedFaceCount(trackedFaceCount
                                    ?: ConfigUtil.getTrackedFaceCount(context))
                            .build()
                }
            }

            override fun onPreview(nv21: ByteArray, camera: Camera) {
                val facePreviewInfoList = faceHelper?.onPreviewFrame(nv21)
                if (facePreviewInfoList != null && faceRectView != null && drawHelper != null) {
                    drawPreviewInfo(facePreviewInfoList) //绘制人脸前框
                }
                if (!isRegistered) {
                    return
                }
                registerFace(nv21, facePreviewInfoList)
                clearLeftFace(facePreviewInfoList)
                if (facePreviewInfoList != null && facePreviewInfoList.size > 0 && previewSize != null) {
                    for (i in facePreviewInfoList.indices) {
                        val status = requestFeatureStatusMap[facePreviewInfoList[i].trackId]
                        /**
                         * 在活体检测开启，在人脸识别状态不为成功或人脸活体状态不为处理中（ANALYZING）且不为处理完成（ALIVE、NOT_ALIVE）时重新进行活体检测
                         */
                        if (livenessDetect && (status == null || status != RequestFeatureStatus.SUCCEED)) {
                            val liveness = livenessMap[facePreviewInfoList[i].trackId]
                            if (liveness == null
                                    || liveness != LivenessInfo.ALIVE && liveness != LivenessInfo.NOT_ALIVE && liveness != RequestLivenessStatus.ANALYZING) {
                                livenessMap[facePreviewInfoList[i].trackId] = RequestLivenessStatus.ANALYZING
                                faceHelper!!.requestFaceLiveness(nv21, facePreviewInfoList[i].faceInfo, previewSize!!.width, previewSize!!.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList[i].trackId, LivenessType.RGB)
                            }
                        }
                        /**
                         * 对于每个人脸，若状态为空或者为失败，则请求特征提取（可根据需要添加其他判断以限制特征提取次数），
                         * 特征提取回传的人脸特征结果在[FaceListener.onFaceFeatureInfoGet]中回传
                         */
                        if (status == null || status == RequestFeatureStatus.TO_RETRY) {
                            requestFeatureStatusMap[facePreviewInfoList[i].trackId] = RequestFeatureStatus.SEARCHING
                            faceHelper!!.requestFaceFeature(nv21, facePreviewInfoList[i].faceInfo, previewSize!!.width, previewSize!!.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList[i].trackId)
                            //                            Log.i(TAG, "onPreview: fr start = " + System.currentTimeMillis() + " trackId = " + facePreviewInfoList.get(i).getTrackedFaceCount());
                        }
                    }
                }
            }

            override fun onCameraClosed() {
                //XLog.d("onCameraClosed: ");
            }

            override fun onCameraError(e: Exception) {
                //XLog.d("onCameraError: " + e.getMessage());
            }

            override fun onCameraConfigurationChanged(cameraID: Int, displayOrientation: Int) {
                drawHelper?.cameraDisplayOrientation = displayOrientation
                //XLog.d("onCameraConfigurationChanged: " + cameraID + "  " + displayOrientation);
            }
        }
        cameraHelper = CameraHelper.Builder()
                .previewViewSize(Point(single_camera_texture_preview!!.measuredWidth, single_camera_texture_preview!!.measuredHeight))
                .rotation(activity!!.window.windowManager.defaultDisplay.rotation)
                .specificCameraId(rgbCameraID ?: Camera.CameraInfo.CAMERA_FACING_FRONT)
                .isMirror(false)
                .previewOn(single_camera_texture_preview)
                .cameraListener(cameraListener)
                .build()
        cameraHelper?.init()
        cameraHelper?.start()
    }

    private fun searchFace(frFace: FaceFeature, requestId: Int) {
        Observable.create<CompareResult> { emitter ->
            val compareResult = FaceServer.getInstance().getTopOfFaceLib(frFace)
            emitter.onNext(compareResult)
        }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<CompareResult?> {
                    override fun onSubscribe(d: Disposable) {}
                    override fun onNext(compareResult: CompareResult) {
                        try {
                            if (compareResult?.userName == null) {
                                requestFeatureStatusMap[requestId] = RequestFeatureStatus.FAILED
                                //faceHelper.setName(requestId, "VISITOR " + requestId);
                                return
                            }
                            if (compareResult.similar > SIMILAR_THRESHOLD) {
                                var isAdded = false
                                if (compareResultList == null) {
                                    requestFeatureStatusMap[requestId] = RequestFeatureStatus.FAILED
                                    //  faceHelper.setName(requestId, "VISITOR " + requestId);
                                    return
                                }
                                for (compareResult1 in compareResultList!!) {
                                    if (compareResult1.trackId == requestId) {
                                        isAdded = true
                                        break
                                    }
                                }
                                if (!isAdded) {
                                    //对于多人脸搜索，假如最大显示数量为 MAX_DETECT_NUM 且有新的人脸进入，则以队列的形式移除
                                    if (compareResultList!!.size >= MAX_DETECT_NUM) {
                                        compareResultList!!.removeAt(0)
                                        //adapter.notifyItemRemoved(0);
                                    }
                                    //添加显示人员时，保存其trackId
                                    compareResult.trackId = requestId
                                    compareResultList!!.add(compareResult)
                                    //adapter.notifyItemInserted(compareResultList.size() - 1);
                                }
                                requestFeatureStatusMap[requestId] = RequestFeatureStatus.SUCCEED
                                //XLog.d("onFaceFeatureInfoGet--->" + "    trackId = " + requestId + "   " + getString(R.string.recognize_success_notice));
                                if (callback != null) {
                                    callback!!.onSuccess(stuId)
                                }
                                //  faceHelper.setName(requestId, getContext().getString(R.string.recognize_success_notice, compareResult.getUserName()));
                            } else {
                                //XLog.d("onFaceFeatureInfoGet--->" + "    trackId = " + requestId + "   " + getString(R.string.recognize_failed_notice));
                                // faceHelper.setName(requestId, getContext().getString(R.string.recognize_failed_notice, "NOT_REGISTERED"));
                                retryRecognizeDelayed(requestId)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun onError(e: Throwable) {
                        try {
                            //faceHelper.setName(requestId, getContext().getString(R.string.recognize_failed_notice, "NOT_REGISTERED"));
                            retryRecognizeDelayed(requestId)
                        } catch (ex: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun onComplete() {}
                })
    }

    /**
     * 删除已经离开的人脸
     *
     * @param facePreviewInfoList 人脸和trackId列表
     */
    private fun clearLeftFace(facePreviewInfoList: List<FacePreviewInfo>?) {
        try {
            if (compareResultList != null) {
                for (i in compareResultList!!.indices.reversed()) {
                    if (!requestFeatureStatusMap.containsKey(compareResultList!![i].trackId)) {
                        compareResultList!!.removeAt(i)
                        //adapter.notifyItemRemoved(i);
                    }
                }
            }
            if (facePreviewInfoList == null || facePreviewInfoList.isEmpty()) {
                requestFeatureStatusMap.clear()
                livenessMap.clear()
                livenessErrorRetryMap.clear()
                extractErrorRetryMap.clear()
                getFeatureDelayedDisposables?.clear()
                return
            }
            val keys = requestFeatureStatusMap.keys()
            while (keys.hasMoreElements()) {
                val key = keys.nextElement()
                var contained = false
                for (facePreviewInfo in facePreviewInfoList) {
                    if (facePreviewInfo.trackId == key) {
                        contained = true
                        break
                    }
                }
                if (!contained) {
                    requestFeatureStatusMap.remove(key)
                    livenessMap.remove(key)
                    livenessErrorRetryMap.remove(key)
                    extractErrorRetryMap.remove(key)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun registerFace(nv21: ByteArray, facePreviewInfoList: List<FacePreviewInfo>?) {
        if (registerStatus == REGISTER_STATUS_READY && facePreviewInfoList != null && facePreviewInfoList.isNotEmpty()) {
            registerStatus = REGISTER_STATUS_PROCESSING
            Observable.create<Boolean> { emitter ->
                try {
                    val success = FaceServer.getInstance().registerNv21(context, nv21.clone(), previewSize!!.width, previewSize!!.height,
                            facePreviewInfoList[0].faceInfo, "registered " + faceHelper!!.trackedFaceCount)
                    emitter.onNext(success)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : Observer<Boolean> {
                        override fun onSubscribe(d: Disposable) {}
                        override fun onNext(success: Boolean) {
                            try {
                                val result = if (success) "register success!" else "register failed!"
                                showToast(result)
                                registerStatus = REGISTER_STATUS_DONE
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onError(e: Throwable) {
                            try {
                                showToast("register failed!")
                                registerStatus = REGISTER_STATUS_DONE
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }

                        override fun onComplete() {}
                    })
        }
    }

    private fun drawPreviewInfo(facePreviewInfoList: List<FacePreviewInfo>) {
        val drawInfoList: MutableList<DrawInfo> = ArrayList()
        for (i in facePreviewInfoList.indices) {
            val name = faceHelper!!.getName(facePreviewInfoList[i].trackId)
            val liveness = livenessMap[facePreviewInfoList[i].trackId]
            val recognizeStatus = requestFeatureStatusMap[facePreviewInfoList[i].trackId]

            // 根据识别结果和活体结果设置颜色
            var color = RecognizeColor.COLOR_UNKNOWN
            if (recognizeStatus != null) {
                if (recognizeStatus == RequestFeatureStatus.FAILED) {
                    color = RecognizeColor.COLOR_FAILED
                }
                if (recognizeStatus == RequestFeatureStatus.SUCCEED) {
                    color = RecognizeColor.COLOR_SUCCESS
                }
            }
            if (liveness != null && liveness == LivenessInfo.NOT_ALIVE) {
                color = RecognizeColor.COLOR_FAILED
            }
            drawInfoList.add(DrawInfo(drawHelper!!.adjustRect(facePreviewInfoList[i].faceInfo.rect),
                    GenderInfo.UNKNOWN, AgeInfo.UNKNOWN_AGE, liveness
                    ?: LivenessInfo.UNKNOWN, color,
                    name ?: facePreviewInfoList[i].trackId.toString()))
        }
        drawHelper?.draw(faceRectView, drawInfoList)
    }

    /**
     * 将map中key对应的value增1回传
     *
     * @param countMap map
     * @param key      key
     * @return 增1后的value
     */
    fun increaseAndGetValue(countMap: MutableMap<Int?, Int?>?, key: Int): Int {
        if (countMap == null) {
            return 0
        }
        var value = countMap[key]
        if (value == null) {
            value = 0
        }
        countMap[key] = ++value
        return value
    }

    /**
     * 延迟 FAIL_RETRY_INTERVAL 重新进行活体检测
     *
     * @param requestId 人脸ID
     */
    private fun retryLivenessDetectDelayed(requestId: Int) {
        Observable.timer(FAIL_RETRY_INTERVAL, TimeUnit.MILLISECONDS)
                .subscribe(object : Observer<Long?> {
                    var disposable: Disposable? = null
                    override fun onSubscribe(d: Disposable) {
                        disposable = d
                        delayFaceTaskCompositeDisposable!!.add(disposable!!)
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                    }

                    override fun onComplete() {
                        // 将该人脸状态置为UNKNOWN，帧回调处理时会重新进行活体检测
                        if (livenessDetect) {
                            //  faceHelper.setName(requestId, Integer.toString(requestId));
                        }
                        livenessMap[requestId] = LivenessInfo.UNKNOWN
                        delayFaceTaskCompositeDisposable!!.remove(disposable!!)
                    }

                    override fun onNext(t: Long) {
                    }
                })
    }

    /**
     * 延迟 FAIL_RETRY_INTERVAL 重新进行人脸识别
     *
     * @param requestId 人脸ID
     */
    private fun retryRecognizeDelayed(requestId: Int) {
        requestFeatureStatusMap[requestId] = RequestFeatureStatus.FAILED
        Observable.timer(FAIL_RETRY_INTERVAL, TimeUnit.MILLISECONDS)
                .subscribe(object : Observer<Long?> {
                    var disposable: Disposable? = null
                    override fun onSubscribe(d: Disposable) {
                        disposable = d
                        delayFaceTaskCompositeDisposable!!.add(disposable!!)
                    }

                    override fun onNext(aLong: Long) {}
                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                    }

                    override fun onComplete() {
                        // 将该人脸特征提取状态置为FAILED，帧回调处理时会重新进行活体检测
                        // faceHelper.setName(requestId, Integer.toString(requestId));
                        requestFeatureStatusMap[requestId] = RequestFeatureStatus.TO_RETRY
                        delayFaceTaskCompositeDisposable!!.remove(disposable!!)
                    }
                })
    }


    /**
     * 初始化引擎
     */
    private fun initEngine() {
        FaceServer.getInstance().init(context)
        ftEngine = FaceEngine()
        ftInitCode = ftEngine!!.init(context, DetectMode.ASF_DETECT_MODE_VIDEO, DetectFaceOrientPriority.ASF_OP_ALL_OUT,
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_DETECT)
        frEngine = FaceEngine()
        frInitCode = frEngine!!.init(context, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_RECOGNITION)
        flEngine = FaceEngine()
        flInitCode = flEngine!!.init(context, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                16, MAX_DETECT_NUM, FaceEngine.ASF_LIVENESS)


        //  VersionInfo versionInfo = new VersionInfo();
        //ftEngine.getVersion(versionInfo);
        //XLog.d("initEngine:  init: " + ftInitCode + "  version:" + versionInfo);
        if (ftInitCode != ErrorInfo.MOK) {
            val error = context!!.getString(R.string.specific_engine_init_failed, "ftEngine", ftInitCode)
            //XLog.d("initEngine: " + error);
            showToast(error)
        }
        if (frInitCode != ErrorInfo.MOK) {
            val error = context!!.getString(R.string.specific_engine_init_failed, "frEngine", frInitCode)
            //XLog.d("initEngine: " + error);
            showToast(error)
        }
        if (flInitCode != ErrorInfo.MOK) {
            val error = context!!.getString(R.string.specific_engine_init_failed, "flEngine", flInitCode)
            //XLog.d("initEngine: " + error);
            showToast(error)
        }
    }

    /**
     * 销毁引擎，faceHelper中可能会有特征提取耗时操作仍在执行，加锁防止crash
     */
    private fun unInitEngine() {
        if (ftInitCode == ErrorInfo.MOK && ftEngine != null) {
            synchronized(ftEngine!!) { val ftUnInitCode = ftEngine!!.unInit() }
        }
        if (frInitCode == ErrorInfo.MOK && frEngine != null) {
            synchronized(frEngine!!) { val frUnInitCode = frEngine!!.unInit() }
        }
        if (flInitCode == ErrorInfo.MOK && flEngine != null) {
            synchronized(flEngine!!) { val flUnInitCode = flEngine!!.unInit() }
        }
    }

    fun showToast(str: String?) {
        ToastUtils.showShort(str)
    }

    private fun startAnimation(view: View) {
        val ta1 = TranslateAnimation(0f, 0f, DpPxUtils.dip2px(context, 0f).toFloat(), DpPxUtils.dip2px(context, 223f).toFloat())
        ta1.duration = 3000
        //ta1.setStartTime(0);
        //ta1.setRepeatCount(Integer.MAX_VALUE);
        ta1.repeatMode = Animation.RESTART
        ta1.repeatCount = -1
        view.startAnimation(ta1)
    }

    private fun stopAnimation(view: View?) {
        view!!.clearAnimation()
    }


    override fun onDestroy() {
        super.onDestroy()
        stopAnimation(iv_scanImage)
        //EventBus.getDefault().unregister(this);
        if (cdTimer != null) {
            cdTimer!!.cancel()
            cdTimer = null
        }
        if (cameraHelper != null) {
            cameraHelper!!.release()
            cameraHelper = null
        }
        unInitEngine()
        if (faceHelper != null) {
            ConfigUtil.setTrackedFaceCount(context, faceHelper!!.trackedFaceCount)
            faceHelper!!.release()
            faceHelper = null
        }
        getFeatureDelayedDisposables?.clear()
        delayFaceTaskCompositeDisposable?.clear()
        FaceServer.getInstance().clearAllFaces(context)
        FaceServer.getInstance().unInit()

    }


    companion object {
        private const val MAX_DETECT_NUM = 10

        /**
         * 当FR成功，活体未成功时，FR等待活体的时间
         */
        private const val WAIT_LIVENESS_INTERVAL = 100

        /**
         * 失败重试间隔时间（ms）
         */
        private const val FAIL_RETRY_INTERVAL: Long = 1000

        /**
         * 出错重试最大次数
         */
        private const val MAX_RETRY_TIME = 3

        /**
         * 注册人脸状态码，准备注册
         */
        private const val REGISTER_STATUS_READY = 0

        /**
         * 注册人脸状态码，注册中
         */
        private const val REGISTER_STATUS_PROCESSING = 1

        /**
         * 注册人脸状态码，注册结束（无论成功失败）
         */
        private const val REGISTER_STATUS_DONE = 2

        /**
         * 识别阈值
         */
        private const val SIMILAR_THRESHOLD = 0.8f
    }
}