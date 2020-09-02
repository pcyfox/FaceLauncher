package ch.taike.launcher

import android.app.Application
import androidx.multidex.MultiDex
import ch.taike.launcher.manager.TrdServiceManager
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.PermissionUtils
import com.taike.lib_common.BuildConfig

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        MultiDex.install(this)
        initLog()
    }

    companion object {
        var isRegister = false
    }

    private fun initLog() {
        val ELKPort = 8090
        TrdServiceManager.initLog("TK-LAUNCHER", BuildConfig.ELK_URL + ":" + ELKPort)
        TrdServiceManager.initLiveEventBus()
        if (PermissionUtils.isGranted(PermissionConstants.STORAGE)) {
            Thread {
                Thread.sleep(3 * 1000)//延迟处理，避免影响APP启动流畅度
                TrdServiceManager.uploadCacheLog()
            }.start()
        }
    }
}