package ch.arnab.simplelauncher.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import ch.arnab.simplelauncher.App
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.*
import com.elvishew.xlog.XLog
import com.taike.lib_common.base.BaseActivity
import com.taike.lib_common.base.BaseViewModel
import com.taike.lib_common.ext.post
import com.taike.lib_common.ext.postDelayed
import com.taike.lib_common.ext.toastLong
import com.taike.lib_utils.MaskUtils
import com.taike.module_arcface.arcface.ImportFaceUtils
import com.taike.module_arcface.ui.dialog.ArcFaceDialog
import com.taike.module_arcface.ui.dialog.ArcFaceUtils
import com.taike.module_arcface.ui.fragment.ArcFaceFragment
import com.tk.facelauncher.R
import com.tk.facelauncher.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.lang.Exception

class MainActivity(override val layoutId: Int = R.layout.activity_main) : BaseActivity<ActivityMainBinding, BaseViewModel>() {
    private val firstStart = "firstStart"
    private val pkgName = "com.ctrl.freesky"
    private val faceDir = ImportFaceUtils.ROOT_DIR
    private var arcFaceFragment: ArcFaceFragment? = null
    private val notFindAppTip = "未找到待启动应用,请点击后重试！"
    override var isFullScreen = true
    private var isMaskShowing = true
    private var isHasPermission = false
    private val TAG = "MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MaskUtils.show(this, View.inflate(this, R.layout.mask_splash, null), "mask_splash")
        isMaskShowing = true
    }


    override fun onCreateOver() {
        super.onCreateOver()
        questPermission()
        arcFaceFragment = fl_recognize_face as ArcFaceFragment
        arcFaceFragment?.stop()

        postDelayed(8 * 1000, Runnable {
            MaskUtils.hide(this, "mask_splash")
            isMaskShowing = false
            if (isHasPermission) {
                arcFaceFragment?.start()
            } else {
                toastLong("应用程序未获得完整授权，无法继续")
            }
        })
    }

    override fun createViewModel(): BaseViewModel {
        return ViewModelProvider(this).get(BaseViewModel::class.java)
    }

    override fun onDestroy() {
        super.onDestroy()
        App.isRegister = false
    }

    private fun goNextActivity() {
        try {
            if (isAvilible(pkgName)) {
                AppUtils.launchApp(pkgName)
            } else {
                arcFaceFragment?.stop(notFindAppTip, Color.RED)
                toastLong("未找到待启动应用！")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isAvilible(packageName: String): Boolean {
        val packageManager = packageManager;
        // 获取所有已安装程序的包信息
        val pinfo = packageManager.getInstalledPackages(0);
        pinfo.forEach {
            if (it.packageName.equals(packageName, true)) {
                return true
            }
        }
        return false
    }

    private fun questPermission() {
        PermissionUtils.permission(
                PermissionConstants.CAMERA,
                PermissionConstants.STORAGE,
                PermissionConstants.PHONE).callback(object : PermissionUtils.FullCallback {
            override fun onGranted(permissionsGranted: MutableList<String>?) {
                Log.d(TAG, "onGranted() called with: permissionsGranted = $permissionsGranted")
                isHasPermission = permissionsGranted != null && permissionsGranted.size >= 3
                if (isHasPermission) {
                    if (!SPUtils.getInstance().getBoolean(firstStart, false)) {
                        FileUtils.deleteAllInDir(faceDir)
                        SPUtils.getInstance().put(firstStart, true)
                    }
                }
                val file = File(faceDir)
                if (!file.exists()) {
                    file.mkdir()
                }
                registerFace()
                ArcFaceUtils.initFace(this@MainActivity, object : ArcFaceUtils.ArcfaceCallback {
                    override fun onSuccess() {
                        ToastUtils.showLong("人脸识别激活成功")
                        XLog.i("initFace success!")
                        recognizeFace()
                    }

                    override fun onError(errorMsg: String?) {
                        ToastUtils.showLong("人脸识别激活失败")
                    }

                })
                if (!isMaskShowing) {
                    arcFaceFragment?.start()
                }
            }

            override fun onDenied(permissionsDeniedForever: MutableList<String>?, permissionsDenied: MutableList<String>?) {
                //To change body of created functions use File | Settings | File Templates.
            }

        }).request()
    }

    private fun registerFace() {
        ImportFaceUtils.getInstance().doRegister(applicationContext, faceDir, object : ImportFaceUtils.ImportFaceCallBack() {
            override fun onFinish(result: String?) {
                XLog.d("ImportFaceUtils  onFinish$result")
                arcFaceFragment?.setRegistered(true)
            }
        })
    }


    private fun recognizeFace() {
        registerFace()
        arcFaceFragment?.setCallback(object : ArcFaceFragment.OnArcFaceCallback {
            override fun onSuccess(id: String?) {
                toastLong("$id   识别成功")
                postDelayed(400, Runnable {
                    goNextActivity()
                })
            }

            override fun onFail(id: String?) {
                postDelayed(500, Runnable {
                    toastLong("$id   识别失败")
                })
            }

            override fun onTipViewClick(text: String) {
            }
        }, "", "")
    }
}