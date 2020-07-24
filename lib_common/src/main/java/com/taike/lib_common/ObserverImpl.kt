package com.taike.lib_common


import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.GsonUtils
import com.elvishew.xlog.XLog
import com.taike.lib_common.base.BaseRespEntity
import com.taike.lib_utils.Util
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.net.SocketTimeoutException


open class ObserverImpl<T> : Observer<T> {
    private var liveData: MutableLiveData<T>? = null
    private var clazz: Class<T>? = null

    constructor(data: MutableLiveData<T>, clazz: Class<T>) {
        liveData = data
        this.clazz = clazz
    }

    override fun onNext(t: T) {
        liveData?.postValue(t)
    }



    override fun onError(e: Throwable) {
        e.printStackTrace()
        XLog.w("ObserverImpl onError():" + genName() + Util.getExceptionContent(e))
        liveData?.postValue(buildErrorData(e, clazz!!))
    }

    override fun onComplete() {}

    override fun onSubscribe(d: Disposable) {}

    private fun genName(): String {
        return javaClass.name + hashCode()
    }

    companion object {
        private fun <T> buildErrorData(e: Throwable, clazz: Class<T>): T {
            var code = -200
            val tip = if (e.javaClass.name.contains("java.net")) {
                if (e is SocketTimeoutException) {
                    code = -300
                    "连接服务器超时"
                } else {
                    code = -400
                    "网络异常"
                }
            } else {
                e.javaClass.simpleName
            }
            val baseEntity = BaseRespEntity<T>()
            baseEntity.message = tip
            baseEntity.resultCode = code

            return GsonUtils.fromJson<T>(GsonUtils.toJson(baseEntity), clazz)
        }
    }

}