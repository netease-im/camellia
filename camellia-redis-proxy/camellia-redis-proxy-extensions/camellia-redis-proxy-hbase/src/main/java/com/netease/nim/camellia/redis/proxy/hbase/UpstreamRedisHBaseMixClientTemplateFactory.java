package com.netease.nim.camellia.redis.proxy.hbase;

import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplate;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2023/1/31
 */
public class UpstreamRedisHBaseMixClientTemplateFactory implements IUpstreamClientTemplateFactory {

    private static final Logger logger = LoggerFactory.getLogger(UpstreamRedisHBaseMixClientTemplateFactory.class);

    private final UpstreamRedisHBaseMixClientTemplate template;

    public UpstreamRedisHBaseMixClientTemplateFactory(CamelliaRedisTemplate redisTemplate, CamelliaHBaseTemplate hBaseTemplate) {
        try {
            logger.info("try init UpstreamRedisHBaseMixClientTemplateFactory");
            template = new UpstreamRedisHBaseMixClientTemplate(redisTemplate, hBaseTemplate);
            logger.info("UpstreamRedisHBaseMixClientTemplateFactory init success");
        } catch (Throwable e) {
            logger.error("UpstreamRedisHBaseMixClientTemplateFactory init error", e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public IUpstreamClientTemplate getOrInitialize(Long bid, String bgroup) {
        return template;
    }

    @Override
    public CompletableFuture<IUpstreamClientTemplate> getOrInitializeAsync(Long bid, String bgroup) {
        return IUpstreamClientTemplateFactory.wrapper(template);
    }

    @Override
    public IUpstreamClientTemplate tryGet(Long bid, String bgroup) {
        return template;
    }

    @Override
    public boolean isMultiTenantsSupport() {
        return false;
    }

    @Override
    public List<IUpstreamClientTemplate> getAll() {
        return Collections.singletonList(template);
    }
}
