package ch.taike.launcher.update;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blankj.utilcode.util.GsonUtils;
import com.vector.update_app.interf.HttpManager;
import com.vector.update_app.utils.OKHttpUtils;

import java.io.IOException;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Response;

/**
 * Created by Vector
 * on 2017/6/19 0019.
 */

public class DefHttpManagerImpl implements HttpManager {
    /**
     * 异步get
     *
     * @param url      get请求地址
     * @param params   get参数
     * @param callBack 回调
     */
    @Override
    public void asyncGet(@NonNull String url, @NonNull Map<String, String> params, @Nullable Map<String, String> header, @Nullable final HttpCallback callBack) {
        OKHttpUtils.get(url, new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callBack.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                callBack.onResponse(response.body().string());
            }
        });
    }

    /**
     * 异步post
     *
     * @param url      post请求地址
     * @param params   post请求参数
     * @param callBack 回调
     */
    @Override
    public void asyncPost(@NonNull String url, @Nullable Map<String, String> params, @Nullable Map<String, String> header, @Nullable final HttpCallback callBack) {
        OKHttpUtils.post(url, GsonUtils.toJson(params),header, new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callBack.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                callBack.onResponse(response.body().string());
            }
        });
    }
}