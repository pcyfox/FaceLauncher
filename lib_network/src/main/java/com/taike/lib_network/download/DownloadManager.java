package com.taike.lib_network.download;


import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.elvishew.xlog.XLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 下载管理
 */
public class DownloadManager {
    private static final String TAG = "DownloadManager";
    private static final AtomicReference<DownloadManager> INSTANCE = new AtomicReference<>();
    private OkHttpClient okHttpClient;
    private HashMap<DownloadInfo.Key, Call> downCalls; //用来存放各个下载的请求
    private WeakHashMap<DownloadInfo.Key, DownloadInfo> downloadInfoMap = new WeakHashMap<>();
    private String storePath;
    private boolean isSupportBreakpointDown;
    private boolean isUseCache;
    private DownLoadCallback callback;
    private static MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static DownloadManager getInstance() {
        for (; ; ) {
            DownloadManager current = INSTANCE.get();
            if (current != null) {
                return current;
            }
            current = new DownloadManager();
            if (INSTANCE.compareAndSet(null, current)) {
                return current;
            }
        }
    }

    private DownloadManager() {
        downCalls = new HashMap<>();
        okHttpClient = new OkHttpClient.Builder().build();
        storePath = Environment.getExternalStorageDirectory().getPath() + File.separator + "DownloadManager";
    }


    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public void setOkHttpClient(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    /**
     * 查看是否在下载任务中
     *
     * @param key
     * @return
     */
    public boolean isDownCallContainsUrl(DownloadInfo.Key key) {
        return downCalls.containsKey(key);
    }

    public String getStorePath() {
        return storePath;
    }

    public DownloadManager setStorePath(String storePath) {
        this.storePath = storePath;
        return this;
    }

    public DownloadManager setSupportBreakpointDown(boolean isSupportBreakpointDown) {
        this.isSupportBreakpointDown = isSupportBreakpointDown;
        return this;
    }


    public DownloadManager setUseCache(boolean useCache) {
        isUseCache = useCache;
        return this;
    }


    public void download(final String url, final String jsonParam, final String name, final Map<String, String> headers, DownLoadCallback callback) {
        XLog.d(TAG + ":  download() called with: url = [" + url + "], jsonParam = [" + jsonParam + "], headers = [" + headers + "], callback = [" + callback + "]");
        this.callback = callback;
        if (TextUtils.isEmpty(url) || callback == null) {
            return;
        }
        if (isUseCache) {
            DownloadInfo info = getDownloadInfo(new DownloadInfo.Key(url));
            if (info != null && info.getDownloadStatus() == DownloadInfo.DOWNLOAD_OVER && new File(info.getDownloadFilePath()).exists()) {
                XLog.d(TAG + ":hunt file from cache");
                callback.onFinish(info.getDownloadFilePath());
                return;
            }
        }

        Observable.just(url).filter(new Predicate<String>() { // 过滤 call的map中已经有了,就证明正在下载,则这次不下载
            @Override
            public boolean test(String s) {
                return !downCalls.containsKey(new DownloadInfo.Key(url));
            }
        })
                .map(new Function<String, DownloadInfo>() { // 生成 DownloadInfo
                    @Override
                    public DownloadInfo apply(String url) {
                        return createDownInfo(url, jsonParam, name, headers);
                    }
                })
                .map(new Function<DownloadInfo, DownloadInfo>() { // 如果已经下载，重新命名
                    @Override
                    public DownloadInfo apply(DownloadInfo downloadInfo) {
                        return getRealFileName(downloadInfo);
                    }
                })
                .flatMap(new Function<DownloadInfo, ObservableSource<DownloadInfo>>() { // 下载
                    @Override
                    public ObservableSource<DownloadInfo> apply(DownloadInfo downloadInfo) {
                        return Observable.create(new DownloadSubscribe(downloadInfo));
                    }
                })
                .observeOn(AndroidSchedulers.mainThread()) // 事件回调的线程
                .subscribeOn(Schedulers.io()) //事件执行的线程
                .subscribe(new DownloadObserver(callback)); //  添加观察者，监听下载进度

    }


    /**
     * 开始下载
     *
     * @param url 下载请求的网址
     */
    public void download(final String url, DownLoadCallback callback) {
        download(url, null, null, null, callback);
    }

    public void download(String url, String storePath, DownLoadCallback callback) {
        this.storePath = storePath;
        download(url, callback);
    }

    /**
     * 下载取消或者暂停
     *
     * @param key
     */
    public void pauseDownload(DownloadInfo.Key key) {
        Call call = downCalls.get(key);
        if (call != null) {
            call.cancel();//取消
        }
        downCalls.remove(key);
    }

    /**
     * 取消下载 删除本地文件
     *
     * @param info
     */
    public void cancelDownload(DownloadInfo info) {
        pauseDownload(info.getKey());
        info.setProgress(0);
        info.setDownloadStatus(DownloadInfo.DOWNLOAD_CANCEL);
    }


    public void cancelDownload(DownloadInfo.Key key) {
        DownloadInfo info = downloadInfoMap.get(key);
        if (info != null) {
            cancelDownload(info);
        }
    }


    public DownloadInfo getDownloadInfo(DownloadInfo.Key key) {
        return downloadInfoMap.get(key);
    }

    public int getDownloadStatus(DownloadInfo.Key key) {
        DownloadInfo info = getDownloadInfo(key);
        if (info == null) {
            return -200;
        }
        return info.getDownloadStatus();
    }


    public void clear() {
        for (DownloadInfo info : downloadInfoMap.values()) {
            if (info != null) {
                cancelDownload(info);
            }
        }
        downloadInfoMap.clear();

        if (downCalls != null) {
            for (Call call : downCalls.values()) {
                if (call != null) {
                    call.cancel();
                }
            }
            downCalls.clear();
        }
    }

    public void clearCache() {
        clear();
        File cacheFile = new File(storePath);
        if (cacheFile.exists()) {
            cacheFile.delete();
        }
    }

    /**
     * 创建DownInfo
     *
     * @param url 请求网址
     * @return DownInfo
     */
    private DownloadInfo createDownInfo(String url) {
        return createDownInfo(url, "", null, null);
    }

    private DownloadInfo createDownInfo(String url, String jsonParam, String name, Map<String, String> headers) {
        DownloadInfo downloadInfo = new DownloadInfo(url);
        downloadInfo.setHeaders(headers);
        downloadInfo.setJsonParam(jsonParam);
        downloadInfoMap.put(downloadInfo.getKey(), downloadInfo);
        long contentLength = getContentLength(url);//获得文件大小
        downloadInfo.setTotal(contentLength);
        String fileName = TextUtils.isEmpty(name) ? url.substring(url.lastIndexOf("/")) : name;
        downloadInfo.setFileName(fileName);
        return downloadInfo;
    }

    /**
     * 如果文件已下载重新命名新文件名
     *
     * @param downloadInfo
     * @return
     */
    private DownloadInfo getRealFileName(DownloadInfo downloadInfo) {
        String fileName = downloadInfo.getFileName();
        long downloadLength = 0;
        long contentLength = downloadInfo.getTotal();
        File path = new File(storePath);
        if (!path.exists()) {
            path.mkdir();
        }

        File file = new File(storePath, fileName);
        if (isSupportBreakpointDown) {
            if (file.exists()) {
                //找到了文件,代表已经下载过（但不见得下载全）,则获取其长度
                downloadLength = file.length();
            }
            //之前下载过,需要重新来一个文件
            int i = 1;
            while (downloadLength >= contentLength) {
                int dotIndex = fileName.lastIndexOf(".");
                String fileNameOther;
                if (dotIndex == -1) {
                    fileNameOther = fileName + "(" + i + ")";
                } else {
                    fileNameOther = fileName.substring(0, dotIndex)
                            + "(" + i + ")" + fileName.substring(dotIndex);
                }
                File newFile = new File(storePath, fileNameOther);
                file = newFile;
                downloadLength = newFile.length();
                i++;
            }
        } else {
            if (file.exists()) {
                file.delete();
            }
        }
        //设置改变过的文件名/大小
        downloadInfo.setProgress(downloadLength);
        downloadInfo.setFileName(file.getName());
        return downloadInfo;
    }

    private class DownloadSubscribe implements ObservableOnSubscribe<DownloadInfo> {
        private DownloadInfo downloadInfo;

        public DownloadSubscribe(DownloadInfo downloadInfo) {
            this.downloadInfo = downloadInfo;
        }

        @Override
        public void subscribe(ObservableEmitter<DownloadInfo> emitter) throws Exception {
            executeDownload(emitter);
        }


        private void executeDownload(ObservableEmitter<DownloadInfo> emitter) throws IOException {
            String url = downloadInfo.getUrl();
            long downloadLength = downloadInfo.getProgress();//已经下载好的长度
            long contentLength = downloadInfo.getTotal();//文件的总长度
            //初始进度信息
            emitter.onNext(downloadInfo);

            Request.Builder builder = new Request.Builder()
                    //确定下载的范围,添加此头,则服务器就可以跳过已经下载好的部分
                    .addHeader("RANGE", "bytes=" + downloadLength + "-" + contentLength)
                    .url(url);
            Map<String, String> headers = downloadInfo.getHeaders();
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    builder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            //以post-JSON方法下载
            if (!TextUtils.isEmpty(downloadInfo.getJsonParam())) {
                RequestBody requestBody = RequestBody.create(JSON, downloadInfo.getJsonParam());
                builder.post(requestBody);
            }

            Request request = builder.build();
            Call call = okHttpClient.newCall(request);
            downCalls.put(downloadInfo.getKey(), call);//把这个添加到call里,方便取消
            //直接请求（未使用线程池）
            Response response = call.execute();
            storePath = TextUtils.isEmpty(storePath) ? Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() : storePath;
            File file = new File(storePath, downloadInfo.getFileName());
            FileOutputStream fileOutputStream = null;
            ResponseBody body = response.body();
            if (body == null) {
                return;
            }

            InputStream is = body.byteStream();
            try {
                fileOutputStream = new FileOutputStream(file, true);
                byte[] buffer = new byte[4086];//缓冲数组4kB
                int len;
                while (!call.isCanceled() && (len = is.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, len);
                    downloadLength += len;
                    downloadInfo.setProgress(downloadLength);
                    if (!emitter.isDisposed()) {
                        if (call.isCanceled()) {
                            emitter.onError(new Throwable("call is canceled"));
                        } else {
                            emitter.onNext(downloadInfo);
                        }
                    }
                }
                fileOutputStream.flush();
                downCalls.remove(downloadInfo.getKey());
                downloadInfo.setDownloadFilePath(file.getAbsolutePath());

            } finally {
                //关闭IO流
                CloseUtils.close(is, fileOutputStream);
            }
            if (!emitter.isDisposed()) {
                emitter.onComplete();//完成
            }
        }
    }


    /**
     * 获取下载长度
     *
     * @param downloadUrl
     * @return
     */
    private long getContentLength(String downloadUrl) {
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        try {
            if (okHttpClient == null) {
                throw new IllegalArgumentException("okHttpClient is null");
            }
            Response response = okHttpClient.newCall(request).execute();
            ResponseBody body = response.body();
            if (body != null && response.isSuccessful()) {
                long contentLength = body.contentLength();
                response.close();
                return contentLength == 0 ? DownloadInfo.TOTAL_ERROR : contentLength;
            }
        } catch (IOException e) {
            if (callback != null) {
                callback.onError(e.getMessage());
            }
            e.printStackTrace();
        }
        return DownloadInfo.TOTAL_ERROR;
    }

}
