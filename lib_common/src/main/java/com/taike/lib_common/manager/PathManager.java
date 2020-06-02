package com.taike.lib_common.manager;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.PathUtils;


public class PathManager {
    private static PathManager mInstance = null;
    public static PathManager get() {
        if (mInstance == null) {
            synchronized (PathManager.class) {
                if (mInstance == null) {
                    mInstance = new PathManager();
                }
            }
        }
        return mInstance;
    }
    private PathManager() {

    }

    private final static String TAG = "PathManager";

    public void init(Context appContext) {
        Log.i(TAG, String.format("%s\n%s\n%s\n%s\n",
                Environment.getExternalStorageDirectory().getPath(),
                Environment.getDataDirectory().getPath(),
                Environment.getExternalStorageDirectory().getPath(),
                Environment.getRootDirectory().getPath()));

        String appDataPath = PathUtils.getExternalAppFilesPath() + "/";
        mDataRootPath = createDir(appDataPath, "xtv");
        mLogPath = createDir(mDataRootPath,  "log");
        mCachePath = createDir(mDataRootPath, "cache");
    }

    private String createDir(String parent, String dirName) {
        String path = parent + dirName + "/";
        FileUtils.createOrExistsDir(path);
        if (!FileUtils.isDir(path)) {
            Log.e(TAG, path + " create fail");
        } else {
            Log.i(TAG, path + " exists");
        }
        return path;
    }

    public String getDataRootPath() {
        return mDataRootPath;
    }
    public String getLogPath() {
        return mLogPath;
    }
    public String getCachePath() {
        return mCachePath;
    }
    private String mDataRootPath;
    private String mLogPath;
    private String mCachePath;
}