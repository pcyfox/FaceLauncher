package com.taike.module_arcface.arcface;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class GlideDownloadUtils {


    public static void downloadImageToFile(final Context mContext, final String imgUrl, final String fileName, final String path, final DownloadImageCallback callback) {
        Disposable d = Observable.create(new ObservableOnSubscribe<File>() {
            @Override
            public void subscribe(ObservableEmitter<File> e) throws Exception {
                //通过gilde下载得到file文件,这里需要注意android.permission.INTERNET权限
                //XLog.dr("downloadImageToFile  subscribe   " + imgUrl);
                e.onNext(Glide.with(mContext)
                        .load(imgUrl)
                        .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .get());
                e.onComplete();
            }
        }).subscribeOn(Schedulers.io()).retry(6)
                .observeOn(Schedulers.newThread())
                .subscribe(new Consumer<File>() {
                    @Override
                    public void accept(File file) throws Exception {
                        File appDir = new File(path);
                        if (!appDir.exists()) {
                            appDir.mkdirs();
                        }
                        // String fileName = System.currentTimeMillis() + ".jpg";
                        File destFile = new File(appDir, fileName);
                        //把gilde下载得到图片复制到定义好的目录中去
                        try {
                            copy(file, destFile);
                            callback.onSuccess(imgUrl, destFile.getAbsolutePath());
                        } catch (Exception e) {
                            e.printStackTrace();
                            callback.onError("链接Exception  " + imgUrl + "    " + e.getMessage());
                        }
                    }

                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        callback.onError("链接Throwable  " + imgUrl + "    " + throwable.getMessage());

                    }
                });
        d.dispose();
    }


    public static void downloadExamImageToFile(final Context mContext, final String imgUrl, final DownloadImageCallback callback) {
        final String fileName = System.currentTimeMillis() + ".jpg";
        Disposable d = Observable.create(new ObservableOnSubscribe<File>() {
            @Override
            public void subscribe(ObservableEmitter<File> e) throws Exception {
                //通过gilde下载得到file文件,这里需要注意android.permission.INTERNET权限
                //XLog.dr("downloadImageToFile  subscribe   " + imgUrl);
                e.onNext(Glide.with(mContext)
                        .load(imgUrl)
                        .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .get());
                e.onComplete();
            }
        }).subscribeOn(Schedulers.io()).retry(6)
                .observeOn(Schedulers.newThread())
                .subscribe(new Consumer<File>() {
                    @Override
                    public void accept(File file) throws Exception {
                        //获取到下载得到的图片，进行本地保存
                        File destFile = new File(mContext.getFilesDir().getPath() + File.separator + fileName);
                        //把gilde下载得到图片复制到定义好的目录中去
                        try {
                            copy(file, destFile);
                            callback.onSuccess(imgUrl, destFile.getAbsolutePath());
                        } catch (Exception e) {
                            e.printStackTrace();
                            callback.onError("链接Exception  " + imgUrl + "    " + e.getMessage());
                        }
                        // 最后通知图库更新
                       /* sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                Uri.fromFile(new File(destFile.getPath()))));*/
                    }

                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        callback.onError("链接Throwable  " + imgUrl + "    " + throwable.getMessage());
                    }
                });
        d.dispose();
    }


    /**
     * 复制文件
     *
     * @param source 输入文件
     * @param target 输出文件
     */
    public static void copy(File source, File target) {
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            fileInputStream = new FileInputStream(source);
            fileOutputStream = new FileOutputStream(target);
            byte[] buffer = new byte[1024];
            while (fileInputStream.read(buffer) > 0) {
                fileOutputStream.write(buffer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fileInputStream.close();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public interface DownloadImageCallback {
        void onSuccess(String url, String file);

        void onError(String error);
    }
}
