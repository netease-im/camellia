package com.netease.nim.camellia.core.api;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by caojiajun on 2022/3/2
 */
public abstract class ResourceTableUpdater {

    private static final Logger logger = LoggerFactory.getLogger(ResourceTableUpdater.class);
    private final Set<ResourceTableUpdateCallback> callbackSet = new HashSet<>();

    public abstract ResourceTable getResourceTable();

    public void invokeUpdateResourceTable(ResourceTable resourceTable) {
        if (callbackSet.isEmpty()) return;
        checkResourceTable(resourceTable);
        for (ResourceTableUpdateCallback callback : callbackSet) {
            try {
                callback.callback(resourceTable);
            } catch (Exception e) {
                logger.error("callback error", e);
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info("ResourceTableUpdater resourceTable update, resourceTable = {}", ReadableResourceTableUtil.readableResourceTable(resourceTable));
        }
    }

    //you can override
    public void checkResourceTable(ResourceTable resourceTable) {

    }

    public void invokeUpdateResourceTableJson(String json) {
        ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(json);
        invokeUpdateResourceTable(resourceTable);
    }

    public synchronized void addCallback(ResourceTableUpdateCallback callback) {
        this.callbackSet.add(callback);
    }
}
