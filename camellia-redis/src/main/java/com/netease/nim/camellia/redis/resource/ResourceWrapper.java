package com.netease.nim.camellia.redis.resource;


import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;

/**
 *
 * Created by caojiajun on 2019/8/2.
 */
public class ResourceWrapper extends Resource {

    private CamelliaRedisEnv env;
    private final Resource resource;

    public ResourceWrapper(Resource resource) {
        this.resource = resource;
    }

    public Resource getResource() {
        return resource;
    }

    public String getUrl() {
        return resource.getUrl();
    }

    public CamelliaRedisEnv getEnv() {
        return env;
    }

    public void setEnv(CamelliaRedisEnv env) {
        this.env = env;
    }
}
