package com.taike.lib_log.printer;

import com.google.gson.annotations.SerializedName;


public class PrintLogReq extends BasePrintLogReq {

    public PrintLogReq(String client, String uid, String clientId, boolean isDebug, String serviceType, String version) {
        this.client = client;
        this.uid = uid;
        this.clientId = clientId;
        this.isDebug = isDebug;
        this.serviceType = serviceType;
        this.version = version;
    }

    public PrintLogReq(String client, String clientId, boolean isDebug, String serviceType, String version) {
        this(client, "", clientId, isDebug, serviceType, version);
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
    @SerializedName("client")
    private String client;
    @SerializedName("uid")
    private String uid;
    @SerializedName("client_id")
    private String clientId;
    @SerializedName("is_debug")//是否为测试版本
    private boolean isDebug;
    @SerializedName("service_type")//连接的服务器列席
    private String serviceType;
    @SerializedName("version")
    private String version;//app 版本号


    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getUid() {
        return uid;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public void setDebug(boolean debug) {
        isDebug = debug;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "PrintLogReq{" +
                "client='" + client + '\'' +
                ", uid='" + uid + '\'' +
                ", clientId='" + clientId + '\'' +
                ", tag='" + tag + '\'' +
                ", msg='" + msg + '\'' +
                ", ts='" + ts + '\'' +
                '}';
    }
}
