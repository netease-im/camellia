package com.netease.nim.camellia.core.util;


import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.model.operation.ResourceOperation;
import com.netease.nim.camellia.core.model.operation.ResourceReadOperation;
import com.netease.nim.camellia.core.model.operation.ResourceWriteOperation;

import java.util.List;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2019/5/15.
 */
public class CheckUtil {

    public static boolean checkResourceTable(ResourceTable table) {
        if (table == null) return false;
        ResourceTable.Type type = table.getType();
        if (type == null) return false;
        switch (type) {
            case SHADING:
                return checkShardingTable(table.getShadingTable());
            case SIMPLE:
                return checkSimpleTable(table.getSimpleTable());
            default:
                return false;
        }
    }

    public static boolean checkSimpleTable(ResourceTable.SimpleTable simpleTable) {
        if (simpleTable == null) return false;
        ResourceOperation resourceOperation = simpleTable.getResourceOperation();
        return checkResourceOperation(resourceOperation);
    }

    public static boolean checkShardingTable(ResourceTable.ShadingTable table) {
        if (table == null) return false;
        int bucketSize = table.getBucketSize();
        Map<Integer, ResourceOperation> map = table.getResourceOperationMap();
        if (map == null) return false;
        if (bucketSize <= 0) return false;
        if (map.size() != bucketSize) return false;
        for (int i=0; i<bucketSize; i++) {
            ResourceOperation operation = map.get(i);
            boolean checkResourceOperation = checkResourceOperation(operation);
            if (!checkResourceOperation) return false;
        }
        return true;
    }

    public static boolean checkResourceOperation(ResourceOperation operation) {
        if (operation == null) return false;
        ResourceOperation.Type type = operation.getType();
        if (type == null) return false;
        switch (type) {
            case SIMPLE:
                return checkResource(operation.getResource());
            case RW_SEPARATE:
                return checkResourceReadOperation(operation.getReadOperation())
                        && checkResourceWriteOperation(operation.getWriteOperation());
            default:
                return false;
        }
    }

    public static boolean checkResourceReadOperation(ResourceReadOperation readOperation) {
        if (readOperation == null) return false;
        ResourceReadOperation.Type type = readOperation.getType();
        if (type == null) return false;
        switch (type) {
            case SIMPLE:
                return checkResource(readOperation.getReadResource());
            case ORDER:
            case RANDOM:
                List<Resource> readResources = readOperation.getReadResources();
                if (readResources == null || readResources.isEmpty()) return false;
                for (Resource readResource : readResources) {
                    if (!checkResource(readResource)) return false;
                }
                return true;
            default:
                return false;
        }
    }

    public static boolean checkResourceWriteOperation(ResourceWriteOperation writeOperation) {
        if (writeOperation == null) return false;
        ResourceWriteOperation.Type type = writeOperation.getType();
        if (type == null) return false;
        switch (type) {
            case SIMPLE:
                return checkResource(writeOperation.getWriteResource());
            case MULTI:
                List<Resource> readResources = writeOperation.getWriteResources();
                if (readResources == null || readResources.isEmpty()) return false;
                for (Resource readResource : readResources) {
                    if (!checkResource(readResource)) return false;
                }
                return true;
            default:
                return false;
        }
    }


    public static boolean checkResource(Resource resource) {
        return resource != null && resource.getUrl() != null && resource.getUrl().length() > 0;
    }
}
