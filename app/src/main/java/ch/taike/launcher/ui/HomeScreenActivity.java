package ch.taike.launcher.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.SPStaticUtils;
import com.blankj.utilcode.util.ShellUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.Utils;
import com.elvishew.xlog.XLog;
import com.taike.lib_common.config.AppConfig;
import com.taike.lib_utils.IPutils;
import com.taike.lib_utils.Util;
import com.tk.launcher.BuildConfig;
import com.tk.launcher.R;
import com.vector.update_app.update.UpdateHelper;

import ch.taike.launcher.App;
import ch.taike.launcher.RootUtils;
import ch.taike.launcher.entity.Action;
import ch.taike.launcher.manager.SocketMsgHandler;
import ch.taike.launcher.update.UpdateAppManagerUtils;

public class HomeScreenActivity extends FragmentActivity {
    private final static String EXAM_APP_PACKAGE_NAME = "com.taike.student";
    private static final String TAG = "HomeScreenActivity";
    private AppsGridFragment fragment;
    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homescreen);
        fragment = (AppsGridFragment) getSupportFragmentManager().findFragmentById(R.id.apps_grid);
        TextView versionName = findViewById(R.id.tv_version);
        versionName.setText(AppUtils.getAppVersionName());
        TextView tvDeviceID = findViewById(R.id.tv_device_id);
        tvDeviceID.setText(Util.genClientId());
        versionName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count++;
                if (count % 7 == 0) {
                    tvDeviceID.setVisibility(View.VISIBLE);
                } else {
                    tvDeviceID.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        handleUSBDisk();
        XLog.d(TAG + ":onPostCreate() called with: isAppRoot = [" + AppUtils.isAppRoot() + "]");
        SPStaticUtils.put("IP", IPutils.getIpAdress(this));
        initSocketManager();
    }

    private void initSocketManager() {
        SocketMsgHandler.getInstance().init(this, () -> fragment.getAllApps());
        SocketMsgHandler.getInstance().setMessageInterceptor(message -> {
            if (message.getAction() != Action.LAUNCH_APP) {
                return false;
            }
            ShellUtils.CommandResult re = ShellUtils.execCmd("ps", true);
            if (re.successMsg.contains(EXAM_APP_PACKAGE_NAME)) {
                ToastUtils.showLong("考生端软件正在运行,不能切换应用");
                return true;
            }
            return false;
        });
    }


    private void checkUpdate() {
        Log.d(TAG, "checkUpdate() called");
        UpdateHelper.getInstance().setHttpManager(UpdateAppManagerUtils.getDefHttpManagerImpl()).checkUpdate(HomeScreenActivity.this, false, AppConfig.getCheckUpDateUrl(), UpdateAppManagerUtils.getDefDownLoadManagerImpl());
    }

    public void getRunningApp() {
        RootUtils.execCmdAsync("ps", new Utils.Callback<ShellUtils.CommandResult>() {
            @Override
            public void onCall(ShellUtils.CommandResult data) {
                XLog.e(TAG + ":onCall() called with ps: data = [" + data + "]");
            }
        });
    }


    @Override
    protected void onPostResume() {
        super.onPostResume();
        getRunningApp();
        PermissionUtils.permission(PermissionConstants.STORAGE).request();
        String ip = SPStaticUtils.getString("IP");
        if (!TextUtils.isEmpty(ip)) {
            if (!ip.equals(IPutils.getIpAdress(this))) {
                SocketMsgHandler.getInstance().reconnect();
            }
        }

        if (BuildConfig.APP_TYPE == 1) {
            getWindow().getDecorView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ActivityUtils.startActivity(CmdActivity.class);
                }
            }, 1);
        }

        double delay = Math.random() * 1000 * 60 * 2;
        Log.d(TAG, "onPostResume() called delay=" + delay);
        getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkUpdate();
            }
        }, (int) delay);
    }

    private void handleUSBDisk() {
        BroadcastReceiver mSdcardReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)) {
                    Toast.makeText(context, "path:" + intent.getData().getPath(), Toast.LENGTH_SHORT).show();
                    Intent openIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    openIntent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                    openIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(openIntent, 1);
                } else if (intent.getAction().equals(Intent.ACTION_MEDIA_REMOVED)) {
                    Log.i("123", "remove ACTION_MEDIA_REMOVED");
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);   //接受外媒挂载过滤器
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);   //接受外媒挂载过滤器
        filter.addDataScheme("file");
        registerReceiver(mSdcardReceiver, filter, "android.permission.READ_EXTERNAL_STORAGE", null);
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
        SocketMsgHandler.getInstance().stop();
    }

    public void onTest(View view) {
        //{"action":"CLOSE_APP","data":"com.taike.edu.stu"}
        //UDPSocketClient.getInstance().sendBroadcast(GsonUtils.toJson(new LauncherMessage(Action.LAUNCH_APP_SINGLE_TOP, "com.taike.edu.stu")));
        // UDPSocketClient.getInstance().sendBroadcast(GsonUtils.toJson(new LauncherMessage(Action.INSTALL_APP, "http://192.168.28.11:8080/edu_stu.apk")));
        //RootUtils.cleatApp("com.taike.edu.stu");
        //RootUtils.uninstallAPK("com.taike.edu.stu");
        // RootUtils.installAPK(this, "/sdcard/debug_TK-Stu_V1.0.0_1_2020-08-20_19-47-52.apk");
    }
}
