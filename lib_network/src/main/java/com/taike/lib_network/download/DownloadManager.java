package com.taike.lib_network.download;


import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.elvishew.xlog.XLog;
import com.jakewharton.disklrucache.DiskLruCache;
import com.taike.lib_cache.DiskCacheManager;
import com.taike.lib_network.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
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
    private SoftReference<Map<DownloadInfo.Key, DownloadInfo>> memoryCache;
    private DiskCacheManager diskCacheManager;

    private boolean isSupportBreakpointDown;
    private DownLoadCallback callback;
    private static MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private String defStoreDir = Environment.getExternalStorageDirectory().getPath() + File.separator + "DownloadManager";
    private final boolean isDebug = BuildConfig.DEBUG;

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
        Map<DownloadInfo.Key, DownloadInfo> map = new HashMap<>();
        memoryCache = new SoftReference<>(map);
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

    public DownloadManager setSupportBreakpointDown(boolean isSupportBreakpointDown) {
        this.isSupportBreakpointDown = isSupportBreakpointDown;
        return this;
    }


    public void download(final String url, final String cacheKey, final String jsonParam, final String fileName, final String storeDir, final Map<String, String> headers, final boolean isUseCache, DownLoadCallback callback) {
        //Cache- Control:no-cache
        if (isDebug)
            Log.d(TAG, "download() called with: url = [" + url + "], cacheKey = [" + cacheKey + "], jsonParam = [" + jsonParam + "], fileName = [" + fileName + "], storeDir = [" + storeDir + "], headers = [" + headers + "], isUseCache = [" + isUseCache + "], callback = [" + callback + "]");
        this.callback = callback;
        if (TextUtils.isEmpty(url) || callback == null) {
            return;
        }

        if (isUseCache) {
            String key = TextUtils.isEmpty(cacheKey) ? url + jsonParam : cacheKey;
            DownloadInfo info = getCacheDownloadInfo(new DownloadInfo.Key(key), url);
            if (info != null && info.getDownloadStatus() == DownloadInfo.DOWNLOAD_OVER && new File(info.getDownloadFilePath()).exists()) {
                addToMemoryCache(info);
                String localPath = info.getDownloadFilePath();
                callback.onFinish(localPath);
                return;
            } else {
                XLog.i(TAG + ": not found cache");
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
                        return createDownInfo(url, cacheKey, isUseCache, jsonParam, fileName, storeDir, headers);
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
    public void download(final String url, boolean isUseCache, DownLoadCallback callback) {
        download(url, null, null, null, null, null, isUseCache, callback);
    }

    public void download(final String url, DownLoadCallback callback) {
        download(url, null, null, null, null, null, true, callback);
    }

    public void downloadWithCacheKey(final String url, String cacheKey, DownLoadCallback callback) {
        download(url, cacheKey, null, null, null, null, true, callback);
    }


    public void downloadToDir(String dUrl, String subStorePath, String cacheKey, boolean isUseCache, DownLoadCallback downloadUrlCallback) {
        download(dUrl, cacheKey, null, null, subStorePath, null, isUseCache, downloadUrlCallback);
    }

    public void downloadToDir(String dUrl, String storeDir, boolean isUseCache, DownLoadCallback downloadUrlCallback) {
        download(dUrl, null, null, null, storeDir, null, isUseCache, downloadUrlCallback);
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

    public String getDefStoreDir() {
        return defStoreDir;
    }

    /**
     * 取消下载 删除本地文件
     *
     * @param info
     */
    public void cancelDownload(DownloadInfo info) {
        pauseDownload(info.getCacheKey());
        info.setProgress(0);
        info.setDownloadStatus(DownloadInfo.DOWNLOAD_CANCEL);
    }


    public void cancelDownload(DownloadInfo.Key key) {
        if (memoryCache.get() == null) {
            return;
        }
        DownloadInfo info = memoryCache.get().get(key);
        if (info != null) {
            cancelDownload(info);
        }
    }


    public DownloadInfo getCacheDownloadInfo(DownloadInfo.Key key, String url) {
        DownloadInfo downloadInfo = null;
        if (memoryCache != null && memoryCache.get() != null) {
            downloadInfo = memoryCache.get().get(key);
            if (downloadInfo != null) {
                XLog.i(TAG + ":hunt file from ------------>memoryCache localPath:" + downloadInfo.getDownloadFilePath());
                return downloadInfo;
            }
        }

        File file = DiskCacheManager.INSTANCE().getFile(key.getKey(), DownloadInfo.getType(url));
        if (file != null && file.exists()) {
            downloadInfo = new DownloadInfo(url);
            downloadInfo.setDownloadStatus(DownloadInfo.DOWNLOAD_OVER);
            downloadInfo.setDownloadFilePath(file.getAbsolutePath());
            XLog.i(TAG + ":hunt file from ------------>diskCache localPath:" + downloadInfo.getDownloadFilePath());
        }
        return downloadInfo;
    }


    private void addToMemoryCache(DownloadInfo downloadInfo) {
        Log.d(TAG, "addToMemoryCache() called with: downloadInfo = [" + downloadInfo + "]");
        if (memoryCache != null && memoryCache.get() != null) {
            memoryCache.get().put(downloadInfo.getCacheKey(), downloadInfo);
        }
    }


    public void clearCache() {
        if (memoryCache.get() != null) {
            for (DownloadInfo info : memoryCache.get().values()) {
                if (info != null) {
                    cancelDownload(info);
                }
            }
            memoryCache.clear();
        }


        if (diskCacheManager != null) {
            diskCacheManager.clear();
        }
    }


    public void clearTask() {
        if (downCalls != null) {
            for (Call call : downCalls.values()) {
                if (call != null) {
                    call.cancel();
                }
            }
            downCalls.clear();
        }
    }

    public void clear() {
        clearCache();
        clearTask();
        callback = null;
        File cacheFile = new File(defStoreDir);
        if (cacheFile.exists()) {
            cacheFile.delete();
        }
    }


    private DownloadInfo createDownInfo(String url, String cacheKey, boolean isUseCache, String jsonParam, String name, String storePath, Map<String, String> headers) {
        String key = TextUtils.isEmpty(cacheKey) ? url + jsonParam : cacheKey;
        String rootPath = defStoreDir;
        if (!TextUtils.isEmpty(storePath) && new File(storePath).isDirectory()) {
            rootPath = defStoreDir;
        }
        DownloadInfo downloadInfo = new DownloadInfo(url);
        downloadInfo.setStoreDir(rootPath);
        downloadInfo.setHeaders(headers);
        downloadInfo.setUseCache(isUseCache);
        downloadInfo.setCacheKey(new DownloadInfo.Key(key));
        downloadInfo.setJsonParam(jsonParam);
        long contentLength = getContentLength(url);//获得文件大小
        downloadInfo.setTotal(contentLength);
        String fileName = TextUtils.isEmpty(name) ? getFileNameFormUrl(url) : name;
        downloadInfo.setFileName(fileName);

        if (memoryCache.get() == null) {
            Map<DownloadInfo.Key, DownloadInfo> map = new HashMap<>();
            memoryCache = new SoftReference<>(map);
        }
        return downloadInfo;
    }

    private String getFileNameFormUrl(String url) {
        int index = url.indexOf("?");
        if (index > 0) {
            String sub = url.substring(0, index);
            return sub.substring(url.lastIndexOf("/"));
        }
        return url;
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
        File path = new File(downloadInfo.getStoreDir());
        if (!path.exists()) {
            path.mkdir();
        }

        File file = new File(downloadInfo.getStoreDir(), fileName);
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
                File newFile = new File(downloadInfo.getStoreDir(), fileNameOther);
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
            downCalls.put(downloadInfo.getCacheKey(), call);//把这个添加到call里,方便取消
            //直接请求（未使用线程池）
            Response response = call.execute();
            String storePath = downloadInfo.getStoreDir();
            if (isDebug)
                Log.d(TAG, "executeDownload()  downloadInfo = [" + downloadInfo + "]");

            FileOutputStream fileOutputStream = null;
            OutputStream outputStream = null;
            ResponseBody body = response.body();
            if (body == null) {
                return;
            }

            InputStream is = body.byteStream();
            try {
                File file = new File(storePath, downloadInfo.getFileName());
                if (file.exists()) {
                    file.delete();
                }else {
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();// 能创建多级目录
                    }
                }

                DiskLruCache.Editor editor = null;
                if (downloadInfo.isUseCache() && DiskCacheManager.INSTANCE().isInitOk()) {
                    editor = DiskCacheManager.INSTANCE().getEditor(downloadInfo.getCacheKey().getKey());
                    editor.getEntry().setSuffix(downloadInfo.getType());
                    outputStream = editor.newOutputStream(0);
                } else {
                    fileOutputStream = new FileOutputStream(file, true);
                }

                byte[] buffer = new byte[4086 * 2];//缓冲数组4kB
                int len;

                while (!call.isCanceled() && (len = is.read(buffer)) != -1) {
                    if (outputStream != null) {
                        outputStream.write(buffer, 0, len);
                    } else {
                        if (fileOutputStream == null) {
                            return;
                        }
                        fileOutputStream.write(buffer, 0, len);
                    }
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
                if (outputStream != null) {
                    outputStream.flush();
                }
                if (editor != null) {
                    editor.commit();
                    File cacheFile = editor.getEntry().getCleanFile(0);
                    downloadInfo.setDownloadFilePath(cacheFile.getAbsolutePath());
                } else {
                    downloadInfo.setDownloadFilePath(file.getAbsolutePath());
                }

                if (fileOutputStream != null) {
                    fileOutputStream.flush();
                }
                downCalls.remove(downloadInfo.getCacheKey());
                addToMemoryCache(downloadInfo);
                if (isDebug)
                    Log.d(TAG, "executeDownload() called 下载完成: downloadInfo = [" + downloadInfo + "]");
            } catch (Exception e) {
                if (!emitter.isDisposed()) {
                    emitter.onError(e);
                }
                e.printStackTrace();
            } finally {
                //关闭IO流
                CloseUtils.close(is, fileOutputStream);
            }

            if (!emitter.isDisposed()) {
                emitter.onComplete();//完成
            }
        }
    }

    public void setDefStoreDir(String defStoreDir) {
        this.defStoreDir = defStoreDir;
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
                //  callback.onError(e.getMessage());
            }
            e.printStackTrace();
        }
        return DownloadInfo.TOTAL_ERROR;
    }

}
