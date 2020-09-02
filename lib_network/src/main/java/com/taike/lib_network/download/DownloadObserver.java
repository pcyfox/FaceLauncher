package com.taike.lib_network.download;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;


public class DownloadObserver implements Observer<DownloadInfo> {
    private DownLoadCallback callback;
    private Disposable disposable;//可以用于取消注册的监听者
    private DownloadInfo downloadInfo;
    public DownloadObserver(DownLoadCallback callback) {
        this.callback = callback;
    }
    public Disposable getDisposable() {
        return disposable;
    }

    @Override
    public void onSubscribe(Disposable d) {
        disposable = d;
        callback.onStart();
    }

    @Override
    public void onNext(DownloadInfo value) {
        this.downloadInfo = value;
        downloadInfo.setDownloadStatus(DownloadInfo.DOWNLOAD);
        callback.onProgress(downloadInfo.getProgress(), downloadInfo.getTotal());
    }

    @Override
    public void onError(Throwable e) {
        if(downloadInfo!=null){
            if (DownloadManager.getInstance().isDownCallContainsUrl(downloadInfo.getCacheKey())) {
                DownloadManager.getInstance().pauseDownload(downloadInfo.getCacheKey());
                downloadInfo.setDownloadStatus(DownloadInfo.DOWNLOAD_ERROR);
                callback.onError(e.getMessage());
            } else {
                downloadInfo.setDownloadStatus(DownloadInfo.DOWNLOAD_PAUSE);
                callback.onPause();
            }
        }else {
            callback.onError(e.getMessage());
        }
    }

    @Override
    public void onComplete() {
        if (downloadInfo != null) {
            downloadInfo.setDownloadStatus(DownloadInfo.DOWNLOAD_OVER);
            callback.onFinish(downloadInfo.getDownloadFilePath());
        }
    }
}
