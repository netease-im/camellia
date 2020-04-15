package com.netease.nim.camellia.core.api;


import com.netease.nim.camellia.core.model.ResourceTable;

/**
 *
 * Created by caojiajun on 2019/5/17.
 */
public class CamelliaApiResponse {

    private int code;
    private ResourceTable resourceTable;
    private String md5;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public ResourceTable getResourceTable() {
        return resourceTable;
    }

    public void setResourceTable(ResourceTable resourceTable) {
        this.resourceTable = resourceTable;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }
}
