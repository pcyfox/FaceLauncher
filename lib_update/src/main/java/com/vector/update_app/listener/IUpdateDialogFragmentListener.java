package com.vector.update_app.listener;

import com.vector.update_app.UpdateAppBean;

/**
 * version 1.0
 * Created by jiiiiiin on 2018/4/1.
 */

public interface IUpdateDialogFragmentListener {


    void onUpdateNotifyDialogOnStart(UpdateAppBean updateApp);

    /**
     * 当默认的更新提示框被用户点击取消的时候调用
     *
     * @param updateApp updateApp
     */
    void onUpdateNotifyDialogCancel(UpdateAppBean updateApp);

    /**
     * 当默认的更新提示框被用户选择忽略
     *
     * @param updateApp updateApp
     * @param state   public static final int STATE_NORMAL = 0;//普通状态
     *
     *     public static final int STATE_CONSTRAINT = -1;//强制升级状态
     *
     *     public static final int STATE_IGNORE = 1;//可忽略状态
     */
    void onUpdateNotifyDialogIgnore(UpdateAppBean updateApp,int state);

    /**
     * 当默认的更新提示框被用户选择升级
     *
     * @param updateApp updateApp
     */
    void onUpdateNotifyDialogOk(UpdateAppBean updateApp);
}
