package com.netease.nim.camellia.redis.intercept;

import com.netease.nim.camellia.core.model.Resource;

/**
 * Created by caojiajun on 2023/6/30
 */
public class InterceptContext {

    private final Resource resource;
    private final byte[] key;
    private final String command;
    private final boolean pipeline;

    public InterceptContext(Resource resource, byte[] key, String command, boolean pipeline) {
        this.resource = resource;
        this.key = key;
        this.command = command;
        this.pipeline = pipeline;
    }

    public Resource getResource() {
        return resource;
    }

    public byte[] getKey() {
        return key;
    }

    public String getCommand() {
        return command;
    }

    public boolean isPipeline() {
        return pipeline;
    }
}
