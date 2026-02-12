package com.netease.nim.camellia.redis.proxy.route;


import com.netease.nim.camellia.core.api.ResourceTableRemoveCallback;
import com.netease.nim.camellia.core.api.ResourceTableUpdateCallback;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.base.resource.RedisResourceUtil;
import com.netease.nim.camellia.redis.proxy.auth.ClientIdentity;
import com.netease.nim.camellia.redis.proxy.monitor.CommandMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 通过userName和password，映射到bid/bgroup
 * <p>
 * 通过bid/bgroup，映射到ResourceTable
 * <p>
 * Created by caojiajun on 2026/2/11
 */
public abstract class RouteConfProvider {

    private static final Logger logger = LoggerFactory.getLogger(RouteConfProvider.class);
    private final ConcurrentHashMap<String, ResourceTableUpdateCallback> updateCallbackMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ResourceTableRemoveCallback> removeCallbackMap = new ConcurrentHashMap<>();

    /**
     * 对应于auth或者hello命令
     * @param userName 账号
     * @param password 密码
     * @return 校验结果+ bid/bgroup
     */
    public abstract ClientIdentity auth(String userName, String password);

    /**
     * 是否需要密码
     * @return true/false
     */
    public abstract boolean isPasswordRequired();

    /**
     * 默认路由，当客户端没有声明bid/bgroup时的路由配置
     * @return route conf
     */
    public abstract String getRouteConfig();

    /**
     * 当客户端声明bid/bgroup时的路由配置
     * @param bid bid
     * @param bgroup bgroup
     * @return route conf
     */
    public abstract String getRouteConfig(long bid, String bgroup);

    /**
     * 是否支持多租户
     * @return true/false
     */
    public abstract boolean isMultiTenantsSupport();

    public CommandMonitor getCommandMonitor() {
        return null;
    }

    //
    //

    protected final void invokeUpdateResourceTable(String routeConf) {
        invokeUpdateResourceTable(-1, "default", routeConf);
    }

    protected final void invokeUpdateResourceTable(long bid, String bgroup, String routeConf) {
        ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(routeConf);
        checkResourceTable(resourceTable);
        ResourceTableUpdateCallback callback = updateCallbackMap.get(bid + "|" + bgroup);
        if (callback != null) {
            callback.callback(resourceTable);
        }
        if (logger.isInfoEnabled()) {
            logger.info("resourceTable update, bid = {}, bgroup = {}, resourceTable = {}", bid, bgroup, ReadableResourceTableUtil.readableResourceTable(resourceTable));
        }
    }

    protected final void invokeRemoveResourceTable(long bid, String bgroup) {
        String key = bid + "|" + bgroup;
        ResourceTableRemoveCallback callback = removeCallbackMap.get(key);
        if (callback != null) {
            callback.callback();
            removeCallbackMap.remove(key);
        }
        if (logger.isInfoEnabled()) {
            logger.info("resourceTable remove, bid = {}, bgroup = {}", bid, bgroup);
        }
    }

    public final void addCallback(long bid, String bgroup, ResourceTableUpdateCallback addCallback, ResourceTableRemoveCallback removeCallback) {
        String key = bid + "|" + bgroup;
        if (addCallback != null) {
            updateCallbackMap.put(key, addCallback);
        }
        if (removeCallback != null) {
            removeCallbackMap.put(key, removeCallback);
        }
    }

    public void checkResourceTable(ResourceTable resourceTable) {
        RedisResourceUtil.checkResourceTable(resourceTable);
    }

}
