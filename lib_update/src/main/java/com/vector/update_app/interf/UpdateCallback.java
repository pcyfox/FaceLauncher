package com.vector.update_app.interf;


import com.vector.update_app.UpdateAppBean;
import com.vector.update_app.UpdateAppManager;

/**
 * 新版本版本检测回调
 */
public interface UpdateCallback {

    /**
     * 解析json,自定义协议
     *
     * @param json 服务器返回的json
     * @return UpdateAppBean
     */
    UpdateAppBean parseJson(String json);

    /**
     * 有新版本
     *
     * @param updateAppManager app更新管理器
     */
    void hasNewApp(UpdateAppBean updateApp, UpdateAppManager updateAppManager);

    /**
     * 网路请求之后
     */
    void onAfter();


    /**
     * 没有新版本
     *
     * @param error HttpManager实现类请求出错返回的错误消息，交给使用者自己返回，有可能不同的应用错误内容需要提示给客户
     */
    void noNewApp(String error);

    /**
     * 网络请求之前
     */
    void onBefore();

}
