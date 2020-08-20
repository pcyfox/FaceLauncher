package ch.arnab.launcher;

import android.content.Context;
import android.util.Log;

import com.blankj.utilcode.util.ShellUtils;
import com.blankj.utilcode.util.Utils;

public class RootUtils {
    private static final String TAG = "RootUtils";

    /**
     * 安装apk
     */
    public static void installAPK(Context context, String apkPath) {
        execCmdAsync("pm install -i " + context.getPackageName() + " --user 0 " + apkPath);
    }

    public static void uninstallAPK(String packName) {
        execCmdAsync("pm uninstall " + packName);
    }

    public static void cleatApp(String packName) {
        execCmdAsync("pm clear " + packName);
    }


    public static void killApp(String pkgName) {
        execCmdAsync("am force-stop  " + pkgName);
    }

    public static void startApp(String pkgName) {
        execCmdAsync("am start " + pkgName);
    }


    public static void execCmdAsync(String cmd) {
        ShellUtils.execCmdAsync(cmd, true, new Utils.Callback<ShellUtils.CommandResult>() {
            @Override
            public void onCall(ShellUtils.CommandResult data) {
                Log.d(TAG, "onCall() called with: data = [" + data + "]");
            }
        });
    }
}
