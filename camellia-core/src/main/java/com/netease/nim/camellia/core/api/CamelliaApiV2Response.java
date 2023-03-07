package com.netease.nim.camellia.core.api;


/**
 * Created by caojiajun on 2023/3/7
 */
public class CamelliaApiV2Response {

    private int code;
    private String routeTable;
    private String md5;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getRouteTable() {
        return routeTable;
    }

    public void setRouteTable(String routeTable) {
        this.routeTable = routeTable;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }
}
