package com.taike.lib_common.config;

import android.text.TextUtils;

import com.taike.lib_common.manager.PathManager;

public final class AppConfig {
    private AppConfig() {
    }

    private static String baseUrl;
    public static final String NET_CAMERA_ACCOUNT = "admin";
    public static final String NET_CAMERA_PSD = "123456";
    public static final int NET_CAMERA_PORT = 8091;
    public static final String KEY_BASE_URL = "KEY_BASE_URL";


    public static String getNetCameraRTSPSubUrl(String host) {
        if (TextUtils.isEmpty(host)) {
            return "";
        }
        return "rtsp://" + NET_CAMERA_ACCOUNT + ":" + NET_CAMERA_PSD + "@" + host + "/mpeg4cif";
    }

    public static String getNetCameraRTSPMainUrl(String host) {
        if (TextUtils.isEmpty(host)) {
            return "";
        }
        return "rtsp://" + NET_CAMERA_ACCOUNT + ":" + NET_CAMERA_PSD + "@" + host + "/mpeg4";
    }

    /**
     * 下载文件统一存放地
     *
     * @return
     */
    public static String getDownloadStorePath() {
        return PathManager.get().getDownloadPath();
    }

    public static String getCachePath() {
        return PathManager.get().getCachePath();
    }

    public static String getDataRootPath() {
        return PathManager.get().getDataRootPath();
    }

    public static String getLogPath() {
        return PathManager.get().getLogPath();
    }

}
