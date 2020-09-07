package com.vector.update_app.update;

import android.util.Log;

import com.vector.update_app.R;
import com.vector.update_app.UpdateAppBean;
import com.vector.update_app.UpdateAppManager;
import com.vector.update_app.interf.UpdateCallback;

import org.json.JSONObject;


/**
 * 新版本版本检测回调
 */
public class NormalUpdateCallbackImpl implements UpdateCallback {
    private static final String TAG = "NormalUpdateCallbackImp";

    /**
     * 解析json,自定义协议
     *
     * @param json 服务器返回的json
     * @return UpdateAppBean
     */
    @Override
    public UpdateAppBean parseJson(String json) {
        Log.d(TAG, "parseJson() called with: json = [" + json + "]");
        //System.out.println("parseJson() called with: json = [" + json + "]");
        //ignored-------如果使用服务端的这个参数，忽略后APP卸载重装就不能升级！
        UpdateAppBean updateAppBean = new UpdateAppBean();
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (0 != jsonObject.optInt("resultCode")) {
                return null;
            }
            if (json.contains("null")) {
                return null;
            }
            JSONObject data = jsonObject.getJSONObject("data");
            if (data == null) {
                return new UpdateAppBean();
            }
            updateAppBean
                    .setUpdate(true)
                    .setOriginRes(json)
                    .setNewVersion(data.optString("versionName"))
                    .setApkFileUrl(data.optString("apkUrl"))
                    .setTargetSize(data.optString("apkSize"))
                    .setUpdateLog(data.optString("description"))
                    .setNewMd5(data.optString("md5Code"))
                    .setConstraint(data.optInt("forceStatus") == 0)//0:可忽略
                    .setCanIgnoreVersion(data.optInt("negligible") == 0)
                    .setDismissNotification(true);//隐藏通知栏
            if (updateAppBean.isConstraint()) {//强制升级
                // 避免强制升级影响测试版本正常使用
//                if (BuildConfig.DEBUG) {
//                    updateAppBean.setConstraint(false);
//                }
                updateAppBean.setDialogTopBg(R.drawable.ic_update_app_top_bg_constraint);
            } else if (updateAppBean.isCanIgnoreVersion()) {
                updateAppBean.setDialogTopBg(R.drawable.ic_update_app_top_bg_can_ignore);
            } else {
                updateAppBean.setDialogTopBg(R.drawable.ic_update_app_top_bg_normal);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return updateAppBean;
    }


    /**
     * 有新版本
     *
     * @param updateAppManager app更新管理器
     */
    @Override
    public void hasNewApp(UpdateAppBean updateApp, UpdateAppManager updateAppManager) {
        updateAppManager.showDialogFragment();
    }

    /**
     * 网路请求之后
     */
    @Override
    public void onAfter() {
    }


    /**
     * 没有新版本
     *
     * @param error HttpManager实现类请求出错返回的错误消息，交给使用者自己返回，有可能不同的应用错误内容需要提示给客户
     */
    @Override
    public void noNewApp(String error) {
    }

    /**
     * 网络请求之前
     */
    @Override
    public void onBefore() {
    }

}
