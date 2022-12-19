package com.netease.nim.camellia.cache.core;

import java.io.Serializable;

public class NullCache implements Serializable {

    public static final NullCache INSTANCE = new NullCache();

    private int id = 0;

    private NullCache() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
