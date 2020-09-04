package com.vector.update_app.update;

import android.app.Activity;

import com.vector.update_app.UpdateAppBean;
import com.vector.update_app.UpdateAppManager;
import com.vector.update_app.interf.ApkDownLoadManager;
import com.vector.update_app.interf.HttpManager;
import com.vector.update_app.listener.IUpdateDialogFragmentListener;

import static com.vector.update_app.UpdateDialogFragment.STATE_IGNORE;
import static com.vector.update_app.UpdateDialogFragment.STATE_NORMAL;

public final class UpdateHelper {
    private static final String TAG = "UpdateManager";
    private static String UPDATE_DIALOG_SHOWN_TIMES = "UPDATE_DIALOG_SHOWN_TIME";
    private static String REQUEST_PARAM = "REQUEST_PARAM";
    private static UpdateHelper instance = new UpdateHelper();
    private HttpManager httpManager;
    private final UpdateAppManager.Builder builder;

    private UpdateHelper() {
        httpManager = new HttpManagerImpl();
        builder = new UpdateAppManager.Builder();
    }

    public static UpdateHelper getInstance() {
        return instance;
    }

    /**
     * @return 弹窗次数，正常弹出加1，正常关闭减1
     */
    public static int getTimes() {
        return SettingStorage.get().read(UPDATE_DIALOG_SHOWN_TIMES, 0);
    }


    public static boolean isKilledAppWhenDialogIsShowing() {
        //说明检查升级对话框未被关闭，app被杀死
        return UpdateHelper.getTimes() > 0;
    }

    public static void reset() {
        SettingStorage.get().write(UPDATE_DIALOG_SHOWN_TIMES, 0);
        SettingStorage.get().write(REQUEST_PARAM, null);
    }


    /**
     * @param activity            需要检测升级的activity
     * @param isCheckUpdateByUser 是否为用户手动触发升级，此时会不管之前是否已经忽略过该版本
     */
    public void checkUpdate(Activity activity, final boolean isCheckUpdateByUser, String url, ApkDownLoadManager apkDownLoadManager) {
        builder.setDownLoadManager(apkDownLoadManager)
                .setHttpManager(httpManager)
                .setActivity(activity)
                .setCheckUpdateByUser(isCheckUpdateByUser)
                .setUpdateUrl(url)
                .setPost(true)
                .setUpdateCallback(new NormalUpdateCallbackImpl())
                .setUpdateDialogFragmentListener(new IUpdateDialogFragmentListener() {
                    @Override
                    public void onUpdateNotifyDialogOnStart(UpdateAppBean updateApp) {
                        if (!isCheckUpdateByUser) {
                            SettingStorage.get().write(UPDATE_DIALOG_SHOWN_TIMES, getTimes() + 1);
                            SettingStorage.get().write(REQUEST_PARAM, builder.getRequestParams().toString());
                        }
                    }

                    @Override
                    public void onUpdateNotifyDialogCancel(UpdateAppBean updateApp) {
                        if (!isCheckUpdateByUser) {
                            SettingStorage.get().write(UPDATE_DIALOG_SHOWN_TIMES, getTimes() - 1);
                        }
                    }

                    @Override
                    public void onUpdateNotifyDialogIgnore(UpdateAppBean updateApp, int state) {

                        if (!isCheckUpdateByUser) {
                            SettingStorage.get().write(UPDATE_DIALOG_SHOWN_TIMES, getTimes() - 1);
                        }

                        switch (state) {
                            case STATE_NORMAL://暂不升级
                                if (!isCheckUpdateByUser) {
                                }
                                break;
                            case STATE_IGNORE://忽略升级
                                break;
                        }
                    }

                    @Override
                    public void onUpdateNotifyDialogOk(UpdateAppBean updateApp) {
                        if (!isCheckUpdateByUser) {
                            SettingStorage.get().write(UPDATE_DIALOG_SHOWN_TIMES, getTimes() - 1);
                        }
                    }
                }).build()
                .checkUpdate();
    }

    public UpdateHelper setHttpManager(HttpManager httpManager) {
        this.httpManager = httpManager;
        return this;
    }
}
