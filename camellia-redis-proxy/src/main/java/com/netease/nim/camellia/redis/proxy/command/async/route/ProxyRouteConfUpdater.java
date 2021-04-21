package com.netease.nim.camellia.redis.proxy.command.async.route;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.proxy.command.async.AsyncCamelliaRedisTemplate;
import com.netease.nim.camellia.redis.resource.RedisResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2021/4/21
 */
public abstract class ProxyRouteConfUpdater {

    private static final Logger logger = LoggerFactory.getLogger(ProxyRouteConfUpdater.class);
    private final ConcurrentHashMap<String, AsyncCamelliaRedisTemplate> map = new ConcurrentHashMap<>();

    public abstract ResourceTable getResourceTable(long bid, String bgroup);

    /**
     * 动态更新ResourceTable，ProxyRouteConfUpdater的子类在检测到路由信息发生变更的时候，需要回调本方法，则对应的AsyncCamelliaRedisTemplate会更新对应的路由
     * @param bid bid
     * @param bgroup bgroup
     * @param resourceTable ResourceTable
     */
    public final void invokeUpdateResourceTable(long bid, String bgroup, ResourceTable resourceTable) {
        RedisResourceUtil.checkResourceTable(resourceTable);
        AsyncCamelliaRedisTemplate template = map.get(bid + "|" + bgroup);
        if (template == null) return;
        template.getCallback().callback(resourceTable);
        if (logger.isInfoEnabled()) {
            logger.info("resourceTable update, bid = {}, bgroup = {}, resourceTable = {}", bid, bgroup, ReadableResourceTableUtil.readableResourceTable(resourceTable));
        }
    }

    /**
     * 动态更新ResourceTable
     * @param resourceTable ResourceTable
     */
    public final void invokeUpdateResourceTable(ResourceTable resourceTable) {
        invokeUpdateResourceTable(-1, "default", resourceTable);
    }

    /**
     * 动态更新ResourceTable
     * @param bid bid
     * @param bgroup bgroup
     * @param json ResourceTableJson
     */
    public final void invokeUpdateResourceTableJson(long bid, String bgroup, String json) {
        ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(json);
        invokeUpdateResourceTable(bid, bgroup, resourceTable);
    }

    /**
     * 动态更新ResourceTable
     * @param json ResourceTableJson
     */
    public final void invokeUpdateResourceTableJson(String json) {
        invokeUpdateResourceTableJson(-1, "default", json);
    }

    /**
     * 添加AsyncCamelliaRedisTemplate给ProxyRouteConfUpdater去管理
     * @param bid bid
     * @param bgroup bgroup
     * @param template AsyncCamelliaRedisTemplate
     */
    public final void addAsyncCamelliaRedisTemplate(long bid, String bgroup, AsyncCamelliaRedisTemplate template) {
        map.put(bid + "|" + bgroup, template);
    }
}
