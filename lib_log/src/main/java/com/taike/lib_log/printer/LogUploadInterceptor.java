package com.taike.lib_log.printer;

public interface LogUploadInterceptor {
    String upload(final int logLevel, final String tag, final String msg);
}
