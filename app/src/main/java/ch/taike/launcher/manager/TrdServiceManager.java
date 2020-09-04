package ch.taike.launcher.manager;

import android.content.Context;
import android.util.Log;

import com.blankj.utilcode.util.AppUtils;
import com.jeremyliao.liveeventbus.LiveEventBus;
import com.taike.lib_common.BuildConfig;
import com.taike.lib_common.config.AppConfig;
import com.taike.lib_log.XLogHelper;
import com.taike.lib_log.printer.CloudLogPrinter;
import com.taike.lib_log.printer.PrintLogReq;
import com.taike.lib_utils.CrashHandlerUtils;
import com.taike.lib_utils.Util;


/**
 * 第三方库管理类
 */
public final class TrdServiceManager {
    private TrdServiceManager() {
    }

    private static final String TAG = "TrdService";

    public static void initLog(String clientName, String ELKUrl) {
        Log.d(TAG, "initLog() called with: clientName = [" + clientName + "], ELKUrl = [" + ELKUrl + "]");
        PrintLogReq printLogReq = new PrintLogReq(clientName,
                Util.genClientId(),
                BuildConfig.DEBUG,
                BuildConfig.BASE_URL,
                "" + AppUtils.getAppVersionCode());

        CloudLogPrinter cloudLogPrinter = CloudLogPrinter.getInstance();
        cloudLogPrinter.init(printLogReq, ELKUrl, clientName);
        cloudLogPrinter.setQuantityInterval(5);
        XLogHelper.initLog(cloudLogPrinter, AppConfig.getLogPath(), clientName, clientName);
    }

    public static void initCrashHandler(Context context) {
        CrashHandlerUtils.getInstance().init(context);
    }

    /**
     * 上传上次APP未上传的log
     */
    public static void uploadCacheLog() {
        Log.d(TAG, "uploadCacheLog() called");
        XLogHelper.uploadCache();
    }

    public static void initLiveEventBus() {
        LiveEventBus.get()
                .config()
                // .supportBroadcast(this)//配置支持跨进程、跨APP通信
                //true：整个生命周期（从onCreate到onDestroy）都可以实时收到消息
                //false：激活状态（Started）可以实时收到消息，非激活状态（Stoped）无法实时收到消息，需等到Activity重新变成激活状态，方可收到消息
                .lifecycleObserverAlwaysActive(false)
                //配置在没有Observer关联的时候是否自动清除LiveEvent以释放内存
                .autoClear(true);
    }
}
