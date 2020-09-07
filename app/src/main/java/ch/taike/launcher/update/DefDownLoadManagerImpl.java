package ch.taike.launcher.update;

import android.util.Log;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.elvishew.xlog.XLog;
import com.taike.lib_common.config.AppConfig;
import com.taike.lib_network.download.DownLoadCallback;
import com.taike.lib_network.download.DownloadInfo;
import com.taike.lib_network.download.DownloadManager;
import com.vector.update_app.interf.ApkDownLoadManager;
import com.vector.update_app.utils.OKHttpUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import ch.taike.launcher.RootUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DefDownLoadManagerImpl implements ApkDownLoadManager {
    private static final String TAG = "DefDownLoadManagerImpl";

    @Override
    public void startDownload(@NonNull String url, @NonNull String path, @NonNull String fileName, @NonNull final FileDownloadCallback callback) {
        Log.d(TAG, "startDownload() called with: url = [" + url + "], path = [" + path + "], fileName = [" + fileName + "], callback = [" + callback + "]");
        url = AppConfig.getDownloadUrl(url);
        OKHttpUtils.get(url, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                ResponseBody body = response.body();
                if (body == null) {
                    return;
                }
                String json = body.string();
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    if (0 != jsonObject.optInt("resultCode")) {
                        return;
                    }
                    String appUrl = jsonObject.getString("data");
                    DownloadManager.getInstance().download(appUrl, new DownLoadCallback() {
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
                            XLog.i(TAG + ":onFinish() called with: file = [" + file + "]");
                            if (!AppUtils.isAppRoot()) {
                                XLog.w(TAG + ":onFinish() called isAppRoot=false");
                                callback.onFinish(new File(file), true);
                                return;
                            }
                            RootUtils.installAPK(file, data -> {
                                if (data.result == 0) {
                                    ToastUtils.showShort("安装成功！");
                                    callback.onFinish(new File(file), false);
                                    XLog.i(TAG + "onFinish() called shell install apk fail! error:"+data.successMsg);
                                } else {
                                    XLog.i(TAG + "onFinish() called shell install apk fail! error:"+data.errorMsg);
                                    callback.onFinish(new File(file), true);
                                }
                            });
                        }

                        @Override
                        public void onError(String msg) {
                            callback.onError(msg);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


    }

    @Override
    public void pauseDownLoad(@NonNull String url) {
        Log.d(TAG, "pauseDownLoad() called with: url = [" + url + "]");
        DownloadManager.getInstance().pauseDownload(new DownloadInfo.Key(url));
    }

    @Override
    public void cancelDownLoad(@NonNull String url) {
        Log.d(TAG, "cancelDownLoad() called with: url = [" + url + "]");
        DownloadManager.getInstance().cancelDownload(new DownloadInfo.Key(url));
    }

}
