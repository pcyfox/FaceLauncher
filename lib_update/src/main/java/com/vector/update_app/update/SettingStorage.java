package com.vector.update_app.update;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingStorage {
    private static SettingStorage mSettingManager = new SettingStorage();

    public static SettingStorage get() {
        return mSettingManager;
    }

    public void init(Context appContext) {
        if (mSharedPerferences == null) {
            mSharedPerferences = appContext.getSharedPreferences("xphone", Context.MODE_PRIVATE);
        }
    }

    public void write(String key, boolean value) {
        SharedPreferences.Editor editor = mSharedPerferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public void write(String key, String value) {
        SharedPreferences.Editor editor = mSharedPerferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public void write(String key, int value) {
        SharedPreferences.Editor editor = mSharedPerferences.edit();
        editor.putInt(key, value);
        editor.apply();

    }

    public void write(String key, long value) {
        SharedPreferences.Editor editor = mSharedPerferences.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public void write(String key, float value) {
        SharedPreferences.Editor editor = mSharedPerferences.edit();
        editor.putFloat(key, value);
        editor.apply();
    }

    public boolean read(String key, boolean defultValue) {
        return mSharedPerferences.getBoolean(key, defultValue);
    }

    public String read(String key, String defultValue) {
        return mSharedPerferences.getString(key, defultValue);
    }

    public int read(String key, int defultValue) {
        return mSharedPerferences.getInt(key, defultValue);
    }

    public long read(String key, long defultValue) {
        return mSharedPerferences.getLong(key, defultValue);
    }

    public float read(String key, float defultValue) {
        return mSharedPerferences.getFloat(key, defultValue);
    }


    private SharedPreferences mSharedPerferences;
}


