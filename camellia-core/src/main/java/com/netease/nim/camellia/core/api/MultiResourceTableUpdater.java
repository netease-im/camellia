package com.netease.nim.camellia.core.api;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2022/3/2
 */
public abstract class MultiResourceTableUpdater {

    private static final Logger logger = LoggerFactory.getLogger(MultiResourceTableUpdater.class);
    private final ConcurrentHashMap<String, ResourceTableUpdateCallback> updateCallbackMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ResourceTableRemoveCallback> removeCallbackMap = new ConcurrentHashMap<>();

    public abstract ResourceTable getResourceTable(long bid, String bgroup);

    /**
     * 动态更新ResourceTable，MultiResourceTableUpdater的子类在检测到路由信息发生变更的时候，需要回调本方法，则会更新对应的路由
     * @param bid bid
     * @param bgroup bgroup
     * @param resourceTable ResourceTable
     */
    public void invokeUpdateResourceTable(long bid, String bgroup, ResourceTable resourceTable) {
        checkResourceTable(resourceTable);
        ResourceTableUpdateCallback callback = updateCallbackMap.get(bid + "|" + bgroup);
        if (callback != null) {
            callback.callback(resourceTable);
        }
        if (logger.isInfoEnabled()) {
            logger.info("resourceTable update, bid = {}, bgroup = {}, resourceTable = {}", bid, bgroup, ReadableResourceTableUtil.readableResourceTable(resourceTable));
        }
    }

    //you can override
    public void checkResourceTable(ResourceTable resourceTable) {

    }

    /**
     * 动态更新ResourceTable
     * @param bid bid
     * @param bgroup bgroup
     * @param json ResourceTableJson
     */
    public void invokeUpdateResourceTableJson(long bid, String bgroup, String json) {
        ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(json);
        invokeUpdateResourceTable(bid, bgroup, resourceTable);
    }

    /**
     * 动态删除ResourceTable
     * @param bid bid
     * @param bgroup bgroup
     */
    public void invokeRemoveResourceTable(long bid, String bgroup) {
        ResourceTableRemoveCallback callback = removeCallbackMap.get(bid + "|" + bgroup);
        if (callback != null) {
            callback.callback();
        }
        if (logger.isInfoEnabled()) {
            logger.info("resourceTable remove, bid = {}, bgroup = {}", bid, bgroup);
        }
    }

    /**
     * 添加callback
     * @param bid bid
     * @param bgroup bgroup
     * @param addCallback ResourceTableUpdateCallback
     * @param removeCallback ResourceTableRemoveCallback
     */
    public void addCallback(long bid, String bgroup, ResourceTableUpdateCallback addCallback, ResourceTableRemoveCallback removeCallback) {
        if (addCallback != null) {
            updateCallbackMap.put(bid + "|" + bgroup, addCallback);
        }
        if (removeCallback != null) {
            removeCallbackMap.put(bid + "|" + bgroup, removeCallback);
        }
    }

}
