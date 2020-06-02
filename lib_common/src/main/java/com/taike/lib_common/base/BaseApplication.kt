package com.taike.lib_common.base

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.taike.lib_common.application.BaseAbstractApplication


/**
 * @author xiaoqqq
 * @package com.ihealthcare.doctor_app
 * @date 2018/11/26
 * @describe todo
 */

  @RequiresApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  abstract class BaseApplication : BaseAbstractApplication() , Application.ActivityLifecycleCallbacks{
    override fun onActivityPaused(activity: Activity?) {

    }

    override fun onActivityResumed(activity: Activity?) {

    }

    override fun onActivityStarted(activity: Activity?) {

    }

    override fun onActivityDestroyed(activity: Activity?) {

    }

    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {

    }

    override fun onActivityStopped(activity: Activity?) {

    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {

    }

    @RequiresApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    @RequiresApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    override fun onTerminate() {
        super.onTerminate()
        unregisterActivityLifecycleCallbacks(this)
    }

}
