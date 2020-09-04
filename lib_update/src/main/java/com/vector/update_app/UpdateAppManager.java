package com.vector.update_app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.vector.update_app.interf.ApkDownLoadManager;
import com.vector.update_app.interf.DownloadCallback;
import com.vector.update_app.interf.HttpManager;
import com.vector.update_app.interf.UpdateCallback;
import com.vector.update_app.listener.ExceptionHandler;
import com.vector.update_app.listener.ExceptionHandlerHelper;
import com.vector.update_app.listener.IUpdateDialogFragmentListener;
import com.vector.update_app.service.DownloadService;
import com.vector.update_app.utils.AppUpdateUtils;


import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 版本更新管理器
 */
public class UpdateAppManager {
    final static String INTENT_KEY = "update_dialog_values";
    final static String THEME_KEY = "theme_color";
    final static String IS_CHECK_UPDATE_BY_USER = "IS_CHECK_UPDATE_BY_USER";
    private final static String UPDATE_APP_KEY = "UPDATE_APP_KEY";
    private static final String TAG = UpdateAppManager.class.getSimpleName();
    private UpdateAppBean mUpdateApp;
    private final BuidlerParam buidlerParam;


    //添加默认参数
    private static Map<String, String> params = new HashMap<>();

    private UpdateAppManager(Builder builder) {
        buidlerParam = builder.param;
    }

    public Context getContext() {
        return buidlerParam.mActivity;
    }

    /**
     * @return 新版本信息
     */
    public UpdateAppBean fillUpdateAppData() {
        if (mUpdateApp != null) {
            mUpdateApp.setHideDialog(buidlerParam.mHideDialog);
            mUpdateApp.setOnlyWifi(buidlerParam.mOnlyWifi);
            return mUpdateApp;
        }
        return null;
    }


    private boolean isOk() {
        if (mUpdateApp == null) return false;
        if (TextUtils.isEmpty(buidlerParam.storePath) || !new File(buidlerParam.storePath).isDirectory()) {
            Log.e(TAG, "下载路径错误:" + buidlerParam.storePath);
            return false;
        }
        //没有强制升级时检查是否已经忽略版本及提示次数已用完
        if (!mUpdateApp.isConstraint()) {
            //用户手动触发升级
            if (buidlerParam.isCheckUpdateByUser) return true;
            return !mUpdateApp.isIgnored() && (mUpdateApp.getTipCount() == -1 || AppUpdateUtils.getTipCount(buidlerParam.mActivity, mUpdateApp.getNewVersion()) < mUpdateApp.getTipCount());
        }
        return true;
    }

    /**
     * 显示更新页面
     *
     * @return 是否弹窗
     */
    public boolean showDialogFragment() {
        //校验
        if (!isOk()) return false;
        if (buidlerParam.mActivity != null && !buidlerParam.mActivity.isFinishing()) {
            Bundle bundle = new Bundle();
            //添加信息，
            fillUpdateAppData();
            bundle.putSerializable(INTENT_KEY, mUpdateApp);
            bundle.putBoolean(IS_CHECK_UPDATE_BY_USER, buidlerParam.isCheckUpdateByUser);

            if (buidlerParam.mThemeColor != 0) {
                bundle.putInt(THEME_KEY, buidlerParam.mThemeColor);
            }
            UpdateDialogFragment
                    .newInstance(bundle)
                    .setUpdateDialogFragmentListener(buidlerParam.mUpdateDialogFragmentListener)
                    .show(((FragmentActivity) buidlerParam.mActivity).getSupportFragmentManager(), "dialog");

            if (!mUpdateApp.isConstraint()) {
                //提醒次数减1
                AppUpdateUtils.increaseTipCount(getContext(), mUpdateApp.getNewVersion());
            }
            return true;
        }

        return false;

    }


