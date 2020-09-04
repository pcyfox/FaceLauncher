package com.vector.update_app.interf;


import androidx.annotation.NonNull;

import java.io.File;
import java.io.Serializable;

/**
 * 定义下载管理相关接口
 */
public interface ApkDownLoadManager extends Serializable {

    /**
     * 下载
     *
     * @param url      下载地址
     * @param path     文件保存路径
     * @param fileName 文件名称
     * @param callback 回调
     */
    void startDownload(@NonNull String url, @NonNull String path, @NonNull String fileName, @NonNull FileDownloadCallback callback);

    void pauseDownLoad(@NonNull String url);

    void cancelDownLoad(@NonNull String url);


    /**
     * 下载回调
     */
    interface FileDownloadCallback {
        void onProgress(float progress, long total);

        void onError(String error);

        void  onFinish(File file,boolean isNeedInstall);

        void onStart();

        void onPause();

        void onCanceled();
    }
}
