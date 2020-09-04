package com.vector.update_app.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.vector.update_app.R;
import com.vector.update_app.UpdateAppBean;
import com.vector.update_app.interf.ApkDownLoadManager;
import com.vector.update_app.interf.DownloadCallback;
import com.vector.update_app.utils.AppUpdateUtils;

import java.io.File;


/**
 * 后台下载
 */
public class DownloadService extends Service {
    private static final int NOTIFY_ID = 110;
    private static final String TAG = "DownloadService";
    private static final String CHANNEL_ID = "app_update_id";
    private static final CharSequence CHANNEL_NAME = "AppUpdate";
    private static final String ACTION_NOTIFICATION_CLICKED = "ACTION_NOTIFICATION_CLICKED";

    public static boolean isRunning = false;
    private NotificationManager mNotificationManager;
    private DownloadBinder binder = new DownloadBinder();
    private NotificationCompat.Builder mBuilder;
    private boolean isDismissNotificationProgress = false;
    private static final int REQUEST_CODE = 110;
    private UpdateAppBean currentUpdateApp;
    private boolean isDownloading;
    private DownloadCallback callback;

    /**
     * 非跨进程
     *
     * @param context
     * @param connection
     */
    public static void bindService(Context context, ServiceConnection connection) {
        Intent intent = new Intent(context, DownloadService.class);
        context.startService(intent);
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        isRunning = true;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        isRunning = false;
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_NOTIFICATION_CLICKED.equals(intent.getAction())) {
            if (currentUpdateApp != null) {
                if (!isDownloading) {
                    DownloadService.this.startDownload(currentUpdateApp, callback);
                } else {
                    DownloadService.this.pauseDownload(currentUpdateApp);
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // 返回自定义的DownloadBinder实例
        return binder;
    }

    @Override
    public void onDestroy() {
        mNotificationManager = null;
        super.onDestroy();
    }

    /**
     * 创建通知
     */
    private void setUpNotification() {
        if (isDismissNotificationProgress) {
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_MIN);
            channel.enableVibration(false);
            channel.enableLights(false);
            mNotificationManager.createNotificationChannel(channel);
        }

        Intent clickIntent = new Intent(this, DownloadService.class);
        clickIntent.setAction(ACTION_NOTIFICATION_CLICKED);

        mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        mBuilder.setPriority(Notification.PRIORITY_MIN);
        mBuilder.setContentTitle("开始下载")
                .setContentText("正在连接服务器...")
                .setSmallIcon(R.mipmap.lib_update_app_update_icon)
                .setLargeIcon(AppUpdateUtils.drawableToBitmap(getResources().getDrawable(R.mipmap.ic_launcher)))
                .setOngoing(false)
                .setAutoCancel(false)
                .setVibrate(null)
                .setVibrate(new long[]{0})//关闭震动
                .setDefaults(NotificationCompat.FLAG_ONLY_ALERT_ONCE)//统一消除声音和震动
                .setContentIntent(PendingIntent.getService(this, REQUEST_CODE, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setWhen(System.currentTimeMillis());

        mNotificationManager.notify(NOTIFY_ID, mBuilder.build());
    }

    /**
     * 下载模块
     */
    private void startDownload(UpdateAppBean updateApp, final DownloadCallback callback) {
        Log.d(TAG, "start() called with: updateApp = [" + updateApp + "], callback = [" + callback + "]");
        currentUpdateApp = updateApp;
        isDismissNotificationProgress = updateApp.isDismissNotification();
        String apkUrl = updateApp.getApkFileUrl();
        if (TextUtils.isEmpty(apkUrl)) {
            String contentText = "新版本下载路径错误";
            stopServer(contentText);
            return;
        }
        String appName = AppUpdateUtils.getApkName(updateApp);
        File appDir = new File(updateApp.getStorePath());
        if (!appDir.exists()) {
            appDir.mkdirs();
        }
        String target = appDir + File.separator + getApplication().getPackageName() + "_" + updateApp.getNewVersion();
        updateApp.getDownLoadManager().startDownload(apkUrl, target, appName, new FileDownloadCallBack(callback));
    }


    private void pauseDownload(UpdateAppBean updateApp) {
        if (updateApp == null) return;
        ApkDownLoadManager manager = updateApp.getDownLoadManager();
        if (manager == null) {
            return;
        }
        manager.pauseDownLoad(updateApp.getApkFileUrl());
    }


    private void stopServer(String contentText) {
        if (mBuilder != null) {
            mBuilder.setContentTitle(AppUpdateUtils.getAppName(DownloadService.this))
                    .setContentText(contentText);
            Notification notification = mBuilder.build();
            notification.flags = Notification.FLAG_AUTO_CANCEL;
            mNotificationManager.notify(NOTIFY_ID, notification);
            mNotificationManager.cancel(NOTIFY_ID);
        }

        close();
    }

    private void close() {
        stopSelf();
        isRunning = false;
    }


    /**
     * DownloadBinder中定义了一些实用的方法
     *
     * @author user
     */
    public class DownloadBinder extends Binder {
        /**
         * 开始下载
         *
         * @param updateApp 新app信息
         * @param callback  下载回调
         */
        public void start(UpdateAppBean updateApp, DownloadCallback callback) {
            //下载
            DownloadService.this.callback = callback;
            DownloadService.this.startDownload(updateApp, callback);
        }

        public void stop(String msg, UpdateAppBean updateApp) {
            ApkDownLoadManager downLoadManager = updateApp.getDownLoadManager();
            if (downLoadManager != null) {
                downLoadManager.cancelDownLoad(updateApp.getApkFileUrl());
            }
            DownloadService.this.stopServer(msg);
        }
    }


    class FileDownloadCallBack implements ApkDownLoadManager.FileDownloadCallback {
        private final DownloadCallback downloadCallback;
        int oldRate = 0;

        public FileDownloadCallBack(@Nullable DownloadCallback callback) {
            super();
            this.downloadCallback = callback;
        }

        @Override
        public void onStart() {
            //初始化通知栏
            setUpNotification();
            if (downloadCallback != null) {
                downloadCallback.onStart();
                isDownloading = true;
            }
        }

        @Override
        public void onPause() {
            isDownloading = false;
            if (mBuilder != null) {
                if (isDismissNotificationProgress) {
                    return;
                }
                mBuilder.setContentTitle("已暂停")
                        .setWhen(System.currentTimeMillis());
                Notification notification = mBuilder.build();
                mNotificationManager.notify(NOTIFY_ID, notification);
            }
        }

        @Override
        public void onCanceled() {
            isDownloading = false;
        }

        @SuppressLint("WrongConstant")
        @Override
        public void onProgress(float progress, long total) {
            //做一下判断，防止自回调过于频繁，造成更新通知栏进度过于频繁，而出现卡顿的问题。
            int rate = Math.round(progress / total * 100);
            if (oldRate != rate) {
                if (downloadCallback != null) {
                    downloadCallback.setMax(total);
                    downloadCallback.onProgress(progress, total);
                }
                if (mBuilder != null) {
                    if (isDismissNotificationProgress) {
                        return;
                    }
                    mBuilder.setPriority(Notification.PRIORITY_MIN);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mBuilder.setVisibility(Notification.VISIBILITY_PRIVATE);
                    }
                    mBuilder.setContentTitle("正在下载：" + AppUpdateUtils.getAppName(DownloadService.this))
                            .setContentText(rate + "%")
                            .setProgress(100, rate, false)
                            .setWhen(System.currentTimeMillis());

                    Notification notification = mBuilder.build();
                    notification.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONLY_ALERT_ONCE;
                    mNotificationManager.notify(NOTIFY_ID, notification);
                }
                //重新赋值
                oldRate = rate;
            }
        }

        @Override
        public void onError(String error) {
            Log.e(TAG, "onError() called with: error = [" + error + "]");
            if (!isDismissNotificationProgress) {
                Toast.makeText(DownloadService.this, "更新新版本出错，" + error, Toast.LENGTH_SHORT).show();
            }
            //App前台运行
            if (downloadCallback != null) {
                downloadCallback.onError(error);
            }
            try {
                mNotificationManager.cancel(NOTIFY_ID);
                close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

        @Override
        public void onFinish(File file, boolean isNeedInstall) {
            Log.d(TAG, "onFinish() called with: file = [" + file + "], isNeedInstall = [" + isNeedInstall + "]");
            if (!isNeedInstall) {
                close();
                return;
            }
            if (downloadCallback != null) {
                if (!downloadCallback.onFinish(file)) {
                    close();
                    return;
                }
            }
            try {

                if (AppUpdateUtils.isAppOnForeground(DownloadService.this) || mBuilder == null) {
                    //App前台运行
                    mNotificationManager.cancel(NOTIFY_ID);
                    if (downloadCallback != null) {
                        boolean temp = downloadCallback.onInstallAppAndAppOnForeground(file);
                        if (!temp) {
                            AppUpdateUtils.installApp(DownloadService.this, file);
                        }
                    } else {
                        AppUpdateUtils.installApp(DownloadService.this, file);
                    }
                } else {
                    //App后台运行
                    //更新参数,注意flags要使用FLAG_UPDATE_CURRENT
                    Intent installAppIntent = AppUpdateUtils.getInstallAppIntent(DownloadService.this, file);
                    PendingIntent contentIntent = PendingIntent.getService(DownloadService.this, 0, installAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    mBuilder.setContentIntent(contentIntent)
                            .setContentTitle(AppUpdateUtils.getAppName(DownloadService.this))
                            .setContentText("下载完成，请点击安装")
                            .setProgress(0, 0, false)
                            .setAutoCancel(false);
                    Notification notification = mBuilder.build();
                    notification.flags = Notification.FLAG_AUTO_CANCEL;
                    mNotificationManager.notify(NOTIFY_ID, notification);
                }
                //下载完自杀
                close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                close();
            }
        }
    }
}
