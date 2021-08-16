package com.netease.nim.camellia.redis.resource;

import com.netease.nim.camellia.core.api.ResourceTableUpdateCallback;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;


/**
 *
 * Created by caojiajun on 2021/7/29
 */
public abstract class RedisTemplateResourceTableUpdater {

    private static final Logger logger = LoggerFactory.getLogger(RedisTemplateResourceTableUpdater.class);
    private final Set<ResourceTableUpdateCallback> callbackSet = new HashSet<>();

    public abstract ResourceTable getResourceTable();

    public final void invokeUpdateResourceTable(ResourceTable resourceTable) {
        if (callbackSet.isEmpty()) return;
        RedisResourceUtil.checkResourceTable(resourceTable);
        for (ResourceTableUpdateCallback callback : callbackSet) {
            try {
                callback.callback(resourceTable);
            } catch (Exception e) {
                logger.error("callback error", e);
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info("CamelliaRedisTemplate resourceTable update, resourceTable = {}", ReadableResourceTableUtil.readableResourceTable(resourceTable));
        }
    }

    public final void invokeUpdateResourceTableJson(String json) {
        ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(json);
        invokeUpdateResourceTable(resourceTable);
    }

    public final synchronized void addCallback(ResourceTableUpdateCallback callback) {
        this.callbackSet.add(callback);
    }
}
