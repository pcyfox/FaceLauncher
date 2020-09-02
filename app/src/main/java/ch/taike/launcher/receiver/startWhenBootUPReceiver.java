package ch.taike.launcher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.blankj.utilcode.util.SPStaticUtils;


public class startWhenBootUPReceiver extends BroadcastReceiver {
    private final String activeName = "android.intent.action.BOOT_COMPLETED";
    private static final String ACTION_SHUTDOWN = "android.intent.action.ACTION_SHUTDOWN";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("startWhenBootup", "开机自动服务自动启动...");
        if (intent.getAction().equals(activeName)) {

        } else {
            SPStaticUtils.clear();
        }

    }
}
