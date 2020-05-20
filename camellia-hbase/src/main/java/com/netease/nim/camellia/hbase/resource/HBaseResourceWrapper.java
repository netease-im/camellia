package com.netease.nim.camellia.hbase.resource;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.hbase.CamelliaHBaseEnv;

/**
 *
 * Created by caojiajun on 2020/3/20.
 */
public class HBaseResourceWrapper extends Resource {

    private CamelliaHBaseEnv env;
    private final Resource resource;

    public HBaseResourceWrapper(Resource resource) {
        this.resource = resource;
    }

    public Resource getResource() {
        return resource;
    }

    public String getUrl() {
        return resource.getUrl();
    }

    public CamelliaHBaseEnv getEnv() {
        return env;
    }

    public void setEnv(CamelliaHBaseEnv env) {
        this.env = env;
    }
}
