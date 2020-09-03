package com.vector.update_app.interf;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.Map;

/**
 * http请求接口
 */
public interface HttpManager extends Serializable {
    /**
     * 异步get
     *
     * @param url      get请求地址
     * @param params   get参数
     * @param callBack 回调
     */
    void asyncGet(@NonNull String url, @Nullable Map<String, String> params, @Nullable Map<String, String> header, @Nullable HttpCallback callBack);


    /**
     * 异步post
     *
     * @param url      post请求地址
     * @param params   post请求参数
     * @param callBack 回调
     */
    void asyncPost(@NonNull String url, @Nullable Map<String, String> params, @Nullable Map<String, String> header, @Nullable HttpCallback callBack);


    /**
     * 网络请求回调
     */
    interface HttpCallback {
        /**
         * 结果回调
         *
         * @param result 结果
         */
        void onResponse(String result);

        /**
         * 错误回调
         *
         * @param error 错误提示
         */
        void onError(String error);
    }
}
