package ch.taike.launcher;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;

import com.blankj.utilcode.util.ShellUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.Utils;
import com.elvishew.xlog.XLog;
import com.tk.launcher.BuildConfig;

public class RootUtils {
    private static final String TAG = "RootUtils";

    /**
     * 为app申请root权限
     */
    public static ShellUtils.CommandResult grantRoot(Context context) {
        return ShellUtils.execCmd("chmod 777 " + context.getPackageCodePath(), true);
    }


    public static void installAPK(String apkPath, Utils.Callback<ShellUtils.CommandResult> callback) {
        execCmdAsync("pm install -r " + apkPath, callback);
    }

    public static void uninstallAPK(String packName) {
        XLog.i(TAG + ":uninstallAPK() called with: packName = [" + packName + "]");
        execCmdAsync("pm uninstall --user 0 " + packName);
    }

    public static void clearApp(String packName) {
        execCmdAsync("pm clear " + packName);
    }


    public static void killApp(String pkgName) {
        execCmdAsync("am force-stop  " + pkgName);
    }

    public static void startApp(String pkgName, Utils.Callback<ShellUtils.CommandResult> callback) {
        execCmdAsync("am start " + pkgName, callback);
    }

    public static void startApp(String pkgName) {
        execCmdAsync("am start " + pkgName);
    }

    public static void wmOverscan(Rect windowRect) {
        execCmdAsync("wm overscan " + windowRect.left + "," + windowRect.top + "," + windowRect.right + "," + windowRect.bottom);
    }


    public static void execCmdAsync(String cmd) {
        ShellUtils.execCmdAsync(cmd, true, new Utils.Callback<ShellUtils.CommandResult>() {
            @Override
            public void onCall(ShellUtils.CommandResult data) {
                XLog.i(TAG + ":adb execCmdAsync called with:cmd=" + cmd + ",result= [" + data + "]");
                if (data != null && (BuildConfig.DEBUG)) {
                    if (data.result == 0) {
                        ToastUtils.showShort("执行成功！");
                    } else {
                        ToastUtils.showLong("执行失败,msg：" + data.toString());
                    }
                }
            }
        });
    }

    public static void execCmdAsync(String cmd, Utils.Callback<ShellUtils.CommandResult> callback) {
        if (callback == null) {
            execCmdAsync(cmd);
            return;
        }
        ShellUtils.execCmdAsync(cmd, true, callback);
    }
}
