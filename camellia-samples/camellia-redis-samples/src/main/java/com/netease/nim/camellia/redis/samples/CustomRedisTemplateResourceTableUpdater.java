package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.redis.resource.RedisTemplateResourceTableUpdater;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2021/7/30
 */
public class CustomRedisTemplateResourceTableUpdater extends RedisTemplateResourceTableUpdater {

    private String url = "redis://@127.0.0.1:6379";

    public CustomRedisTemplateResourceTableUpdater() {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(this::checkUpdate, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    public ResourceTable getResourceTable() {
        //用于初始化ResourceTable
        return ResourceTableUtil.simpleTable(new Resource(url));
    }

    private void checkUpdate() {
        //从你的配置中心获取配置，或者监听配置的变更
        String newUrl = "redis://@127.0.0.2:6379";
        if (!url.equals(newUrl)) {
            //如果配置发生了变更，则回调告诉CamelliaRedisTemplate有更新
            url = newUrl;
            ResourceTable resourceTable = ResourceTableUtil.simpleTable(new Resource(url));
            invokeUpdateResourceTable(resourceTable);
        }
    }
}
