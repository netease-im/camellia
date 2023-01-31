package com.netease.nim.camellia.redis.proxy.hbase;

import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplate;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplateChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2023/1/31
 */
public class UpstreamRedisHBaseMixClientTemplateChooser implements IUpstreamClientTemplateChooser {

    private static final Logger logger = LoggerFactory.getLogger(UpstreamRedisHBaseMixClientTemplateChooser.class);

    private final UpstreamRedisHBaseMixClientTemplate template;

    public UpstreamRedisHBaseMixClientTemplateChooser(CamelliaRedisTemplate redisTemplate, CamelliaHBaseTemplate hBaseTemplate) {
        template = new UpstreamRedisHBaseMixClientTemplate(redisTemplate, hBaseTemplate);
        logger.info("UpstreamRedisHBaseMixClientTemplateChooser init success");
    }

    @Override
    public IUpstreamClientTemplate choose(Long bid, String bgroup) {
        return template;
    }

    @Override
    public CompletableFuture<IUpstreamClientTemplate> chooseAsync(Long bid, String bgroup) {
        return IUpstreamClientTemplateChooser.wrapper(template);
    }

    @Override
    public boolean isMultiTenancySupport() {
        return false;
    }
}
