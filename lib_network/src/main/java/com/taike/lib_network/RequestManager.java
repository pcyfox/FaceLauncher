package com.taike.lib_network;


import android.text.TextUtils;
import android.util.Log;

import com.elvishew.xlog.XLog;
import com.taike.lib_cache.DiskTools;
import com.taike.lib_network.log.Filter;
import com.taike.lib_network.log.HeaderInterceptor;
import com.taike.lib_network.log.HttpLogger;
import com.taike.lib_network.log.LogFilter;
import com.taike.lib_network.log.MyHttpLoggingInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RequestManager extends AbsRequest {
    private static final String TAG = "RequestManager";
    private static RequestManager requestManager = new RequestManager();
    private HeaderInterceptor headerInterceptor;

    private RequestManager() {
        super();
    }

    @Override
    OkHttpClient.Builder createOkHttpClientBuilder() {
        return new OkHttpClient().newBuilder()
                .retryOnConnectionFailure(false)//默认重试一次，若需要重试N次，则要实现拦截器。
                .dns(new OkHttpDns(3L))
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    Retrofit.Builder createRetrofitBuilder() {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create());
    }

    public static RequestManager get() {
        return requestManager;
    }


    public void iniRetrofit(String clientId, String baseUrl, String appVersionCode, String appVersionName, String pkgName) {
        XLog.i(TAG + ":iniRetrofit() called with: clientId = [" + clientId + "], baseUrl = [" + baseUrl + "], appVersionCode = [" + appVersionCode + "], pkgName = [" + pkgName + "]");
        this.baseUrl = baseUrl;
        headerInterceptor = new HeaderInterceptor();
        headerInterceptor.setDeviceId(clientId);
        headerInterceptor.setAppVersionCode(appVersionCode);
        headerInterceptor.setPkgName(pkgName);
        headerInterceptor.setAppVersionName(appVersionName);
        loggingInterceptor.setLevel(MyHttpLoggingInterceptor.Level.BODY);
        loggingInterceptor.setCareHeaders("uid", "token", "device-id", "token", "authorization");
        cleatInterceptor();
        addInterceptor(loggingInterceptor, headerInterceptor);
        cleaConverterFactories();
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

    public void setUidToken(String uid) {
        headerInterceptor.setUid(uid);
    }


    public Map<String, String> getHeader() {
        if (headerInterceptor == null) {
            return null;
        }
        return headerInterceptor.getHeadMap();
    }

    /**
     * 设置请求头过滤器
     *
     * @param filter
     */
    public void setHeaderInterceptorFilter(Filter filter) {
        if (headerInterceptor != null) {
            headerInterceptor.setFilter(filter);
        }
    }


    public void setHttpLoggingInterceptor(LogFilter filter) {
        if (loggingInterceptor != null) {
            loggingInterceptor.setFilter(filter);
        }
    }

    private MyHttpLoggingInterceptor loggingInterceptor = new MyHttpLoggingInterceptor(new HttpLogger() {
        @Override
        public void log(String url, String message) {
            //将所有请求日志交给XLog处理
            if (!TextUtils.isEmpty(message)) {
                try {
                    if (checkForUpload(url)) {
                        XLog.i("XRetrofitLog: " + message);
                    } else {
                        Log.d("RetrofitLog: ", message);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
            , new LogFilter() {//日志过滤处理
        @Override
        public String filter(String url, String log) {
            return log;
        }
    });

    private boolean checkForUpload(String url) {
        String matchUrl = null;
        synchronized (lock) {
            for (String key : uploadLogRequests.keySet()) {
                if (url.contains(key)) {
                    matchUrl = key;
                    break;
                }
            }
            if (matchUrl != null) {
                synchronized (lock) {
                    Boolean isNeedUpdate = uploadLogRequests.get(matchUrl);
                    return isNeedUpdate != null && isNeedUpdate;
                }
            }
        }
        return true;
    }
}