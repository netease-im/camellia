package com.netease.nim.camellia.redis.proxy.monitor.model;

/**
 * Created by caojiajun on 2024/5/21
 */
public class KvGcStats {

    private String namespace;
    private long scanMetaKeys;
    private long deleteMetaKeys;
    private long scanSubKeys;
    private long deleteSubKeys;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public long getScanMetaKeys() {
        return scanMetaKeys;
    }

    public void setScanMetaKeys(long scanMetaKeys) {
        this.scanMetaKeys = scanMetaKeys;
    }

    public long getScanSubKeys() {
        return scanSubKeys;
    }

    public void setScanSubKeys(long scanSubKeys) {
        this.scanSubKeys = scanSubKeys;
    }

    public long getDeleteMetaKeys() {
        return deleteMetaKeys;
    }

    public void setDeleteMetaKeys(long deleteMetaKeys) {
        this.deleteMetaKeys = deleteMetaKeys;
    }

    public long getDeleteSubKeys() {
        return deleteSubKeys;
    }

    public void setDeleteSubKeys(long deleteSubKeys) {
        this.deleteSubKeys = deleteSubKeys;
    }
}
