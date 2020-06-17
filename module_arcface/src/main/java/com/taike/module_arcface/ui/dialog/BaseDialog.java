package com.taike.module_arcface.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

public abstract class BaseDialog<V extends ViewDataBinding> extends Dialog {
     protected V binding;

    public BaseDialog(@NonNull Context context) {
        super(context);
        binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), initContentView(), null, false);
        setContentView(binding.getRoot());
        //setCanceledOnTouchOutside(false);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);//去掉白色背景
        initView();

    }

    public BaseDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), initContentView(), null, false);
        setContentView(binding.getRoot());
        setCanceledOnTouchOutside(false);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);//去掉白色背景
        initView();
    }

    protected BaseDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), initContentView(), null, false);
        setContentView(binding.getRoot());
        setCanceledOnTouchOutside(false);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);//去掉白色背景
        initView();
    }

    protected abstract int initContentView();

    protected void initView() {
    }
}
