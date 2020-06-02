package com.taike.lib_network;


import android.text.TextUtils;
import android.util.Log;

import com.elvishew.xlog.XLog;
import com.taike.lib_network.log.HeaderInterceptor;
import com.taike.lib_network.log.LogFilter;
import com.taike.lib_network.log.Logger;
import com.taike.lib_network.log.MyHttpLoggingInterceptor;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RequestManager extends AbsRequest {
    private static final String TAG = "RequestManager";
    private boolean isStopUploadLog = false;
    private String stopTag;
    private String cancelPrintLogTag;

    private static RequestManager requestManager = new RequestManager();
    private HeaderInterceptor headerInterceptor;

    private RequestManager() {
        super();
    }

    @Override
    OkHttpClient.Builder createOkHttpClientBuilder() {
        return new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)//默认重试一次，若需要重试N次，则要实现拦截器。
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS);
    }

    @Override
    Retrofit.Builder createRetrofitBuilder() {
        return new Retrofit.Builder()
                .baseUrl(host)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create());
    }

    public static RequestManager get() {
        return requestManager;
    }


    public void iniRetrofit(String clientId, String host) {
        Log.d(TAG, "iniRetrofit() called with: clientId = [" + clientId + "], host = [" + host + "]");
        this.host = host;
        headerInterceptor = new HeaderInterceptor();
        headerInterceptor.setDeviceId(clientId);
        loggingInterceptor.setLevel(MyHttpLoggingInterceptor.Level.BODY);
        loggingInterceptor.setCareHeaders("uid", "token", "client-id", "token");
        addInterceptor(headerInterceptor, loggingInterceptor);
        addConverterFactory(GsonConverterFactory.create());
        buildHttpClient();
    }

    public void setAuthorization(String authorization) {
        Log.d(TAG, "setAuthorization() called with: authorization = [" + authorization + "]");
        if (headerInterceptor == null) {
            return;
        }
        headerInterceptor.setAuthorization(authorization);
    }


    private MyHttpLoggingInterceptor loggingInterceptor = new MyHttpLoggingInterceptor(new Logger() {
        @Override
        public void log(String message) {
            if (cancelPrintLogTag != null && message.contains(cancelPrintLogTag)) {
                return;
            }
            boolean stop = isStopUploadLog;
            if (!TextUtils.isEmpty(stopTag)) {
                stop = message.contains(stopTag);
            }
            //将所有请求日志交给XLog处理
            if (!TextUtils.isEmpty(message) && isRecordLog) {
                if (stop) {
                    Log.d("RetrofitLog: ", message);
                } else {
                    XLog.i("XRetrofitLog: " + message);
                }
            }
        }
    }
            , new LogFilter() {//日志过滤处理
        @Override
        public String filter(String log) {
            //屏蔽密码
            if (log.startsWith("{") && log.contains("psw")) {
//                try {
//                    //TODO:如果密码未在[0-9a-zA-Z]中就会出现问题，而上面已注释的方法没这个问题
//                    String replacedLog = log.replaceAll("psw\\\"\\:\\\"[0-9a-zA-Z]*", "psw\":\"***");
//                    if (!TextUtils.isEmpty(replacedLog)) {
//                        return replacedLog;
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
            }
            return log;
        }
    });

    public boolean isStopUploadLog() {
        return isStopUploadLog;
    }

    public void setStopUploadLog(boolean stopUploadLog) {
        isStopUploadLog = stopUploadLog;
    }

    public void stopUploadLog(String tag) {
        this.stopTag = tag;
    }

    public void setCancelPrintLogTag(String cancelPrintLogTag) {
        this.cancelPrintLogTag = cancelPrintLogTag;
    }
}