package com.netease.nim.camellia.redis.base.resource;

import com.netease.nim.camellia.core.model.Resource;

/**
 * Created by caojiajun on 2025/1/10
 */
public class RedisLocalStorageResource extends Resource {

    private final String dir;

    public RedisLocalStorageResource(String dir) {
        this.dir = dir;
        setUrl(RedisType.LocalStorage.getPrefix() + dir);
    }

    public String getDir() {
        return dir;
    }
}
