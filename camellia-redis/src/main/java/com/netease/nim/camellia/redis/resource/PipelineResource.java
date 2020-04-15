package com.netease.nim.camellia.redis.resource;


import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.pipeline.RedisClientPool;
import com.netease.nim.camellia.redis.pipeline.ResponseQueable;

/**
 *
 * Created by caojiajun on 2019/7/22.
 */
public class PipelineResource extends Resource {

    private RedisClientPool clientPool;
    private ResponseQueable queable;
    private Resource resource;

    public PipelineResource(Resource resource) {
        this.resource = resource;
    }

    public String getUrl() {
        return resource.getUrl();
    }

    public Resource getResource() {
        return resource;
    }

    public RedisClientPool getClientPool() {
        return clientPool;
    }

    public void setClientPool(RedisClientPool clientPool) {
        this.clientPool = clientPool;
    }

    public ResponseQueable getQueable() {
        return queable;
    }

    public void setQueable(ResponseQueable queable) {
        this.queable = queable;
    }
}
