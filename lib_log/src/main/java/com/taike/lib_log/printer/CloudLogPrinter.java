package com.taike.lib_log.printer;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.XLog;
import com.elvishew.xlog.printer.Printer;
import com.google.gson.Gson;
import com.taike.lib_log.LogCache;
import com.taike.lib_log.LogCacheManager;
import com.taike.lib_network.RequestManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 主要是处理日志上传
 */
public class CloudLogPrinter implements Printer {
    private static final String TAG = "CloudLogPrinter";
    private final List<String> mLogs = new ArrayList<>();
    private BasePrintLogReq printLogReq;//必需是个JavaBean
    private String index;
    private int quantityInterval = 30;//上传数量间隔,默认是没满30条就上传
    private static String url = "";
    private Map<String, String> header = new HashMap<>();
    private static final String KEY_LOG_LEVEL = "log_level";
    private SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.CHINA);
    private Handler logUpDateHandler;

    private LogUploadInterceptor logUploadInterceptor;

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public int getQuantityInterval() {
        return quantityInterval;
    }

    public void setQuantityInterval(int quantityInterval) {
        this.quantityInterval = quantityInterval;
    }

    private static volatile boolean isUpdating = false;
    private float addLogCount;
    private long lastAddTime;
    private boolean isTooFast = false;
    private int tooFastCount;//持续发生添加日志过快的次数


    private CloudLogPrinter() {
        HandlerThread handlerThread = new HandlerThread("updateLogHandlerThread");
        handlerThread.start();
        logUpDateHandler = new Handler(handlerThread.getLooper());
    }

    /**
     * @param printLogReq 自定义打印类对象
     */
    public void init(BasePrintLogReq printLogReq/*必需是一个JavaBean*/, String url, String index) {
        this.printLogReq = printLogReq;
        this.index = index;
        CloudLogPrinter.url = url;
    }


    private static CloudLogPrinter instance = new CloudLogPrinter();

    public static CloudLogPrinter getInstance() {
        return instance;
    }

    public BasePrintLogReq getPrintLogReq() {
        return printLogReq;
    }

    public Map<String, String> getHeader() {
        return header;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        CloudLogPrinter.url = url;
    }

    private long getTime() {
        long timeDifference = 0;
        return System.currentTimeMillis() + timeDifference;
    }

    @Override
    public void println(final int logLevel, final String tag, final String msg) {
        // Log.d(TAG, "println() called with: logLevel = [" + logLevel + "], tag = [" + tag + "], msg = [" + msg + "]");
        //  Log.println(logLevel, tag, msg);
        if (logLevel <= LogLevel.DEBUG || TextUtils.isEmpty(msg) || TextUtils.isEmpty(url)) {
            return;
        }

        Runnable worker = new Runnable() {
            @Override
            public void run() {
                String logMsg = msg;
                if (logUploadInterceptor != null) {
                    logMsg = logUploadInterceptor.upload(logLevel, tag, msg);
                }
                String time = "" + SystemClock.uptimeMillis();
                String levelName = "";
                switch (logLevel) {
                    case LogLevel.NONE:
                        levelName = "CRASH";
                        header.put(KEY_LOG_LEVEL, levelName);
                        upload(tag, logMsg, levelName + "-" + time, true);
                        break;
                    case LogLevel.ERROR:
                        levelName = LogLevel.getLevelName(logLevel);
                        header.put(KEY_LOG_LEVEL, levelName);
                        upload(tag, logMsg, levelName + "-" + time, true);
                        break;
                    default:
                        levelName = LogLevel.getLevelName(logLevel);
                        header.put(KEY_LOG_LEVEL, levelName);
                        upload(tag, logMsg, levelName + "-" + time, false);
                }
            }
        };
        if (logLevel >= LogLevel.ERROR) {
            logUpDateHandler.postAtFrontOfQueue(worker);
        } else {
            logUpDateHandler.post(worker);
        }
    }

    private void upload(String tag, final String msg, String cacheKey, boolean isUpdateNow) {
        synchronized (mLogs) {
            if (isUpdateNow) {
                handleUpdate(createLog(tag, msg), cacheKey);
                return;
            }
            if (mLogs.size() < quantityInterval || isUpdating) {
                addLog(tag, msg);
                return;
            }
            int size = mLogs.size();
            Log.d(TAG, "upload log:-----------------> 日志已满，开始打包上传  size:" + size);
            try {
                List<String> temp = new ArrayList<>();
                final StringBuilder reqContent = new StringBuilder();
                for (int i = 0; i < size; i++) {
                    if (i == quantityInterval) {
                        break;
                    }
                    String log = mLogs.get(i);
                    reqContent.append(log);
                    temp.add(mLogs.get(i));
                }
                mLogs.removeAll(temp);
                Log.d(TAG, "upload log:-----------------> 日志已处理,还剩size:" + mLogs.size());
                handleUpdate(reqContent.toString(), cacheKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleUpdate(String reqContent, String cacheKey) {
        LogCache logCache = new LogCache(header, reqContent, cacheKey);
        try {
            LogCacheManager.getInstance().save(logCache, cacheKey);
        } catch (OutOfMemoryError error) {//日志过大可能搞爆内存
            XLog.e(error.getMessage());
        }
        realUpdate(header, reqContent, cacheKey);
    }


    private void addLog(String tag, String msg) {
        synchronized (mLogs) {
            long span = SystemClock.uptimeMillis() - lastAddTime;
            //  Log.d(TAG, "addLog() called with: span = [" + span + "]");
            if (span >= 1000 && span <= 1500) {
                // Log.d(TAG, "addLog() called with: addLogRate = [" + addLogCount + "]");
                if (addLogCount > 30) {//短时间内上传日志速率过高，代码可能有异常！
                    isTooFast = true;
                }
                addLogCount = 0;
            } else if (span > 1500) {
                isTooFast = false;
                addLogCount = 0;
            }

            if (isTooFast) {
                tooFastCount++;
            } else {
                tooFastCount = 0;
            }
            if (tooFastCount >= 50) {
                //TODO:代码极有可能出现问题
                String tip = " addLog() called with:添加日志过快，日志将被丢弃！";
                if (tooFastCount % 1008 == 0) {
                    XLog.e(TAG + tip);
                } else {
                    Log.e(TAG, tip);
                }
            }

            if (!isTooFast) {
                if (mLogs.size() >= 50) {//避免一直添加，撑爆内存
                    Log.e(TAG, "addLog() 添加日志达到上线，丢掉最早的一条日式");
                    mLogs.remove(0);
                }
                mLogs.add(createLog(tag, msg));
            }

            if (addLogCount == 0) {
                lastAddTime = SystemClock.uptimeMillis();
            }
            addLogCount++;
        }
    }

    private String createLog(String tag, String msg) {
        TimeZone timeZone = TimeZone.getTimeZone("Asia/Shanghai");
        // format1.setTimeZone(timeZone);
        StringBuilder reqLogItem = new StringBuilder();
        format2.setTimeZone(timeZone);
        String pTs = format2.format(getTime()) + "+0800";
        printLogReq.initBaseLogReq(tag, msg, pTs);
        reqLogItem.append(new Gson().toJson(printLogReq)).append("\n");
        return reqLogItem.toString();
    }

    private static void realUpdate(final Map<String, String> header, final String reqContent, final String cacheKey) {
        isUpdating = true;
        RequestManager.get().asyncPost(url, reqContent, header, false, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                isUpdating = false;
                Log.e(TAG, "upload log fail url:" + url + " header:" + header + "\n Exception:" + e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                isUpdating = false;
                if (!response.isSuccessful()) {
                    Log.e(TAG, String.format("upload log  onResponse. code:%d   req:%s", response.code(), response.body() == null ? "" : response.body().string()));
                } else {
                    synchronized (CloudLogPrinter.class) {
                        LogCacheManager.getInstance().clear(cacheKey);
                    }
                    Log.d(TAG, "upload log successfully !!");
                }
            }
        });
    }

    public void uploadCache() {
        logUpDateHandler.post(new Runnable() {
            @Override
            public void run() {
                List<LogCache> caches = LogCacheManager.getInstance().getLogCaches();
                if (caches.size() > 0) {
                    XLog.i(TAG + ":------------------uploadCache() called----------------------  size=" + caches.size());
                }
                for (LogCache logCache : caches) {
                    if (logCache != null) {
                        realUpdate(logCache.getHeader(), logCache.getLogContent(), logCache.getCacheKey());
                    }
                }
            }
        });
    }
}