    public void checkUpdate() {
        if (buidlerParam.updateCallback == null) {
            throw new IllegalArgumentException("updateCallback is null");
        }
        buidlerParam.updateCallback.onBefore();
        if (DownloadService.isRunning || UpdateDialogFragment.isShow) {
            buidlerParam.updateCallback.onAfter();
            buidlerParam.mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(buidlerParam.mActivity, "app正在更新", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }


        if (!buidlerParam.mIgnoreDefParams) {
            if (!TextUtils.isEmpty(buidlerParam.mAppKey)) {
                params.put("appKey", buidlerParam.mAppKey);
            }
            String versionName = AppUpdateUtils.getVersionName(buidlerParam.mActivity);
            //过滤掉，debug 这情况
            if (versionName.endsWith("-debug")) {
                versionName = versionName.substring(0, versionName.lastIndexOf('-'));
            }
            if (!TextUtils.isEmpty(versionName)) {
                params.put("versionName", versionName);
            }
            params.put("versionCode", "" + AppUpdateUtils.getVersionCode(getContext()));
            params.put("packageName", "" + getContext().getPackageName());
            params.put("manufacturer", Build.MANUFACTURER);
            params.put("sdk_version", "" + Build.VERSION.SDK_INT);
            params.put("product", Build.PRODUCT);
            params.put("cpu_abi", Build.CPU_ABI);
        }

        //添加自定义参数，其实可以实现HttManager中添加
        if (buidlerParam.requestParams != null && !buidlerParam.requestParams.isEmpty()) {
            if (buidlerParam.mIgnoreDefParams) {
                params.clear();
            }
            params.putAll(buidlerParam.requestParams);
            buidlerParam.requestParams = params;
        }

        //网络请求
        if (buidlerParam.isPost) {
            buidlerParam.mHttpManager.asyncPost(buidlerParam.checkUpdateUrl, params, null, new HttpManager.HttpCallback() {
                @Override
                public void onResponse(String result) {
                    buidlerParam.updateCallback.onAfter();
                    if (result != null) {
                        processData(result, buidlerParam.updateCallback);
                    }
                }

                @Override
                public void onError(String error) {
                    buidlerParam.updateCallback.onAfter();
                    buidlerParam.updateCallback.noNewApp(error);
                }
            });
        } else {
            buidlerParam.mHttpManager.asyncGet(buidlerParam.checkUpdateUrl, params, null, new HttpManager.HttpCallback() {
                @Override
                public void onResponse(String result) {
                    buidlerParam.updateCallback.onAfter();
                    if (result != null) {
                        processData(result, buidlerParam.updateCallback);
                    }
                }

                @Override
                public void onError(String error) {
                    buidlerParam.updateCallback.onAfter();
                    buidlerParam.updateCallback.noNewApp(error);
                }
            });
        }
    }

    /**
     * 可以直接利用下载功能，
     *
     * @param context          上下文
     * @param updateAppBean    下载信息配置
     * @param downloadCallback 下载回调
     */
    public static void download(final Context context, @NonNull final UpdateAppBean updateAppBean, @Nullable final DownloadCallback downloadCallback) {

        DownloadService.bindService(context.getApplicationContext(), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                ((DownloadService.DownloadBinder) service).start(updateAppBean, downloadCallback);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        });
    }

