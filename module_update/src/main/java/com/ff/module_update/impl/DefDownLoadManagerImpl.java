package com.ff.module_update.impl;

import android.util.Log;

import androidx.annotation.NonNull;

import com.taike.lib_network.download.DownLoadCallback;
import com.taike.lib_network.download.DownloadManager;
import com.vector.update_app.interf.ApkDownLoadManager;

import java.io.File;

public class DefDownLoadManagerImpl implements ApkDownLoadManager {
    private static final String TAG = "DefDownLoadManagerImpl";

    @Override
    public void startDownload(@NonNull String url, @NonNull String path, @NonNull String fileName, @NonNull final FileDownloadCallback callback) {
        Log.d(TAG, "startDownload() called with: url = [" + url + "], path = [" + path + "], fileName = [" + fileName + "], callback = [" + callback + "]");
        DownloadManager.getInstance().download(url, new DownLoadCallback() {
            @Override
            public void onStart() {
                callback.onStart();
            }

            @Override
            public void onPause() {
                callback.onPause();
            }

            @Override
            public void onProgress(float progress, long totalSize) {
                callback.onProgress(progress, totalSize);
            }

            @Override
            public void onFinish(String file) {
                Log.d(TAG, "onFinish() called with: file = [" + file + "]");
                callback.onFinish(new File(file));
            }

            @Override
            public void onError(String msg) {
                callback.onError(msg);
            }
        });

    }

    @Override
    public void pauseDownLoad(@NonNull String url) {
        DownloadManager.getInstance().pauseDownload(url);

    }

    @Override
    public void cancelDownLoad(@NonNull String url) {
        DownloadManager.getInstance().cancelDownload(url);
    }

}
