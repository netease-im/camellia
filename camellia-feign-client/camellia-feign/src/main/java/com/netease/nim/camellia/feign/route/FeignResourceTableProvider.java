package com.netease.nim.camellia.feign.route;

import com.netease.nim.camellia.core.api.ResourceTableProvider;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.feign.resource.FeignResourceUtils;


/**
 * Created by caojiajun on 2022/3/2
 */
public abstract class FeignResourceTableProvider extends ResourceTableProvider {

    @Override
    public void checkResourceTable(ResourceTable resourceTable) {
        FeignResourceUtils.checkResourceTable(resourceTable);
    }
}
