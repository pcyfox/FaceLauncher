package com.taike.lib_utils;

import android.graphics.Bitmap;
import android.view.View;

import androidx.annotation.WorkerThread;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ViewUtils {
    private ViewUtils() {
    }

    public static Bitmap createBitmap(View view) {
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();  //启用DrawingCache并创建位图
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache()); //创建一个DrawingCache的拷贝，因为DrawingCache得到的位图在禁用后会被回收
        view.setDrawingCacheEnabled(false);  //禁用DrawingCahce否则会影响性能
        return bitmap;
    }

    public static void saveImage(final String path, final Bitmap bitmap, final OnSaveListener listener) {
        if (bitmap != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        saveToLocal(bitmap, path);
                        if (listener != null) {
                            listener.onSaveFinish();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    @WorkerThread
    public static void saveToLocal(Bitmap bitmap, String filepath) throws IOException {
        File file = new File(filepath);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)) {
                out.flush();
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public interface OnSaveListener {
        void onSaveFinish();
    }

}
