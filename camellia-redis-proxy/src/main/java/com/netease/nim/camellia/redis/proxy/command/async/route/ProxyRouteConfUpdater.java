package com.netease.nim.camellia.redis.proxy.command.async.route;

import com.netease.nim.camellia.core.api.MultiResourceTableUpdater;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.redis.resource.RedisResourceUtil;


/**
 * Created by caojiajun on 2021/4/21
 */
public abstract class ProxyRouteConfUpdater extends MultiResourceTableUpdater {

    @Override
    public void checkResourceTable(ResourceTable resourceTable) {
        super.checkResourceTable(resourceTable);
        RedisResourceUtil.checkResourceTable(resourceTable);
    }
}
