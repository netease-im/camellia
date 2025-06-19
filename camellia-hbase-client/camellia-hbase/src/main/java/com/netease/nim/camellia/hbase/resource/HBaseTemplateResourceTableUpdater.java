package com.netease.nim.camellia.hbase.resource;

import com.netease.nim.camellia.core.api.ResourceTableUpdater;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.hbase.util.HBaseResourceUtil;


public abstract class HBaseTemplateResourceTableUpdater extends ResourceTableUpdater {

    @Override
    public void checkResourceTable(ResourceTable resourceTable) {
        super.checkResourceTable(resourceTable);
        HBaseResourceUtil.checkResourceTable(resourceTable);
    }

}
