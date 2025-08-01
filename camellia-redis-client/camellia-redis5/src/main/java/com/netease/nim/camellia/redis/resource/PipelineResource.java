package com.netease.nim.camellia.redis.resource;


import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.pipeline.PipelineFactory;

/**
 *
 * Created by caojiajun on 2019/7/22.
 */
public class PipelineResource extends Resource {

    private PipelineFactory pipelineFactory;
    private final Resource resource;

    private CamelliaRedisEnv redisEnv;

    public PipelineResource(Resource resource) {
        this.resource = resource;
    }

    public String getUrl() {
        return resource.getUrl();
    }

    public Resource getResource() {
        return resource;
    }

    public PipelineFactory getPipelineFactory() {
        return pipelineFactory;
    }

    public void setPipelineFactory(PipelineFactory pipelineFactory) {
        this.pipelineFactory = pipelineFactory;
    }

    public CamelliaRedisEnv getRedisEnv() {
        return redisEnv;
    }

    public void setRedisEnv(CamelliaRedisEnv redisEnv) {
        this.redisEnv = redisEnv;
    }
}