    /**
     * 后台下载
     *
     * @param downloadCallback 后台下载回调
     */
    public void download(@Nullable final DownloadCallback downloadCallback) {
        if (mUpdateApp == null) {
            throw new NullPointerException("updateApp 不能为空");
        }

        DownloadService.bindService(buidlerParam.mActivity.getApplicationContext(), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                ((DownloadService.DownloadBinder) service).start(mUpdateApp, downloadCallback);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        });
    }

    /**
     * 后台下载
     */
    public void download() {
        download(null);
    }

    /**
     * 解析
     *
     * @param result
     * @param callback
     */
    private void processData(String result, @NonNull UpdateCallback callback) {
        try {
            mUpdateApp = callback.parseJson(result);
            if (mUpdateApp == null) {
                return;
            }
            mUpdateApp.setStorePath(buidlerParam.storePath);
            mUpdateApp.setDownLoadManager(buidlerParam.downLoadManager);

            if (mUpdateApp.isUpdate()) {
                callback.hasNewApp(mUpdateApp, this);
                //假如是静默下载，可能需要判断，
                //是否wifi,
                //是否已经下载，如果已经下载直接提示安装
                //没有则进行下载，监听下载完成，弹出安装对话框
            } else {
                callback.noNewApp("没有新版本");
            }
        } catch (Exception e) {
            e.printStackTrace();
            callback.noNewApp(String.format("解析自定义更新配置消息出错[%s]", e.getMessage()));
        }
    }


    private static class BuidlerParam {
        private Activity mActivity;
        private HttpManager mHttpManager;
        private ApkDownLoadManager downLoadManager;
        //检测升级的接口
        private String checkUpdateUrl;
        //1，设置按钮，进度条的颜色
        @ColorRes
        private int mThemeColor = 0;
        //3,唯一的appKey
        private String mAppKey;
        //4,apk下载后的保存路径
        private String storePath;
        //5,是否是post请求，默认是get
        private boolean isPost;
        //6,网络自定义参数
        private Map<String, String> requestParams;
        // 是否忽略默认参数
        private boolean mIgnoreDefParams = false;
        //7,是否隐藏对话框下载进度条
        private boolean mHideDialog = false;
        private boolean mOnlyWifi;
        //是不是用户主动触发升级，如果是，此时应该忽略服务器返回的提醒次数及用户已经点击"忽略此版本"的影响
        private boolean isCheckUpdateByUser;
        private IUpdateDialogFragmentListener mUpdateDialogFragmentListener;
        private UpdateCallback updateCallback;
    }

    public static class Builder {
        private BuidlerParam param = new BuidlerParam();

        public Builder setUpdateCallback(UpdateCallback updateCallback) {
            param.updateCallback = updateCallback;
            return this;
        }

        public UpdateCallback getUpdateCallback() {
            return param.updateCallback;
        }

        public Map<String, String> getRequestParams() {
            return param.requestParams;
        }

        public Builder setRequestParams(Map<String, String> params) {
            param.requestParams = params;
            return this;
        }

        public boolean isIgnoreDefParams() {
            return param.mIgnoreDefParams;
        }

        /**
         * @param ignoreDefParams 是否忽略默认的参数注入 appKey version
         * @return Builder
         */
        public Builder setIgnoreDefParams(boolean ignoreDefParams) {
            param.mIgnoreDefParams = ignoreDefParams;
            return this;
        }

        public boolean isPost() {
            return param.isPost;
        }

        /**
         * 是否是post请求，默认是get
         *
         * @param post 是否是post请求，默认是get
         * @return Builder
         */
        public Builder setPost(boolean post) {
            param.isPost = post;
            return this;
        }

        public String getStorePath() {
            return param.storePath;
        }

        /**
         * @param targetPath apk下载后的保存路径
         * @return Builder
         */
        public Builder setStorePath(String targetPath) {
            param.storePath = targetPath;
            return this;
        }

        public String getAppKey() {
            return param.mAppKey;
        }

        /**
         * 唯一的appkey
         *
         * @param appKey 唯一的appkey
         * @return Builder
         */
        public Builder setAppKey(String appKey) {
            param.mAppKey = appKey;
            return this;
        }

        public Activity getActivity() {
            return param.mActivity;
        }

        /**
         * 是否是post请求，默认是get
         *
         * @param activity 当前提示的Activity
         * @return Builder
         */
        public Builder setActivity(Activity activity) {
            param.mActivity = activity;
            return this;
        }

        public HttpManager getHttpManager() {
            return param.mHttpManager;
        }

        /**
         * 设置网络工具
         *
         * @param httpManager 自己实现的网络对象
         * @return Builder
         */
        public Builder setHttpManager(HttpManager httpManager) {
            param.mHttpManager = httpManager;
            return this;
        }

        public ApkDownLoadManager getDownLoadManager() {
            return param.downLoadManager;
        }

        public Builder setDownLoadManager(ApkDownLoadManager downLoadManager) {
            param.downLoadManager = downLoadManager;
            return this;
        }

        public String getUpdateUrl() {
            return param.checkUpdateUrl;
        }

        /**
         * 更新地址
         *
         * @param updateUrl 更新地址
         * @return Builder
         */
        public Builder setUpdateUrl(String updateUrl) {
            param.checkUpdateUrl = updateUrl;
            return this;
        }

        public int getThemeColor() {
            return param.mThemeColor;
        }

        /**
         * 设置按钮，进度条的颜色
         *
         * @param themeColor 设置按钮，进度条的颜色
         * @return Builder
         */
        public Builder setThemeColor(int themeColor) {
            param.mThemeColor = themeColor;
            return this;
        }


        public IUpdateDialogFragmentListener getUpdateDialogFragmentListener() {
            return param.mUpdateDialogFragmentListener;
        }

        /**
         * 设置默认的UpdateDialogFragment监听器
         *
         * @param updateDialogFragmentListener updateDialogFragmentListener 更新对话框关闭监听
         * @return Builder
         */
        public Builder setUpdateDialogFragmentListener(IUpdateDialogFragmentListener updateDialogFragmentListener) {
            param.mUpdateDialogFragmentListener = updateDialogFragmentListener;
            return this;
        }

        /**
         * 是否隐藏对话框下载进度条
         *
         * @return Builder
         */
        public Builder hideDialogOnDownloading() {
            param.mHideDialog = true;
            return this;
        }

        /**
         * @return 是否影藏对话框
         */
        public boolean isHideDialog() {
            return param.mHideDialog;
        }

        public Builder setOnlyWifi() {
            param.mOnlyWifi = true;
            return this;
        }

        public boolean isOnlyWifi() {
            return param.mOnlyWifi;
        }

        public Builder handleException(ExceptionHandler exceptionHandler) {
            ExceptionHandlerHelper.init(exceptionHandler);
            return this;
        }

        public Builder setCheckUpdateByUser(boolean checkUpdateByUser) {
            param.isCheckUpdateByUser = checkUpdateByUser;
            return this;
        }

        public boolean getCheckUpdateByUser() {
            return param.isCheckUpdateByUser;
        }


        /**
         * @return 生成app管理器
         */
        public UpdateAppManager build() {
            //校验
            if (getActivity() == null || getHttpManager() == null || TextUtils.isEmpty(getUpdateUrl())) {
                throw new NullPointerException("必要参数不能为空");
            }
            //当用户未设置下载文件保存路径时，给它整个默认的
            if (TextUtils.isEmpty(getStorePath())) {
                String path = "";
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) || !Environment.isExternalStorageRemovable()) {
                    path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                } else {
                    path = getActivity().getCacheDir().getAbsolutePath();
                }
                setStorePath(path);
            }
            //当用户未设置appKey时，尝试从manifest文件中读取
            if (TextUtils.isEmpty(getAppKey())) {
                String appKey = AppUpdateUtils.getManifestString(getActivity(), UPDATE_APP_KEY);
                if (!TextUtils.isEmpty(appKey)) {
                    setAppKey(appKey);
                }
            }
            return new UpdateAppManager(this);
        }


    }

}

