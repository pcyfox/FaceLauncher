package com.vector.update_app.interf;

import java.io.File;

public interface DownloadCallback {
    /**
     * 开始
     */
    void onStart();

    /**
     * 进度
     *
     * @param progress  进度 0.00 -1.00 ，总大小
     * @param totalSize 总大小 单位B
     */
    void onProgress(float progress, long totalSize);

    /**
     * 总大小
     *
     * @param totalSize 单位B
     */
    void setMax(long totalSize);

    /**
     * 下载完了
     *
     * @param file 下载的app
     * @return true ：下载完自动跳到安装界面，false：则不进行安装
     */
    boolean onFinish(File file);

    /**
     * 下载异常
     *
     * @param msg 异常信息
     */
    void onError(String msg);

    /**
     * 当应用处于前台，准备执行安装程序时候的回调，
     *
     * @param file 当前安装包
     * @return false 默认 false ,当返回时 true 时，需要自己处理 ，前提条件是 onFinish 返回 false 。
     */
    boolean onInstallAppAndAppOnForeground(File file);
}
