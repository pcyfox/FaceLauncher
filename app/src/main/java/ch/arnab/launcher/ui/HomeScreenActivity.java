package ch.arnab.launcher.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.taike.lib_network.udp.UDPSocketClient;
import com.tk.launcher.BuildConfig;
import com.tk.launcher.R;

import java.util.List;

import ch.arnab.launcher.App;
import ch.arnab.launcher.AppModel;
import ch.arnab.launcher.SocketMsgHandler;
import ch.arnab.launcher.entity.Action;
import ch.arnab.launcher.entity.LauncherMessage;

public class HomeScreenActivity extends FragmentActivity {
    private static final String TAG = "HomeScreenActivity";
    private AppsGridFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homescreen);
        fragment = (AppsGridFragment) getSupportFragmentManager().findFragmentById(R.id.apps_grid);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        PermissionUtils.permission(PermissionConstants.STORAGE);
        handleUSBDisk();
        Log.d(TAG, "onPostCreate() called with: isAppRoot = [" + AppUtils.isAppRoot() + "]");
        SocketMsgHandler.getInstance().init(this, new SocketMsgHandler.AppModelGetter() {
            @Override
            public List<AppModel> get() {
                return fragment.getAllApps();
            }
        });
        if (BuildConfig.APP_TYPE == 1) {
            getWindow().getDecorView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ActivityUtils.startActivity(CmdActivity.class);
                }
            }, 1);
        }
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
      //  UDPSocketClient.getInstance().sendBroadcast(GsonUtils.toJson(new LauncherMessage(Action.INSTALL_APP, "http://192.168.28.11:8080/edu_stu.apk")));
        //RootUtils.cleatApp("com.taike.edu.stu");
        //RootUtils.uninstallAPK("com.taike.edu.stu");
        // RootUtils.installAPK(this, "/sdcard/debug_TK-Stu_V1.0.0_1_2020-08-20_19-47-52.apk");
    }
}
