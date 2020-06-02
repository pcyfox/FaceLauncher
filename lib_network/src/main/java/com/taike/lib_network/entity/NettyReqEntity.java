package com.taike.lib_network.entity;

public class NettyReqEntity<D> {
    private String from;
    private int id;
    private String action;
    private D data;



    public NettyReqEntity(String from, int id, String action, D data) {
        this.from = from;
        this.id = id;
        this.action = action;
        this.data = data;
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


}
