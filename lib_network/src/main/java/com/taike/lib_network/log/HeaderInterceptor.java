package com.taike.lib_network.log;

import android.text.TextUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class HeaderInterceptor implements Interceptor {
    private Map<String, String> mHeadMap;

    public HeaderInterceptor() {
        mHeadMap = new HashMap<>();
        mHeadMap.put("Content-Type", "application/json;charset=UTF-8");
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request.Builder builder = chain.request().newBuilder();
        for (String key : mHeadMap.keySet()) {
            String value = mHeadMap.get(key);
            if (!TextUtils.isEmpty(value)) {
                builder.addHeader(key, value);
            }
        }
        return chain.proceed(builder.build());
    }

    public void setDeviceId(String clientId) {
        mHeadMap.put("device-id", clientId);
    }

    public void setUidToken(String uid, String token) {
        mHeadMap.put("uid", uid);
        mHeadMap.put("token", token);
    }

    public void setAuthorization(String authorization) {
        mHeadMap.put("authorization", authorization);
    }

    public void addHeader(String key, String value) {
        mHeadMap.put(key, value);
    }

}
