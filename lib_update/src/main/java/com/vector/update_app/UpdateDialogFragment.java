package com.vector.update_app;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.vector.update_app.interf.DownloadCallback;
import com.vector.update_app.listener.ExceptionHandler;
import com.vector.update_app.listener.ExceptionHandlerHelper;
import com.vector.update_app.listener.IUpdateDialogFragmentListener;
import com.vector.update_app.service.DownloadService;
import com.vector.update_app.utils.AppUpdateUtils;
import com.vector.update_app.utils.ColorUtil;
import com.vector.update_app.utils.DrawableUtil;
import com.vector.update_app.view.NumberProgressBar;

import java.io.File;

/**
 * Created by Vector
 * on 2017/7/19 0019.
 */

public class UpdateDialogFragment extends DialogFragment implements View.OnClickListener {
    public static final String TIPS = "请授权访问存储空间权限，否则App无法更新";
    public static boolean isShow = false;
    private TextView mContentTextView;
    private TextView tvVersionName;
    private Button mUpdateOkButton;
    private UpdateAppBean mUpdateApp;
    private NumberProgressBar mNumberProgressBar;
    private ImageView mIvClose;
    private TextView mTitleTextView;
    private int state;

    public static final int STATE_NORMAL = 0;//普通状态
    public static final int STATE_CONSTRAINT = -1;//强制升级状态
    public static final int STATE_IGNORE = 1;//可忽略状态

