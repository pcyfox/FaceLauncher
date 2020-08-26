package ch.arnab.launcher;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.taike.lib_network.download.DownLoadCallback;
import com.taike.lib_network.download.DownloadManager;
import com.taike.lib_network.udp.UDPSocketClient;

import java.io.File;
import java.util.List;

import ch.arnab.launcher.constant.ConstantData;
import ch.arnab.launcher.entity.LauncherMessage;

import static ch.arnab.launcher.constant.ConstantData.BROADCAST_ACTION_DISC;
import static ch.arnab.launcher.constant.ConstantData.BROADCAST_PERMISSION_DISC;

public class SocketMsgHandler {
    private static final String TAG = "SocketMsgHandler";
    private AppModelGetter appModelGetter;
    private Context context;
    private Handler worker;
    private static final SocketMsgHandler instance = new SocketMsgHandler();

    private SocketMsgHandler() {
    }

    public static SocketMsgHandler getInstance() {
        return instance;
    }


    public void init(final Context context, AppModelGetter appModelGetter) {
        this.appModelGetter = appModelGetter;
        this.context = context;
        initWorker();
        worker.post(new Runnable() {
            @Override
            public void run() {
                RootUtils.grantRoot(context.getApplicationContext());
                initUDP();
            }
        });
    }


    private void initWorker() {
        HandlerThread handlerThread = new HandlerThread("worker");
        handlerThread.start();
        worker = new Handler(handlerThread.getLooper());
    }

    private void handleMsg(final String msg) {
        Log.d(TAG, "onMsgArrived() called with: msg = [" + msg + "]");
        worker.post(new Runnable() {
            @Override
            public void run() {
                if (msg.contains("action")) {
                    LauncherMessage launcherMessage = getMsg(msg);
                    if (launcherMessage == null) {
                        return;
                    }
                    ToastUtils.showShort(msg);
                    String data = launcherMessage.getData();
                    switch (launcherMessage.getAction()) {
                        case EXEC_CMD:
                            RootUtils.execCmdAsync(data);
                            break;
                        case CLOSE_APP:
                            killApp(data);
                            break;
                        case LAUNCH_APP_SINGLE_TOP:
                            launchApp(data, true);
                            break;
                        case LAUNCH_APP:
                            launchApp(data, false);
                            break;
                        case NOTIFY_APP_UPDATE:
                            notifyCheckUpdate(data);
                            break;
                        case CLEAR_APP:
                            RootUtils.clearApp(data);
                            break;
                        case INSTALL_APP:
                            installApp(data);
                            break;
                        case UNINSTALL_APP:
                            if (AppUtils.isAppSystem(data)) return;
                            RootUtils.uninstallAPK(data);
                            break;
                        case DOWNLOAD_FILE:
                            download(data, false);
                            break;
                    }
                }
            }
        });
    }

    private void installApp(String data) {
        if (new File(data).exists()) {
            RootUtils.installAPK(context, data);
        } else {
            download(data, true);
        }
    }

    private void download(String data, final boolean installApk) {
        DownloadManager.getInstance().download(data, new DownLoadCallback() {
            @Override
            public void onStart() {
                super.onStart();
            }

            @Override
            public void onPause() {
                super.onPause();
            }

            @Override
            public void onProgress(float progress, long totalSize) {
                super.onProgress(progress, totalSize);
            }

            @Override
            public void onFinish(String file) {
                Log.d(TAG, "onFinish() called with: file = [" + file + "]");
                if (installApk && file.toLowerCase().endsWith("apk")) {
                    ToastUtils.showShort("APK下载成功,开始安装");
                    RootUtils.installAPK(context, file);
                }
                super.onFinish(file);
            }

            @Override
            public void onError(String msg) {
                ToastUtils.showLong("下载失败:" + msg);
                Log.e(TAG, "onError: " + msg);
                super.onError(msg);
            }
        });
    }

    private void initUDP() {
        UDPSocketClient.getInstance().setMsgArrivedListener(new UDPSocketClient.MsgArrivedListener() {
            @Override
            public void onMsgArrived(final String msg) {
                handleMsg(msg);
            }
        });
        UDPSocketClient.getInstance().startUDPSocket();
    }


    private LauncherMessage getMsg(String msg) {
        try {
            return GsonUtils.fromJson(msg, LauncherMessage.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private void launchApp(String pkgName, boolean isSingleTop) {
        if (TextUtils.isEmpty(pkgName) || AppUtils.isAppRunning(pkgName) || !AppUtils.isAppInstalled(pkgName)) {
            return;
        }
        if (isSingleTop && appModelGetter != null) {
            List<AppModel> appModels = appModelGetter.get();
            if (appModels == null) {
                return;
            }
            for (AppModel appModel : appModels) {
                String name = appModel.getAppInfo().packageName;
                boolean isTKApp = false;
                for (String flag : ConstantData.TK_APP_FLAGS) {
                    if (name.startsWith(flag) && !name.equals(pkgName) && !name.equals(context.getPackageName())) {
                        Log.d(TAG, "launchApp: TK app is running: " + pkgName);
                        isTKApp = true;
                        break;
                    }
                }
                if (isTKApp) {
                    RootUtils.killApp(name);
                }
            }
        }
        RootUtils.startApp(pkgName);
    }


    private void killApp(final String pkgName) {
        boolean isInstalled = AppUtils.isAppInstalled(pkgName);
        if (TextUtils.isEmpty(pkgName) || !isInstalled) {
            return;
        }
        RootUtils.killApp(pkgName);
    }


    public void stop() {
        UDPSocketClient.getInstance().stopUDPSocket();
    }

    public void notifyCheckUpdate(String pkgName) {
        Intent intent = new Intent();  //Itent就是我们要发送的内容
        intent.putExtra("pkgName", pkgName);
        intent.setAction(BROADCAST_ACTION_DISC);   //设置你这个广播的action，只有和这个action一样的接受者才能接受者才能接收广播
        context.sendBroadcast(intent, BROADCAST_PERMISSION_DISC);   //发送广播
    }

    public interface AppModelGetter {
        List<AppModel> get();
    }

}
