package com.netease.nim.camellia.redis.resource;

import com.netease.nim.camellia.core.api.ResourceTableUpdater;
import com.netease.nim.camellia.core.model.ResourceTable;


/**
 *
 * Created by caojiajun on 2021/7/29
 */
public abstract class RedisTemplateResourceTableUpdater extends ResourceTableUpdater {

    @Override
    public void checkResourceTable(ResourceTable resourceTable) {
        super.checkResourceTable(resourceTable);
        RedisClientResourceUtil.checkResourceTable(resourceTable);
    }

}
