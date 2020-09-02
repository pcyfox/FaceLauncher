package com.taike.lib_log.printer;

import com.google.gson.annotations.SerializedName;


public class PrintLogReq extends BasePrintLogReq {
    @SerializedName("client_name")
    private String clientName;
    @SerializedName("user_id")
    private String uid;
    @SerializedName("device_id")
    private String clientId;
    @SerializedName("is_debug")//是否为测试版本
    private boolean isDebug;
    @SerializedName("server_info")//连接的服务器信息
    private String serviceType;
    @SerializedName("app_version_code")
    private String version;//app 版本号

    public void setUid(String uid) {
        this.uid = uid;
    }

    public PrintLogReq(String clientName, String uid, String clientId, boolean isDebug, String serviceType, String version) {
        this.clientName = clientName;
        this.uid = uid;
        this.clientId = clientId;
        this.isDebug = isDebug;
        this.serviceType = serviceType;
        this.version = version;
    }

    public PrintLogReq(String client, String clientId, boolean isDebug, String serviceType, String version) {
        this(client, "", clientId, isDebug, serviceType, version);
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

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }
}
