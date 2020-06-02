package com.taike.lib_network;


import androidx.annotation.Nullable;

final public class Utils {
    public static <T> T checkNotNull(@Nullable T object, String message) {
        if (object == null) {
            throw new NullPointerException(message);
        }
        return object;
    }
}
