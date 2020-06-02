package com.taike.lib_common.utils

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.ScreenUtils
import com.taike.lib_common.R
import kotlinx.android.synthetic.main.common_layout_progress.*


class MaskProgressDialog(private val cancelable: Boolean = false, var listener: DialogClickListener?) : AppCompatDialogFragment() {
    override fun onStart() {
        super.onStart()
        dialog?.setCancelable(cancelable)
        val lp = dialog?.window?.attributes
        lp?.width = ScreenUtils.getScreenWidth()
        lp?.height = ScreenUtils.getScreenHeight()
        dialog?.window?.attributes = lp
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.common_layout_progress, null)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun onDetach() {
        super.onDetach()
        if (!parentFragmentManager.isDestroyed) {
            parentFragmentManager.popBackStack()
        }
    }

    private fun initView() {
        avi_loading.show()
        teach_close_progress.setOnClickListener {
            listener?.onCancelClick()
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        avi_loading.hide()
    }

    interface DialogClickListener {
        fun onCancelClick()
    }

    fun show(context: FragmentActivity) {
        show(context.supportFragmentManager, context::javaClass.name)
    }
}