package com.taike.lib_log.printer;

import com.google.gson.annotations.SerializedName;

public class  BasePrintLogReq {

    public void initBaseLogReq(String tag, String msg, String ts) {
        this.tag = tag;
        this.msg = msg;
        this.ts = ts;
    }

    public void initBaseLogReq(String ts) {
        this.ts = ts;
    }

    @SerializedName("tag")
    protected String tag;
    @SerializedName("msg")
    protected String msg;
    @SerializedName("ts")
    protected String ts;//必要

    @Override
    public String toString() {
        return "BasePrintLogReq{" +
                "tag='" + tag + '\'' +
                ", msg='" + msg + '\'' +
                ", ts='" + ts + '\'' +
                '}';
    }
}
