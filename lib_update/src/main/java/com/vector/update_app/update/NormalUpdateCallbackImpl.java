package com.vector.update_app.update;

import android.util.Log;

import com.vector.update_app.BuildConfig;
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
        //For Test
//       if(BuildConfig.DEBUG){//忘记注释也不要慌
//           json = "\n" +
//                   "{\n" +
//                   "    \"code\":0,\n" +
//                   "    \"msg\":\"success\",\n" +
//                   "    \"data\":{\n" +
//                   "        \"is_update\":true,\n" +
//                   "        \"constraint\":false,\n" +
//                   "        \"is_can_ignore_version\":true,\n" +
//                   "        \"ignored\":false,\n" +
//                   "        \"new_version\":\"1.0\",\n" +
//                   "        \"apk_file_url\":\"http://store.ff.cn/appversion/3skccDLA0y0dqaKa.apk\",\n" +
//                   "        \"update_log\":\"111\",\n" +
//                   "        \"new_md5\":\"e2e87efb03bac18d7f95f16cdd73c6c4\"\n" +
//                   "    }\n" +
//                   "}";
//       }

        //ignored-------如果使用服务端的这个参数，忽略后APP卸载重装就不能升级！
        UpdateAppBean updateAppBean = new UpdateAppBean();
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (0 != jsonObject.optInt("resultCode")) {
                return null;
            }
            JSONObject data = jsonObject.getJSONObject("data");
            if (data == null) {
                return new UpdateAppBean();
            }
            updateAppBean
                    .setUpdate(data.optBoolean("is_update"))
                    .setOriginRes(json)
                    .setNewVersion(data.optString("new_version"))
                    .setIgnored(data.optBoolean("ignored"))
                    .setApkFileUrl(data.optString("apk_file_url"))
                    .setTargetSize(data.optString("target_size"))
                    .setUpdateLog(data.optString("update_log"))
                    .setNewMd5(data.optString("new_md5"))
                    .setCanIgnoreVersion(data.getBoolean("is_can_ignore_version"))
                    .setConstraint(data.optBoolean("constraint"))
                    .setDismissNotification(true);//隐藏通知栏;
            if (updateAppBean.isConstraint()) {//强制升级
                // 避免强制升级影响测试版本正常使用
                if (BuildConfig.DEBUG) {
                    updateAppBean.setConstraint(false);
                }
                //  updateAppBean.setDialogTopBg(R.drawable.ic_update_app_top_bg_constraint);
            } else if (updateAppBean.isCanIgnoreVersion()) {
                // updateAppBean.setDialogTopBg(R.drawable.ic_update_app_top_bg_can_ignore);
            } else {
                // updateAppBean.setDialogTopBg(R.drawable.ic_update_app_top_bg_normal);
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
