package com.netease.nim.camellia.feign.route;

import com.netease.nim.camellia.core.model.ResourceTable;

/**
 * Created by caojiajun on 2022/3/2
 */
public class SimpleFeignResourceTableProvider extends FeignResourceTableProvider {

    private final ResourceTable resourceTable;

    public SimpleFeignResourceTableProvider(ResourceTable resourceTable) {
        checkResourceTable(resourceTable);
        this.resourceTable = resourceTable;
    }

    @Override
    public ResourceTable getResourceTable() {
        return resourceTable;
    }
}
