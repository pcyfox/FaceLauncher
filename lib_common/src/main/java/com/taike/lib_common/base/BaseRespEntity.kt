package com.taike.lib_common.base

import com.taike.lib_common.base.RespCode.OK

open class BaseRespEntity<D>(var resultCode: Int = -1,
                             var message: String = "",
                             var data: D? = null) {
    fun isOK() = resultCode == OK
}

object RespCode {
    const val OK = 0
}