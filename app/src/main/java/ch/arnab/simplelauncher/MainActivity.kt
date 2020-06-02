package ch.arnab.simplelauncher

import android.content.Intent
import android.os.Bundle

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.ToastUtils
import com.elvishew.xlog.XLog
import com.tk.facelauncher.R
import com.taike.lib_common.ext.postDelayed
import com.taike.lib_common.ext.toastLong
import com.taike.module_arcface.arcface.ImportFaceUtils
import com.taike.module_arcface.ui.activity.ArcFaceDialog
import com.taike.module_arcface.ui.activity.ArcFaceUtils

import java.io.File

class MainActivity : AppCompatActivity() {
    private val faceDir = ImportFaceUtils.ROOT_DIR
    private var arcFaceDialog: ArcFaceDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (App.isRegister) {
            startActivity(Intent(this@MainActivity, HomeScreen::class.java))
            finish()
            return
        }
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
                    startActivity(Intent(this@MainActivity, HomeScreen::class.java))
                    finish()
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