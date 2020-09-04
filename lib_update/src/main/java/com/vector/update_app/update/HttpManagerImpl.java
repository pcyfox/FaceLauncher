package com.vector.update_app.update;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vector.update_app.interf.HttpManager;
import com.vector.update_app.utils.OKHttpUtils;

import java.io.IOException;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HttpManagerImpl implements HttpManager {


    @Override
    public void asyncGet(@NonNull String url, @Nullable Map<String, String> params, @Nullable Map<String, String> header, @Nullable final HttpCallback callBack) {
        OKHttpUtils.get(url, params, header, new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callBack.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (callBack == null){
                    return;
                }
                ResponseBody body = response.body();
                if (body == null) {
                    callBack.onResponse(null);
                }else{
                    callBack.onResponse(body.string());
                }
            }
        });

    }

    @Override
    public void asyncPost(@NonNull String url, @NonNull Map<String, String> params,@Nullable Map<String, String> header, @Nullable final HttpCallback callBack) {
        OKHttpUtils.post(url, params, header, new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callBack.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (callBack == null){
                    return;
                }
                ResponseBody body = response.body();
                if (body == null) {
                    callBack.onResponse(null);
                } else {
                    callBack.onResponse(body.string());
                }

            }
        });
    }
}
