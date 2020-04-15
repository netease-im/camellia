package com.netease.nim.camellia.hbase.resource;

import com.netease.nim.camellia.core.model.Resource;

/**
 *
 * Created by caojiajun on 2020/3/20.
 */
public class HBaseResource extends Resource {

    public static final String prefix = "hbase://";

    private String zk;
    private String zkParent;

    public HBaseResource(String zk, String zkParent) {
        this.zk = zk;
        this.zkParent = zkParent;
        setUrl(prefix + zk + zkParent);
    }

    public String getZk() {
        return zk;
    }

    public void setZk(String zk) {
        this.zk = zk;
    }

    public String getZkParent() {
        return zkParent;
    }

    public void setZkParent(String zkParent) {
        this.zkParent = zkParent;
    }
}
