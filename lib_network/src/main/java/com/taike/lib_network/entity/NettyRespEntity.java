package com.taike.lib_network.entity;

public class NettyRespEntity<D> {
    private String from;
    private int id;
    private String action;
    private D data;
    private int code;
    private String desc;

    public NettyRespEntity() {
    }

    public NettyRespEntity(String from, int id, String action, D data, int code, String desc) {
        this.from = from;
        this.id = id;
        this.action = action;
        this.data = data;
        this.code = code;
        this.desc = desc;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public D getData() {
        return data;
    }

    public void setData(D data) {
        this.data = data;
    }


    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