    private boolean isCheckUpdateByUser;//手动处方升级
    /**
     * 回调
     */
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceConnected((DownloadService.DownloadBinder) service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private int mDefaultPicResId = R.mipmap.lib_update_app_top_bg;
    private RelativeLayout mTopIv;
    private TextView mIgnore;
    private IUpdateDialogFragmentListener mUpdateDialogFragmentListener;
    private DownloadService.DownloadBinder mDownloadBinder;
    private Activity mActivity;

    public UpdateDialogFragment setUpdateDialogFragmentListener(IUpdateDialogFragmentListener updateDialogFragmentListener) {
        this.mUpdateDialogFragmentListener = updateDialogFragmentListener;
        return this;
    }


    public static UpdateDialogFragment newInstance(Bundle args) {
        UpdateDialogFragment fragment = new UpdateDialogFragment();
        if (args != null) {
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isShow = true;
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.UpdateAppDialog);
        mActivity = getActivity();
    }

    @Override
    public void onStart() {
        super.onStart();
        //点击window外的区域 是否消失
        getDialog().setCanceledOnTouchOutside(false);
        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    //禁用
                    if (mUpdateApp != null && mUpdateApp.isConstraint()) {
                        //返回桌面
                        startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
                        return true;
                    } else {
                        return false;
                    }
                }
                return false;
            }
        });

        Window dialogWindow = getDialog().getWindow();
        dialogWindow.setGravity(Gravity.CENTER);
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        lp.height = (int) (displayMetrics.heightPixels * 0.9f);
        lp.width = (int) (displayMetrics.widthPixels * 0.25f);
        dialogWindow.setAttributes(lp);

        if (mUpdateDialogFragmentListener != null) {
            mUpdateDialogFragmentListener.onUpdateNotifyDialogOnStart(mUpdateApp);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.lib_update_app_dialog, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
    }

    private void initView(View view) {
        //提示内容
        mContentTextView = view.findViewById(R.id.tv_update_info);
        //标题
        mTitleTextView = view.findViewById(R.id.tv_title);
        //更新按钮
        mUpdateOkButton = view.findViewById(R.id.btn_ok);
        //进度条
        mNumberProgressBar = view.findViewById(R.id.npb);
        //关闭按钮
        mIvClose = view.findViewById(R.id.iv_close);
        //顶部图片
        mTopIv = view.findViewById(R.id.rl_top);
        //忽略
        mIgnore = view.findViewById(R.id.tv_ignore);

        tvVersionName = view.findViewById(R.id.tv_version_name);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initData();
    }

    @Override
    public void onResume() {
        super.onResume();
        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_BACK) {
                    // Toast.makeText(getActivity(), "按了返回键", Toast.LENGTH_SHORT).show();
                    if (mUpdateDialogFragmentListener != null) {
                        mUpdateDialogFragmentListener.onUpdateNotifyDialogCancel(mUpdateApp);
                    }
                }
                return false;
            }
        });
    }

    private void initData() {
        mUpdateApp = (UpdateAppBean) getArguments().getSerializable(UpdateAppManager.INTENT_KEY);
        //设置主题色
        initTheme();

        if (mUpdateApp != null) {
            initEvents();
            //弹出对话框
            final String dialogTitle = mUpdateApp.getUpdateDefDialogTitle();
            final String newVersion = mUpdateApp.getNewVersion();

            final String targetSize = mUpdateApp.getTargetSize();
            final String updateLog = mUpdateApp.getUpdateLog();

            tvVersionName.setText(newVersion);

            String msg = "";
            if (!TextUtils.isEmpty(targetSize)) {
                msg = "新版本大小：" + targetSize + "MB\n\n";
            }

            if (!TextUtils.isEmpty(updateLog)) {
                msg += updateLog;
            }
            //更新内容
            mContentTextView.setText(msg);
            //标题
            if (!TextUtils.isEmpty(dialogTitle)) {
                mTitleTextView.setText(dialogTitle);
            }

            if (isCheckUpdateByUser) {//手动触发升级
                state = STATE_NORMAL;
                mIgnore.setText("暂不体验");
                return;
            }

            //强制更新
            if (mUpdateApp.isConstraint()) {
                mIgnore.setText("退出应用");
                state = STATE_CONSTRAINT;
            } else if (mUpdateApp.isCanIgnoreVersion()) {
                state = STATE_IGNORE;
                mIgnore.setText("忽略该版本");
            } else {
                state = STATE_NORMAL;
                mIgnore.setText("暂不体验");
            }
        }
    }


    /**
     * 初始化主题色
     */
    private void initTheme() {
        Bundle arguments = getArguments();
        if (arguments == null) {
            return;
        }
        final int color = arguments.getInt(UpdateAppManager.THEME_KEY, -1);
        final int topResId = mUpdateApp.getDialogTopBg();
        isCheckUpdateByUser = arguments.getBoolean(UpdateAppManager.IS_CHECK_UPDATE_BY_USER, false);
        //默认色
        int mDefaultColor = 0xff0563bb;
        if (-1 == topResId) {
            if (-1 == color) {
                //默认红色
                setDialogTheme(mDefaultColor, mDefaultPicResId);
            } else {
                setDialogTheme(getResources().getColor(color), mDefaultPicResId);
            }
        } else {
            if (-1 == color) {
                //自动提色
                setDialogTheme(mDefaultColor, topResId);
            } else {
                //更加指定的上色
                setDialogTheme(color, topResId);
            }
        }
    }

    /**
     * 设置
     *
     * @param color    主色
     * @param topResId 图片
     */
    private void setDialogTheme(int color, int topResId) {
        mTopIv.setBackgroundResource(topResId);
        mUpdateOkButton.setBackground(DrawableUtil.getDrawable(AppUpdateUtils.dip2px(6, getActivity()), color));
        mNumberProgressBar.setProgressTextColor(color);
        mNumberProgressBar.setReachedBarColor(color);
        //随背景颜色变化
        mUpdateOkButton.setTextColor(ColorUtil.isTextColorDark(color) ? Color.BLACK : Color.WHITE);
    }

    private void initEvents() {
        mUpdateOkButton.setOnClickListener(this);
        mIvClose.setOnClickListener(this);
        mIgnore.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.btn_ok) {
            if (mUpdateDialogFragmentListener != null) {
                mUpdateDialogFragmentListener.onUpdateNotifyDialogOk(mUpdateApp);
            }
            //权限判断是否有访问外部存储空间权限
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // 用户拒绝过这个权限了，应该提示用户，为什么需要这个权限。
                    Toast.makeText(getActivity(), TIPS, Toast.LENGTH_LONG).show();
                } else {
                    // 申请授权。
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                }
            } else {
                installApp();
            }

        } else if (i == R.id.iv_close) {
            cancelDownloadService();
            if (mUpdateDialogFragmentListener != null) {
                // 通知用户
                mUpdateDialogFragmentListener.onUpdateNotifyDialogCancel(mUpdateApp);
            }

            if (state == STATE_CONSTRAINT) {
                exitApp();
            } else {
                dismiss();
            }
        } else if (i == R.id.tv_ignore) {
            if (mUpdateDialogFragmentListener != null) {
                mUpdateDialogFragmentListener.onUpdateNotifyDialogIgnore(mUpdateApp, state);
            }
            switch (state) {
                case STATE_NORMAL://暂不体验
                    break;
                case STATE_IGNORE:
                    AppUpdateUtils.saveIgnoreVersion(getActivity(), mUpdateApp.getNewVersion());
                    break;
                case STATE_CONSTRAINT:
                    exitApp();
                    break;
            }
            dismiss();
        }
    }


    private void exitApp() {
        getActivity().finish();
        System.exit(0);
    }


    public void cancelDownloadService() {
        if (mDownloadBinder != null) {
            // 标识用户已经点击了更新，之后点击取消
            mDownloadBinder.stop("取消下载", mUpdateApp);
        }
    }

    private void installApp() {
        if (AppUpdateUtils.isAppDownloaded(mUpdateApp)) {//APK已经下载过
            AppUpdateUtils.installApp(UpdateDialogFragment.this, AppUpdateUtils.getAppFile(mUpdateApp));
            //安装完自杀
            //如果上次是强制更新，但是用户在下载完，强制杀掉后台，重新启动app后，则会走到这一步，所以要进行强制更新的判断。
            if (!mUpdateApp.isConstraint()) {
                dismiss();
            } else {
                showInstallBtn(AppUpdateUtils.getAppFile(mUpdateApp));
            }
        } else {//APK未下载，直接启动服务下载
            downloadApp();
            //这里的隐藏对话框会和强制更新冲突，导致强制更新失效，所以当强制更新时，不隐藏对话框。
            if (mUpdateApp.isHideDialog() && !mUpdateApp.isConstraint()) {
                dismiss();
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //升级
                installApp();
            } else {
                //提示，并且关闭
                Toast.makeText(getActivity(), TIPS, Toast.LENGTH_LONG).show();
                dismiss();
            }
        }

    }

    /**
     * 开启后台服务下载
     */
    private void downloadApp() {
        DownloadService.bindService(getActivity().getApplicationContext(), conn);
    }

    /**
     * 回调监听下载
     */
    private void serviceConnected(DownloadService.DownloadBinder binder) {
        // 开始下载，监听下载进度，可以用对话框显示
        if (mUpdateApp == null) {
            return;
        }
        this.mDownloadBinder = binder;

        binder.start(mUpdateApp, new DownloadCallback() {
            @Override
            public void onStart() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!UpdateDialogFragment.this.isRemoving()) {
                            mNumberProgressBar.setVisibility(View.VISIBLE);
                            mUpdateOkButton.setVisibility(View.GONE);
                            mIgnore.setVisibility(View.GONE);
                        }
                    }
                });
            }

            @Override
            public void onProgress(final float progress, final long totalSize) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!UpdateDialogFragment.this.isRemoving()) {
                            mNumberProgressBar.setProgress(Math.round(progress / totalSize * 100));
                            mNumberProgressBar.setMax(100);
                        }
                    }
                });
            }

            @Override
            public void setMax(long total) {

            }

            @Override
            public boolean onFinish(final File file) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!UpdateDialogFragment.this.isRemoving()) {
                            if (mUpdateApp.isConstraint()) {
                                showInstallBtn(file);
                            } else {
                                dismissAllowingStateLoss();
                            }
                        }
                    }
                });
                //一般返回 true ，当返回 false 时，则下载，不安装，为静默安装使用。
                return true;
            }

            @Override
            public void onError(String msg) {
                if (!UpdateDialogFragment.this.isRemoving()) {
                    dismissAllowingStateLoss();
                }
            }

            @Override
            public boolean onInstallAppAndAppOnForeground(File file) {
                //这样做的目的是在跳转安装界面，可以监听到用户取消安装的动作;
                //activity.startActivityForResult(intent, REQ_CODE_INSTALL_APP);
                //但是如果 由DownloadService 跳转到安装界面，则监听失效。
                if (!mUpdateApp.isConstraint()) {
                    dismiss();
                }
                if (mActivity != null) {
                    AppUpdateUtils.installApp(mActivity, file);
                    //返回 true ，自己处理。
                    return true;
                } else {
                    //返回 flase ，则由 DownloadService 跳转到安装界面。
                    return false;
                }
            }
        });

    }

    private void showInstallBtn(final File file) {
        mNumberProgressBar.setVisibility(View.GONE);
        mUpdateOkButton.setText("安装");
        mUpdateOkButton.setVisibility(View.VISIBLE);
        mIgnore.setVisibility(View.VISIBLE);
        mUpdateOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppUpdateUtils.installApp(UpdateDialogFragment.this, file);
            }
        });
    }


    @Override
    public void show(FragmentManager manager, String tag) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            if (manager.isDestroyed()) {
                return;
            }
        }
        try {
            super.show(manager, tag);
        } catch (Exception e) {
            ExceptionHandler exceptionHandler = ExceptionHandlerHelper.getInstance();
            if (exceptionHandler != null) {
                exceptionHandler.onException(e);
            }
        }
    }

    @Override
    public void onDestroyView() {
        isShow = false;
        super.onDestroyView();
    }

}

