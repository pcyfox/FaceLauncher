package com.ff.module_update.utils;

import com.ff.module_update.impl.DefDownLoadManagerImpl;
import com.ff.module_update.impl.DefHttpManagerImpl;
import com.vector.update_app.UpdateAppManager;

public class UpdateAppManagerUtils {
    /**
     * @param
     * @return 返回一个带有默认DownLoadManager、DownLoadManager实现的UpdateAppManager.Builder对象
     */
    public static UpdateAppManager.Builder createDefBuilder() {
        UpdateAppManager.Builder builder = new UpdateAppManager.Builder();
        return builder.setDownLoadManager(new DefDownLoadManagerImpl())
                .setHttpManager(new DefHttpManagerImpl());
    }

    public static DefHttpManagerImpl getDefHttpManagerImpl() {
        return new DefHttpManagerImpl();
    }

    public static DefDownLoadManagerImpl getDefDownLoadManagerImpl() {
        return new DefDownLoadManagerImpl();
    }
}
