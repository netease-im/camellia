package com.netease.nim.camellia.redis.proxy.samples;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.proxy.command.async.route.ProxyRouteConfUpdater;


/**
 *
 * Created by caojiajun on 2021/4/21
 */
public class CustomProxyRouteConfUpdater extends ProxyRouteConfUpdater {

    @Override
    public ResourceTable getResourceTable(long bid, String bgroup) {
        return ReadableResourceTableUtil.parseTable("redis://@127.0.0.1:6379");
    }
}
