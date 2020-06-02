package com.taike.lib_network.download;


import java.util.Map;

public class DownloadInfo {

    /**
     * 下载状态
     */
    public static final int DOWNLOAD = 0;    // 下载中
    public static final int DOWNLOAD_PAUSE = 1; // 下载暂停
    public static final int DOWNLOAD_WAIT = 2;  // 等待下载
    public static final int DOWNLOAD_CANCEL = 3; // 下载取消
    public static final int DOWNLOAD_OVER = 4;    // 下载结束
    public static final int DOWNLOAD_ERROR = -10;  // 下载出错
    public static final long TOTAL_ERROR = -1;//获取进度失败
    private Map<String, String> headers; //请求头
    private String url;
    private String fileName;
    private String downloadFilePath;//下载后文件的保存路径
    private int downloadStatus;
    private long total;
    private long progress;
    //通过post方式请下载，参数是json时使用
    private String jsonParam = "";


    public DownloadInfo(String url) {
        this.url = url;
    }

    public DownloadInfo(String url, int downloadStatus) {
        this.url = url;
        this.downloadStatus = downloadStatus;
    }

    public Key getKey() {
        return new Key(url + jsonParam);
    }


    public String getJsonParam() {
        return jsonParam;
    }

    public void setJsonParam(String jsonParam) {
        this.jsonParam = jsonParam;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getUrl() {
        return url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getProgress() {
        return progress;
    }

    public void setProgress(long progress) {
        this.progress = progress;
    }

    public int getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(int downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDownloadFilePath() {
        if (DownloadInfo.DOWNLOAD_OVER != downloadStatus) {
            throw new IllegalStateException("DOWNLOAD_OVER ?");
        }
        return downloadFilePath;
    }

    public void setDownloadFilePath(String downloadFilePath) {
        this.downloadFilePath = downloadFilePath;
    }

    @Override
    public String toString() {
        return "DownloadInfo{" +
                "url='" + url + '\'' +
                ", fileName='" + fileName + '\'' +
                ", downloadFilePath='" + downloadFilePath + '\'' +
                ", downloadStatus='" + downloadStatus + '\'' +
                ", total=" + total +
                ", progress=" + progress +
                '}';
    }

    public static class Key {
        private String key;

        public Key(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }


}
