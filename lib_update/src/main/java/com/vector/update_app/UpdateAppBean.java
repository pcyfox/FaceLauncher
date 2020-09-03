package com.vector.update_app;


import androidx.annotation.DrawableRes;

import com.vector.update_app.interf.ApkDownLoadManager;

import java.io.Serializable;

/**
 * 版本信息
 */
public class UpdateAppBean implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * isUpdate : Yes
     * newVersion : xxxxx
     * apk_url : http://cdn.the.url.of.apk/or/patch
     * updateLog : xxxx
     * delta : false
     * newMd5 : xxxxxxxxxxxxxx
     * targetSize : 601132
     */
    //是否需要升级
    private boolean isUpdate;
    private boolean ignored;
    //提示次数,tipCount=-1,代表弹出次数无限制
    private int tipCount = -1;
    //新版本号
    private String newVersion;
    //新app下载地址
    private String apkFileUrl;
    //更新日志
    private String updateLog;
    //配置默认更新dialog 的title
    private String updateDefDialogTitle;
    //新app大小
    private String targetSize;
    //是否强制更新
    private boolean constraint;
    //md5
    private String newMd5;
    //服务器端的原生返回数据（json）,方便使用者在hasNewApp自定义渲染dialog的时候可以有别的控制，比如：#issues/59
    private String originRes;
    //是否允许忽略当前版本
    private boolean isCanIgnoreVersion;

    /**********以下是内部使用的数据**********/
    //下载管理器
    private ApkDownLoadManager downLoadManager;
    private String storePath;
    private boolean mHideDialog;
    private boolean isDismissNotification;
    private boolean mOnlyWifi;
    //对话框顶部图片
    @DrawableRes
    private int dialogTopBg = -1;

    //是否隐藏对话框
    public boolean isHideDialog() {
        return mHideDialog;
    }

    public void setHideDialog(boolean hideDialog) {
        mHideDialog = hideDialog;
    }

    public boolean isUpdate() {
        return isUpdate;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public UpdateAppBean setIgnored(boolean ignored) {
        this.ignored = ignored;
        return  this;
    }

    public String getStorePath() {
        return storePath;
    }

    public ApkDownLoadManager getDownLoadManager() {
        return downLoadManager;
    }

    public void setDownLoadManager(ApkDownLoadManager downLoadManager) {
        this.downLoadManager = downLoadManager;
    }

    protected void setStorePath(String storePath) {
        this.storePath = storePath;
    }

    public boolean isConstraint() {
        return constraint;
    }

    public UpdateAppBean setConstraint(boolean constraint) {
        this.constraint = constraint;
        return this;
    }

    public boolean getUpdate() {
        return isUpdate;
    }

    public UpdateAppBean setUpdate(boolean update) {
        this.isUpdate = update;
        return this;
    }

    public String getNewVersion() {
        return newVersion;
    }

    public UpdateAppBean setNewVersion(String new_version) {
        this.newVersion = new_version;
        return this;
    }

    public String getApkFileUrl() {
        return apkFileUrl;
    }


    public UpdateAppBean setApkFileUrl(String apk_file_url) {
        this.apkFileUrl = apk_file_url;
        return this;
    }

    public String getUpdateLog() {
        return updateLog;
    }

    public UpdateAppBean setUpdateLog(String update_log) {
        this.updateLog = update_log;
        return this;
    }

    public String getUpdateDefDialogTitle() {
        return updateDefDialogTitle;
    }

    public UpdateAppBean setUpdateDefDialogTitle(String updateDefDialogTitle) {
        this.updateDefDialogTitle = updateDefDialogTitle;
        return this;
    }

    public String getNewMd5() {
        return newMd5;
    }

    public UpdateAppBean setNewMd5(String new_md5) {
        this.newMd5 = new_md5;
        return this;
    }

    public String getTargetSize() {
        return targetSize;
    }

    public UpdateAppBean setTargetSize(String target_size) {
        this.targetSize = target_size;
        return this;
    }

    public boolean isCanIgnoreVersion() {
        return isCanIgnoreVersion;
    }

    public UpdateAppBean setCanIgnoreVersion(boolean showIgnoreVersion) {
        isCanIgnoreVersion = showIgnoreVersion;
        return this;
    }

    public UpdateAppBean setDismissNotification(boolean isDismissNotification) {
        this.isDismissNotification = isDismissNotification;
        return this;
    }

    public boolean isDismissNotification() {
        return isDismissNotification;
    }

    public boolean isOnlyWifi() {
        return mOnlyWifi;
    }

    public void setOnlyWifi(boolean onlyWifi) {
        mOnlyWifi = onlyWifi;
    }

    public String getOriginRes() {
        return originRes;
    }

    public UpdateAppBean setOriginRes(String originRes) {
        this.originRes = originRes;
        return this;
    }

    public int getTipCount() {
        return tipCount;
    }

    public UpdateAppBean setTipCount(int tipCount) {
        this.tipCount = tipCount;
        return this;
    }

    public int getDialogTopBg() {
        return dialogTopBg;
    }

    public void setDialogTopBg(int dialogTopBg) {
        this.dialogTopBg = dialogTopBg;
    }

    @Override
    public String toString() {
        return "UpdateAppBean{" +
                "isUpdate=" + isUpdate +
                ", tipCount=" + tipCount +
                ", newVersion='" + newVersion + '\'' +
                ", apkFileUrl='" + apkFileUrl + '\'' +
                ", updateLog='" + updateLog + '\'' +
                ", updateDefDialogTitle='" + updateDefDialogTitle + '\'' +
                ", targetSize='" + targetSize + '\'' +
                ", constraint=" + constraint +
                ", newMd5='" + newMd5 + '\'' +
                ", originRes='" + originRes + '\'' +
                ", downLoadManager=" + downLoadManager +
                ", storePath='" + storePath + '\'' +
                ", mHideDialog=" + mHideDialog +
                ", isCanIgnoreVersion=" + isCanIgnoreVersion +
                ", isDismissNotification=" + isDismissNotification +
                ", mOnlyWifi=" + mOnlyWifi +
                '}';
    }
}
