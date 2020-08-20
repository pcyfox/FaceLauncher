package ch.arnab.launcher.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.taike.lib_network.udp.UDPSocketClient;
import com.tk.launcher.R;

import ch.arnab.launcher.App;
import ch.arnab.launcher.RootUtils;
import ch.arnab.launcher.entity.Action;
import ch.arnab.launcher.entity.LauncherMessage;

public class HomeScreenActivity extends FragmentActivity {
    private Handler worker;
    private static final String TAG = "HomeScreenActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homescreen);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (worker != null) {
            return;
        }
        PermissionUtils.permission(PermissionConstants.STORAGE);
        Log.d(TAG, "onPostCreate() called with: isAppRoot = [" + AppUtils.isAppRoot() + "]");
        initUDP();
    }

    private void initUDP() {
        getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() {
                HandlerThread handlerThread = new HandlerThread("worker");
                handlerThread.start();
                worker = new Handler(handlerThread.getLooper());
                UDPSocketClient.getInstance().setMsgArrivedListener(new UDPSocketClient.MsgArrivedListener() {
                    @Override
                    public void onMsgArrived(final String msg) {
                        Log.d(TAG, "onMsgArrived() called with: msg = [" + msg + "]");
                        worker.post(new Runnable() {
                            @Override
                            public void run() {
                                if (msg.contains("action")) {
                                    LauncherMessage launcherMessage = getMsg(msg);
                                    if (launcherMessage == null) {
                                        return;
                                    }
                                    String data = launcherMessage.getData();
                                    switch (findAction(msg)) {
                                        case EXEC_CMD:
                                            RootUtils.execCmdAsync(data);
                                            break;
                                        case CLOSE_APP:
                                            killApp(data);
                                            break;
                                        case LAUNCH_APP:
                                            launchApp(data, false);
                                            break;
                                        case LAUNCH_APP_SINGLE_TOP:
                                            launchApp(data, true);
                                            break;
                                        case NOTIFY_APP_UPDATE:
                                            break;
                                        case CLEAR_APP:
                                            RootUtils.cleatApp(data);
                                            break;
                                        case INSTALL_APP:
                                            RootUtils.installAPK(HomeScreenActivity.this, data);
                                            break;
                                        case UNINSTALL_APP:
                                            RootUtils.uninstallAPK(data);
                                            break;
                                    }
                                }
                            }
                        });
                    }
                });
            }
        }, 2 * 1000);
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
        RootUtils.startApp(pkgName);
    }

    private void killApp(final String pkgName) {
        boolean isInstalled = AppUtils.isAppInstalled(pkgName);
        if (TextUtils.isEmpty(pkgName) || !isInstalled) {
            return;
        }
        RootUtils.killApp(pkgName);
    }


    public void onTest(View view) {
        //{"action":"CLOSE_APP","data":"com.taike.edu.stu"}
        UDPSocketClient.getInstance().sendBroadcast(GsonUtils.toJson(new LauncherMessage(Action.LAUNCH_APP, "com.taike.edu.stu")));
        //RootUtils.cleatApp("com.taike.edu.stu");
        //RootUtils.uninstallAPK("com.taike.edu.stu");
        // RootUtils.installAPK(this, "/sdcard/debug_TK-Stu_V1.0.0_1_2020-08-20_19-47-52.apk");
    }

    private Action findAction(String msg) {
        for (Action action : Action.values()) {
            if (msg.contains(action.name())) {
                return action;
            }
        }
        return Action.EXEC_CMD;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home_screen, menu);
        return true;
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //桌面被干死了，重置登录状态
        App.Companion.setRegister(false);
        UDPSocketClient.getInstance().stopUDPSocket();
    }
}
