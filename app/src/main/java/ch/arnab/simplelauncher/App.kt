package ch.arnab.simplelauncher

import android.app.Application
import androidx.multidex.MultiDex
import com.elvishew.xlog.XLog

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        MultiDex.install(this)
        XLog.init()
    }

    companion object{
        var isRegister=false
    }


}