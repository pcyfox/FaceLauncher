package com.taike.lib_common.ext

import com.elvishew.xlog.XLog

class AnyExt {
    fun Any.XLogD(log: String) {
        XLog.d(" ${javaClass.simpleName}  $log ")
    }

//    fun Object.XLogD(log: String) {
//        XLog.d(" ${javaClass.simpleName}  $log ")
//    }
}