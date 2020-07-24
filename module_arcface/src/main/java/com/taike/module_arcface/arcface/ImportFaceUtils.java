package com.taike.module_arcface.arcface;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import com.arcsoft.imageutil.ArcSoftImageFormat;
import com.arcsoft.imageutil.ArcSoftImageUtil;
import com.arcsoft.imageutil.ArcSoftImageUtilError;
import com.elvishew.xlog.XLog;
import com.taike.module_test.R;

import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImportFaceUtils {
    private static final String TAG = "ImportFaceUtils";
    //注册图所在的目录
    public static final String ROOT_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "TKArcFace";
    //  private static final String REGISTER_DIR = ROOT_DIR + File.separator + "register";
    private static final String REGISTER_FAILED_DIR = ROOT_DIR + File.separator + "failed";
    private ExecutorService executorService;
    private static ImportFaceUtils importface = null;

    public static ImportFaceUtils getInstance() {
        if (importface == null) {
            synchronized (FaceServer.class) {
                if (importface == null) {
                    importface = new ImportFaceUtils();
                    FaceServer.ROOT_PATH = ROOT_DIR;
                }
            }
        }
        return importface;
    }

    public void init(Context context) {
        context = context.getApplicationContext();
        File file = new File(ROOT_DIR);
        if (!file.exists()) {
            file.mkdir();
        }

//        File filer = new File(REGISTER_DIR);
//        if (!filer.exists()) {
//            filer.mkdir();
//        }

        File filef = new File(REGISTER_FAILED_DIR);
        if (!filef.exists()) {
            filef.mkdir();
        }
        FaceServer.getInstance().init(context);
    }

    public void unInit() {
        if (executorService != null) {
            if (!executorService.isShutdown()) {
                executorService.shutdownNow();
            }
        }
        FaceServer.getInstance().unInit();
    }


    //注册人脸
    public void doRegister(final Context context, final String faceDir, final ImportFaceCallBack importFaceCallBack) {
        Log.d(TAG, "doRegister() called with: context = [" + context + "], faceDir = [" + faceDir + "]");
        executorService = Executors.newSingleThreadExecutor();
        File dir = new File(faceDir);
        if (!dir.exists() || !dir.isDirectory()) {
            importFaceCallBack.onError(context.getString(R.string.batch_process_path_is_not_dir, faceDir));
            return;
        }
        final File[] jpgFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(FaceServer.IMG_SUFFIX);
            }
        });
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                final int totalCount = jpgFiles.length;
                int successCount = 0;
                importFaceCallBack.onStart();
                for (int i = 0; i < totalCount; i++) {
                    final int finalI = i;
                    importFaceCallBack.onProgress(finalI);
                    final File jpgFile = jpgFiles[i];
                    Bitmap bitmap = BitmapFactory.decodeFile(jpgFile.getAbsolutePath());
                    if (bitmap == null) {
                        Log.e(TAG, "doRegister() decodeFile fail " + jpgFile.getAbsolutePath());
                        File failedFile = new File(REGISTER_FAILED_DIR + File.separator + jpgFile.getName());
                        if (!failedFile.getParentFile().exists()) {
                            failedFile.getParentFile().mkdirs();
                        }
                        jpgFile.renameTo(failedFile);
                        continue;
                    }
                    bitmap = ArcSoftImageUtil.getAlignedBitmap(bitmap, true);
                    if (bitmap == null) {
                        File failedFile = new File(REGISTER_FAILED_DIR + File.separator + jpgFile.getName());
                        if (!failedFile.getParentFile().exists()) {
                            failedFile.getParentFile().mkdirs();
                        }
                        jpgFile.renameTo(failedFile);
                        continue;
                    }
                    byte[] bgr24 = ArcSoftImageUtil.createImageData(bitmap.getWidth(), bitmap.getHeight(), ArcSoftImageFormat.BGR24);
                    int transformCode = ArcSoftImageUtil.bitmapToImageData(bitmap, bgr24, ArcSoftImageFormat.BGR24);
                    if (transformCode != ArcSoftImageUtilError.CODE_SUCCESS) {
                        return;
                    }
                    boolean success = FaceServer.getInstance().registerBgr24(context, bgr24, bitmap.getWidth(), bitmap.getHeight(), jpgFile.getName().substring(0, jpgFile.getName().lastIndexOf(".")));
                    if (!success) {
                        File failedFile = new File(REGISTER_FAILED_DIR + File.separator + jpgFile.getName());
                        if (!failedFile.getParentFile().exists()) {
                            failedFile.getParentFile().mkdirs();
                        }
                        jpgFile.renameTo(failedFile);
                    } else {
                        successCount++;
                    }
                }
                final int finalSuccessCount = successCount;
                importFaceCallBack.onFinish(context.getString(R.string.batch_process_finished_info, totalCount, finalSuccessCount, totalCount - finalSuccessCount, REGISTER_FAILED_DIR));
                //XLog.d("run: " + executorService.isShutdown());
            }
        });
    }

    //清除人脸数据
    public void clearFaces(Context context, ClearCallBack clearCallBack) {
        int faceNum = FaceServer.getInstance().getFaceNumber(context);
        //XLog.d("'clearFaces "+faceNum);
        if (faceNum != 0) {
            int deleteCount = FaceServer.getInstance().clearAllFaces(context);
        }
        clearCallBack.onClear();
    }

    public static abstract class ImportFaceCallBack {

        public void onFinish(String result) {
        }


        public void onProgress(int pro) {
        }


        public void onStart() {
        }


        public void onError(String error) {
        }

    }

    public interface ClearCallBack {
        void onClear();
    }


}
