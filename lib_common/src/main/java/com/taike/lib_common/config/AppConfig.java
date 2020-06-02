package com.taike.lib_common.config;

import android.text.TextUtils;

import com.taike.lib_common.BuildConfig;

public final class AppConfig {
    private AppConfig() {
    }

    private static String baseUrl;

    public static String getBaseUrl() {
        if (!TextUtils.isEmpty(baseUrl)) {
            return baseUrl;
        }
        if (BuildConfig.DEBUG) return BuildConfig.DEBUG_URL;
        return BuildConfig.BASE_URL;
    }

    public static void setBaseUrl(String baseUrl) {
        AppConfig.baseUrl = baseUrl;
    }


    public static String getDownloadBaseUrl() {
        return getBaseUrl() + "video-service/file/appDownFile";
    }

    //http://192.168.8.95/video-service/file/appDownFile?url=jpg/5efbd4bb95234eb6ada9193b1918f4c3/timg%20(1).jpg
    public static String getDownloadUrl(String path) {
        return getDownloadBaseUrl() + "?url=" + path;
    }

}
