package ch.arnab.simplelauncher.ui

import android.view.View
import androidx.lifecycle.ViewModelProvider
import ch.arnab.simplelauncher.App
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.ToastUtils
import com.elvishew.xlog.XLog
import com.taike.lib_common.base.BaseActivity
import com.taike.lib_common.base.BaseViewModel
import com.taike.lib_common.ext.postDelayed
import com.taike.lib_common.ext.toastLong
import com.taike.module_arcface.arcface.ImportFaceUtils
import com.taike.module_arcface.ui.activity.ArcFaceDialog
import com.taike.module_arcface.ui.activity.ArcFaceUtils
import com.tk.facelauncher.R
import com.tk.facelauncher.databinding.ActivityMainBinding
import java.io.File

class MainActivity(override val layoutId: Int = R.layout.activity_main) : BaseActivity<ActivityMainBinding, BaseViewModel>() {
    private val pkgName = "com.taike.edu.stu"
    private val faceDir = ImportFaceUtils.ROOT_DIR
    private var arcFaceDialog: ArcFaceDialog? = null
    override var isFullScreen = true
    override fun onCreateOver() {
        super.onCreateOver()
        if (App.isRegister) {
            goNextActivity()
            return
        }
        questPermission()
    }

    override fun createViewModel(): BaseViewModel {
        return ViewModelProvider(this).get(BaseViewModel::class.java)
    }

    override fun onDestroy() {
        super.onDestroy()
        App.isRegister = false
    }

    private fun goNextActivity() {
        AppUtils.launchApp(pkgName)
       // startActivity(Intent(this@MainActivity, HomeScreen::class.java))
    }

    private fun questPermission() {
        PermissionUtils.permission(PermissionConstants.CAMERA,
                PermissionConstants.STORAGE, PermissionConstants.PHONE).callback(object : PermissionUtils.FullCallback {
            override fun onGranted(permissionsGranted: MutableList<String>?) {
                val file = File(faceDir)
                if (!file.exists()) {
                    file.mkdir()
                }
                registerFace()
                ArcFaceUtils.initFace(this@MainActivity, object : ArcFaceUtils.ArcfaceCallback {
                    override fun onSuccess() {
                        ToastUtils.showLong("人脸识别激活成功")
                        XLog.e("initFace success!")
                    }

                    override fun onError(errorMsg: String?) {
                        ToastUtils.showLong("人脸识别激活失败")
                    }

                })

            }

            override fun onDenied(permissionsDeniedForever: MutableList<String>?, permissionsDenied: MutableList<String>?) {
                //To change body of created functions use File | Settings | File Templates.
            }

        }).request()


    }


    fun onClick(view: View?) {
        recognizeFace()
    }


    private fun registerFace() {
        ImportFaceUtils.getInstance().doRegister(applicationContext, faceDir, object : ImportFaceUtils.ImportFaceCallBack() {
            override fun onFinish(result: String?) {
                XLog.d("ImportFaceUtils  onFinish$result")
                arcFaceDialog?.setRegistered(true)
            }
        })
    }


    private fun recognizeFace() {
        if (arcFaceDialog != null && arcFaceDialog!!.isShowing) {
            return
        }
        registerFace()
        arcFaceDialog = ArcFaceDialog(this)
        arcFaceDialog?.setCallback(object : ArcFaceDialog.OnArcFaceCallback {
            override fun onSuccess(id: String?) {
                toastLong("$id   识别成功")
                postDelayed(400, Runnable {
                    goNextActivity()
                    arcFaceDialog?.dismiss()
                    App.isRegister = true
                })
            }

            override fun onFail(id: String?) {
                arcFaceDialog?.dismiss()
                postDelayed(500, Runnable {
                    toastLong("$id   识别失败")
                })
            }

            override fun onDismiss() {

            }
        }, "", "")
        arcFaceDialog?.show()
    }
}