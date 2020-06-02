package com.taike.module_arcface.ui.activity;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import com.arcsoft.face.ActiveFileInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.enums.RuntimeABI;
import com.elvishew.xlog.XLog;
import com.taike.module_arcface.arcface.Constants;
import com.taike.module_arcface.arcface.ImportFaceUtils;
import com.taike.module_test.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ArcFaceUtils {
    static boolean libraryExists = true;
    // Demo 所需的动态库文件
    private static final String[] LIBRARIES = new String[]{
            // 人脸相关
            "libarcsoft_face_engine.so",
            "libarcsoft_face.so",
            // 图像库相关
            "libarcsoft_image_util.so",
    };

    /**
     * 检查能否找到动态链接库，如果找不到，请修改工程配置
     *
     * @param libraries 需要的动态链接库
     * @return 动态库是否存在
     */
    private static boolean checkSoFile(String[] libraries, Context context) {
        File dir = new File(context.getApplicationInfo().nativeLibraryDir);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return false;
        }
        List<String> libraryNameList = new ArrayList<>();
        for (File file : files) {
            libraryNameList.add(file.getName());
        }
        boolean exists = true;
        for (String library : libraries) {
            exists &= libraryNameList.contains(library);
        }
        return exists;
    }


    public static void initFace(Context context, ArcfaceCallback callback) {
        libraryExists = checkSoFile(LIBRARIES, context);
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        if (!libraryExists) {
            //showToast(getString(R.string.library_not_found));
        } else {
            activeEngine(context, callback);
        }
        ImportFaceUtils.getInstance().init(context);
    }


    /**
     * 激活引擎
     */
    public static void activeEngine(final Context context, final ArcfaceCallback callback) {
        if (!libraryExists) {
            //showToast(getString(R.string.library_not_found));
            return;
        }
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) {
                RuntimeABI runtimeABI = FaceEngine.getRuntimeABI();
                XLog.d("subscribe: getRuntimeABI() " + runtimeABI);
                int activeCode = FaceEngine.activeOnline(context, Constants.APP_ID, Constants.SDK_KEY);
                emitter.onNext(activeCode);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Integer activeCode) {
                        if (activeCode == ErrorInfo.MOK) {
                            // showToast(getString(R.string.active_success));
                            if (callback != null) {
                                callback.onSuccess();
                            }

                        } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
                            //showToast(getString(R.string.already_activated));
                            if (callback != null) {
                                callback.onSuccess();
                            }
                        } else {
                            //showToast(getString(R.string.active_failed, activeCode));
                            if (callback != null) {
                                callback.onError(context.getString(R.string.active_failed, activeCode));
                            }
                        }
                        ActiveFileInfo activeFileInfo = new ActiveFileInfo();
                        int res = FaceEngine.getActiveFileInfo(context, activeFileInfo);
                        if (res == ErrorInfo.MOK) {
                            XLog.d(activeFileInfo.toString());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        //showToast(e.getMessage());
                        //XLog.d("激活失败" + e.getMessage());
                        if (callback != null) {
                            callback.onError("激活失败，请检查网络");
                        }
                    }

                    @Override
                    public void onComplete() {


                    }
                });
    }

    public interface ArcfaceCallback {
        void onSuccess();

        void onError(String errorMsg);
    }
}
