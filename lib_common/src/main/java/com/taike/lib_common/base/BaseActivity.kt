package com.taike.lib_common.base

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import com.taike.lib_common.R
import com.taike.lib_common.base.BaseActivity.Click.SPACE_TIME
import com.taike.lib_common.base.BaseActivity.Click.hash
import com.taike.lib_common.base.BaseActivity.Click.lastClickTime
import com.taike.lib_common.ext.toast
import com.taike.lib_common.utils.MaskProgressDialog
import com.taike.lib_utils.CloseBarUtil
import com.taike.lib_utils.EventDetector
import com.taike.lib_utils.MaskUtils


/**
 * @author pcy
 * @package
 * @describe Activity 基类
 */

abstract class BaseActivity<VDB : ViewDataBinding, VM : BaseViewModel> : FragmentActivity() {
    protected var viewModel: VM? = null
        private set
    protected var viewDataBinding: VDB? = null
        private set
    private var onKeyDownListeners: ArrayList<OnKeyDownListener>? = null
    protected abstract val layoutId: Int
    protected abstract fun createViewModel(): VM
    private val eventDetector by lazy { EventDetector(3, 1800) }
    open var isDoubleClickExit = false
    open var mainViewModelId = -1
    open var isFullScreen = true
    open var isClickBack = true
    open var isDisableBottomNav = true
    private var progress: MaskProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFullScreen) {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            //全屏
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN)
            hideNavigationBar()
        }
        initViewDataBinding()
        observeData()
        initData()
        initView()
        initListener()
        request()
    }

    protected fun showProgress() {
        if (progress == null) {
            progress = MaskProgressDialog(false, object : MaskProgressDialog.DialogClickListener {
                override fun onCancelClick() {
                    onProgressClosed()
                }
            })
        }
        progress?.show(this)
    }

    protected fun hideProgress() {
        progress?.dismiss()
    }


    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        onCreateOver()
    }

    override fun onPostResume() {
        super.onPostResume()
        onResumeOver()
        if (isDisableBottomNav) {
            // CloseBarUtil.closeBar()
        } else {
            //  CloseBarUtil.showBar()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (viewModel != null) {
            lifecycle.removeObserver(viewModel!!)
        }
        if (onKeyDownListeners != null) {
            onKeyDownListeners!!.clear()
            onKeyDownListeners = null
        }
        progress?.listener = null
        progress?.dismiss()
        progress = null
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (onKeyDownListeners == null) {
            return super.onKeyDown(keyCode, event)
        }

        for (listener in onKeyDownListeners!!) {
            return listener.onKeyDown(keyCode, event)
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onBackPressed() {
        if (!isClickBack) {
            return
        }
        if (isDoubleClickExit) {
            eventDetector.addEvent()
            if (eventDetector.timesLack - 1 != 0) {
                toast(String.format(getString(R.string.common_click_more_will_finish), eventDetector.timesLack - 1))
            } else if (eventDetector.timesLack == 1) {
                finish()
            }
        } else {
            super.onBackPressed()
        }
    }

    fun registerOnKeyDownListener(vararg listener: OnKeyDownListener) {
        if (onKeyDownListeners == null) {
            onKeyDownListeners = ArrayList()
        }
        onKeyDownListeners!!.addAll(listener)
    }

    /**
     * 注入绑定
     */
    private fun initViewDataBinding() {
        viewModel = createViewModel()
        if (viewModel != null) {
            //让ViewModel拥有View的生命周期感应
            lifecycle.addObserver(viewModel!!)
        }
        //DataBindingUtil类需要在project的build中配置 dataBinding {enabled true }, 同步后会自动关联android.databinding包
        viewDataBinding = DataBindingUtil.setContentView(this, layoutId)
        //注入Lifecycle生命周期
        viewDataBinding?.lifecycleOwner = this
        if (mainViewModelId != -1) {
            viewDataBinding?.setVariable(mainViewModelId, viewModel)
        }
    }


    protected fun <T : BaseViewModel> getViewModel(clazz: Class<T>): T {
        return ViewModelProviders.of(this).get(clazz)
    }


    fun addVariable(wm: VM?, vararg ids: Int) {
        wm?.let {
            for (id in ids) {
                viewDataBinding?.setVariable(id, wm)
            }
        }
    }

    open fun beforeDoubleClickToFinish() {}


    open fun initData() {

    }

    open fun request() {

    }

    open fun observeData() {
//        LiveEventBus.get().with(NetWorkChangReceiver.NetWorkChangEvent::class.java.simpleName, NetWorkChangReceiver.NetWorkChangEvent::class.java).observe(this, Observer {
//            onNetWorkChange(it.isAvailable)
//        })
    }

    open fun initListener() {

    }

    open fun initView() {}

    open fun onCreateOver() {
    }

    open fun onResumeOver() {
    }

    open fun onProgressClosed() {}

    protected fun hideNavigationBar() {
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) {
            val v = this.window.decorView
            v.systemUiVisibility = View.GONE
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
            window.decorView.systemUiVisibility = uiOptions
        }
        CloseBarUtil.hideBottomUIMenu(window);
    }


    object Click {
        var hash: Int = 0
        var lastClickTime: Long = 0
        const val SPACE_TIME: Long = 900
    }

    infix fun safeClick(clickAction: () -> Unit): Boolean {
        if (this.hashCode() != hash) {
            hash = this.hashCode()
            lastClickTime = System.currentTimeMillis()
            clickAction()
            return true
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > SPACE_TIME) {
                lastClickTime = System.currentTimeMillis()
                clickAction()
                return true
            }
        }
        return false
    }

    open fun getContext(): Context {
        return this
    }

    open fun onNetWorkChange(isAvailable: Boolean) {
        //网络异常悬浮窗
        if (!isAvailable) {
            MaskUtils.show(window, R.layout.layout_toast_no_available_network_tip, this)
        } else {
            MaskUtils.hide(this, this)
        }
    }

}
