package com.taike.lib_log.printer;

import android.text.TextUtils;
import android.util.Log;

import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.printer.Printer;
import com.google.gson.Gson;
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
    private List<String> mLogs = new ArrayList<>();
    private BasePrintLogReq printLogReq;//必需是个JavaBean
    private String index;
    private String type = "log";
    private int quantityInterval = 30;//上传数量间隔,默认是没满30条就上传
    private String url = "http://103.24.177.9:9200/_bulk";

    private SimpleDateFormat format1 = new SimpleDateFormat("yyyy.MM.dd", Locale.CHINA);
    private SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.CHINA);

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getQuantityInterval() {
        return quantityInterval;
    }

    public void setQuantityInterval(int quantityInterval) {
        this.quantityInterval = quantityInterval;
    }

    /**
     * @param printLogReq 自定义打印类对象
     */
    public CloudLogPrinter(BasePrintLogReq printLogReq/*必需是一个JavaBean*/, String index) {
        this.printLogReq = printLogReq;
        this.index = index;
    }

    public BasePrintLogReq getPrintLogReq() {
        return printLogReq;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }


    private long getTime() {
        long timeDifference = 0;
        return System.currentTimeMillis() + timeDifference;
    }


    @Override
    public void println(int logLevel, String tag, final String msg) {
        if (logLevel <= LogLevel.DEBUG || TextUtils.isEmpty(msg)) {
            return;
        }
        //  upload(tag,msg);
    }

    private void upload(String tag, final String msg) {
        if (mLogs.size() < quantityInterval - 1) {
            TimeZone timeZone = TimeZone.getTimeZone("Asia/Shanghai");
            format1.setTimeZone(timeZone);
            StringBuilder reqLogItem = new StringBuilder("{ \"index\" : { \"_index\" : \"" + index + "-" + format1.format(getTime())
                    + "\", \"_type\" : \"" + type + "\"} }\n");
            format2.setTimeZone(timeZone);
            String pTs = format2.format(getTime()) + "+0800";
            printLogReq.initBaseLogReq(tag, msg, pTs);
            reqLogItem.append(new Gson().toJson(printLogReq)).append("\n");
            mLogs.add(reqLogItem.toString());
            return;
        }
        // Log.d("xlog", "checkUpdate success");
        final StringBuilder _reqContent = new StringBuilder();
        try {
            for (int i = mLogs.size() - 1; i > -1; i--) {
                _reqContent.append(mLogs.get(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        mLogs.clear();
        Map<String, String> header = new HashMap<>();
        header.put("timeStamp", "" + getTime());
        String reqContent = _reqContent.toString();

        RequestManager.get().asyncPost(url, reqContent, header, false, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("xlog", "cloud log upload fail" + e.toString() + " log size :" + mLogs.size());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("xlog", String.format("cloud log print onResponse. code:%d  %s  req:%s", response.code(), _reqContent, response.body() == null ? "" : response.body().string()));
                } else {
                    Log.d("xlog", "upload log successfully---------------------------------->");
                }
            }
        });
    }
}
