package com.netease.nim.camellia.redis.proxy.samples.route;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.proxy.route.ProxyRouteConfUpdater;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 *
 * Created by caojiajun on 2021/4/21
 */
public class CustomProxyRouteConfUpdater extends ProxyRouteConfUpdater {

    private String url = "redis://@127.0.0.1:6379";

    public CustomProxyRouteConfUpdater() {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(this::update, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public ResourceTable getResourceTable(long bid, String bgroup) {
        return ReadableResourceTableUtil.parseTable(url);
    }

    private void update() {
        String newUrl = "redis://@127.0.0.2:6379";
        if (!url.equals(newUrl)) {
            url = newUrl;
            invokeUpdateResourceTableJson(1, "default", url);
        }
    }
}
