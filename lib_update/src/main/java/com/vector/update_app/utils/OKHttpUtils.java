package com.vector.update_app.utils;


import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * description ： 可直接用于网络请求的工具类
 * author : LN
 * date : 2019-10-25 11:10
 */
public class OKHttpUtils {
    private static OkHttpClient okHttpClient;
    private final static MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String TAG = "OKHttpUtils";
    public static void get(@NonNull String url, @NonNull Callback callback) {
        final OkHttpClient client = new OkHttpClient();
        try {
            Cache cache = client.cache();
            if (cache != null) {
                cache.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(callback);

    }


    public static void get(@NonNull String url, @Nullable Map<String, String> params, @Nullable Map<String, String> header, @NonNull Callback callback) {
        get(getOkHttpClient(), url, params, header, false, callback);
    }

    /**
     * 同步请求
     *
     * @param url
     * @param params
     * @param header
     * @param callback
     */
    public static void syncGet(@NonNull String url, @Nullable Map<String, String> params, @Nullable Map<String, String> header, @NonNull Callback callback) {
        get(getOkHttpClient(), url, params, header, true, callback);
    }


    public static void syncGet(@NonNull String url, @NonNull Callback callback) {
        get(getOkHttpClient(), url, null, null, true, callback);
    }


    public static void get(@NonNull OkHttpClient client, @NonNull String url, @Nullable Map<String, String> params, @Nullable Map<String, String> header, boolean sync, @NonNull Callback callback) {
        Request.Builder reqBuild = new Request.Builder();
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) {
            throw new IllegalArgumentException(" can't parse url:" + url);
        }
        HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
            }
        }
        if (header != null) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                reqBuild.addHeader(entry.getKey(), entry.getValue());
            }
        }
        reqBuild.url(urlBuilder.build());
        Request request = reqBuild.build();
        if (sync) {
            try {
                Call call = client.newCall(request);
                Response temp = call.execute();
                if (temp != null) {

                    if (temp.isSuccessful()) {
                        ResponseBody body = temp.body();
                        //call string auto close body
                        callback.onResponse(call, temp);
                    } else {
                        callback.onFailure(call, new IOException("fail"));
                    }
                } else {
                    callback.onFailure(call, new IOException("fail"));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            client.newCall(request).enqueue(callback);
        }

    }

    public static void get(@NonNull OkHttpClient client, @NonNull String url, @NonNull Callback callback) {
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(callback);
    }

    public static void syncGet(@NonNull OkHttpClient client, @NonNull String url, @NonNull Callback callback) {
        Request request = new Request.Builder().url(url).build();
        try {
            client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void post(@NonNull OkHttpClient client, @NonNull String url, Map<String, String> param, Map<String, String> header, @NonNull Callback callback) {
        Request.Builder builder = new Request.Builder().url(url);
        RequestBody body = null;
        if (param != null) {
            FormBody.Builder formBodyBuilder = new FormBody.Builder();
            for (Map.Entry<String, String> entry : param.entrySet()) {
                formBodyBuilder.add(entry.getKey(), entry.getValue());
            }
            body = formBodyBuilder.build();
        }

        if (header != null) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        if (body != null) {
            builder.post(body);
        }
        client.newCall(builder.build()).enqueue(callback);
    }


    public static void post(@NonNull String url, Map<String, String> param, Map<String, String> header, @NonNull Callback callback) {
        post(getOkHttpClient(), url, param, header, callback);
    }


    public static void post(@NonNull OkHttpClient client, @NonNull String url, @NonNull String jsonData, Map<String, String> header, @NonNull Callback callback) {
        Log.d(TAG, "post() called with: client = [" + client + "], url = [" + url + "], jsonData = [" + jsonData + "], header = [" + header + "], callback = [" + callback + "]");
        RequestBody requestBody = RequestBody.create(JSON, jsonData);
        Request.Builder builder = new Request.Builder().url(url).post(requestBody);
        if (header != null) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        client.newCall(builder.build()).enqueue(callback);
    }


    public static void post(@NonNull String url, @NonNull String jsonData, Map<String, String> header, @NonNull Callback callback) {
        post(getOkHttpClient(), url, jsonData, header, callback);
    }


    public static String getSys(String url) {
        Request request = new Request.Builder().url(url).build();
        Call call = getOkHttpClient().newCall(request);
        Response response = null;
        try {
            response = call.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (response != null) {
            if (response.isSuccessful()) {
                try {
                    return response.body().string();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }


    public static OkHttpClient getOkHttpClient(Interceptor... interceptors) {
        if (okHttpClient == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .retryOnConnectionFailure(true)//默认重试一次，若需要重试N次，则要实现拦截器。
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS);

            if (interceptors != null && interceptors.length > 0) {
                for (Interceptor interceptor : interceptors) {
                    builder.addInterceptor(interceptor);
                }
            }
            okHttpClient = builder.build();
        }
        return okHttpClient;
    }

}
