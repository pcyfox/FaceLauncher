package com.taike.lib_network;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.TimeUnit;



import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
/**
 * description ： 可直接用于网络请求的工具类
 * author : LN
 * date : 2019-10-25 11:10
 */
public class OKHttpUtils {
    private static OkHttpClient okHttpClient;
    private final static MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static void get(@NonNull String url, @NonNull Callback callback) {
        final OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(callback);
    }


    public static void get(@NonNull String url, @Nullable Map<String, String> params, @Nullable Map<String, String> header, @NonNull Callback callback) {
        get(getOkHttpClient(), url, params, header, callback);
    }


    public static void get(@NonNull OkHttpClient client, @NonNull String url, @Nullable Map<String, String> params, @Nullable Map<String, String> header, @NonNull Callback callback) {
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
        client.newCall(request).enqueue(callback);
    }

    public static void get(@NonNull OkHttpClient client, @NonNull String url, @NonNull Callback callback) {
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(callback);
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


    public static OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .retryOnConnectionFailure(true)//默认重试一次，若需要重试N次，则要实现拦截器。
                    .connectTimeout(12, TimeUnit.SECONDS)
                    .readTimeout(18, TimeUnit.SECONDS)
                    .writeTimeout(18, TimeUnit.SECONDS)
                    .build();
        }
        return okHttpClient;
    }

}
