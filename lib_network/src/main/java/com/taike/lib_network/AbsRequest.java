package com.taike.lib_network;


import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * description ： Retrofit网络请求封装基类
 * author : LN
 * date : 2019-10-25 11:10
 */
public abstract class AbsRequest {
    private Set<Converter.Factory> converterFactories;
    private Set<Interceptor> interceptors;
    String host;
    private OkHttpClient httpClient;
    private Retrofit retrofit;
    boolean isRecordLog = true;//默认记录每一条请求记录

    abstract OkHttpClient.Builder createOkHttpClientBuilder();

    abstract Retrofit.Builder createRetrofitBuilder();

    AbsRequest() {
        interceptors = new HashSet<>();
        converterFactories = new HashSet<>();
    }

     OkHttpClient buildHttpClient() {
        if (httpClient == null) {
            OkHttpClient.Builder builder = createOkHttpClientBuilder();
            for (Interceptor interceptor : interceptors) {
                builder.addInterceptor(interceptor);
            }
            httpClient = builder.build();
        }
        return httpClient;
    }

    private Retrofit buildRetrofit() {
        Retrofit.Builder builder = createRetrofitBuilder();
        for (Converter.Factory factory : converterFactories) {
            builder.addConverterFactory(factory);
        }
        builder.client(buildHttpClient());
        return builder.build();
    }


    public <T> T create(final Class<T> service) {
        if (retrofit == null) {
            retrofit = buildRetrofit();
        }
        isRecordLog = true;
        return retrofit.create(service);
    }

    public OkHttpClient getHttpClient() {
        return httpClient;
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }

    public Set<Interceptor> getInterceptors() {
        return interceptors;
    }

    public void addConverterFactory(Converter.Factory... factories) {
        converterFactories.addAll(Arrays.asList(factories));
    }

    public void addInterceptor(Interceptor... interceptors) {
        this.interceptors.addAll(Arrays.asList(interceptors));
    }

    public void asyncGet(@NonNull String url, Callback callback, boolean isRecordLog) {
        this.isRecordLog = isRecordLog;

        OKHttpUtils.get(buildHttpClient(), url, callback);
    }

    public void asyncGet(@NonNull String url, Callback callback) {
        asyncGet(url, callback, isRecordLog);
    }

    public void asyncGet(@NonNull String url, Map<String, String> params, Map<String, String> header, boolean isRecordLog, Callback callback) {
        this.isRecordLog = isRecordLog;
        OKHttpUtils.get(buildHttpClient(), url, params, header, callback);
    }

    public void asyncGet(@NonNull String url, Map<String, String> params, Map<String, String> header, Callback callback) {
        asyncGet(url, params, header, isRecordLog, callback);
    }


    public void asyncPost(@NonNull String url, Map<String, String> params, Map<String, String> header, boolean isRecordLog, Callback callback) {
        this.isRecordLog = isRecordLog;
        OKHttpUtils.post(buildHttpClient(), url, params, header, callback);
    }

    public void asyncPost(@NonNull String url, Map<String, String> params, Map<String, String> header, Callback callback) {
        asyncPost(url, params, header, isRecordLog, callback);
    }


    public void asyncPost(@NonNull String url, String jsonContent, Map<String, String> header, boolean isRecordLog, Callback callback) {
        this.isRecordLog = isRecordLog;
        OKHttpUtils.post(buildHttpClient(), url, jsonContent, header, callback);
    }

    public void asyncPost(@NonNull String url, String jsonContent, Map<String, String> header, Callback callback) {
        asyncPost(url, jsonContent, header, isRecordLog, callback);
    }

}
