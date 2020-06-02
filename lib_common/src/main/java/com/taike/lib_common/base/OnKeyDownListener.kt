package com.taike.lib_common.base

import android.view.KeyEvent

interface OnKeyDownListener {
    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean
}