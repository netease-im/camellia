package com.netease.nim.camellia.core.api;

import java.util.Map;

/**
 * Created by caojiajun on 2023/3/7
 */
public class CamelliaConfigResponse {
    private int code;
    private String md5;
    private Map<String, String> conf;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public Map<String, String> getConf() {
        return conf;
    }

    public void setConf(Map<String, String> conf) {
        this.conf = conf;
    }
}
