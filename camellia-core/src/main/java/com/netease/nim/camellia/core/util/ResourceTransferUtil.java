package com.netease.nim.camellia.core.util;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.model.operation.ResourceOperation;
import com.netease.nim.camellia.core.model.operation.ResourceReadOperation;
import com.netease.nim.camellia.core.model.operation.ResourceWriteOperation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2019/7/22.
 */
public class ResourceTransferUtil {

    public static interface ResourceTransferFunc {
        Resource transfer(Resource resource);
    }

    public static ResourceTable transfer(ResourceTable resourceTable, ResourceTransferFunc func) {
        if (resourceTable == null) return null;
        if (func == null) return resourceTable;

        ResourceTable newResourceTable = new ResourceTable();

        ResourceTable.Type type = resourceTable.getType();
        newResourceTable.setType(type);
        if (type == ResourceTable.Type.SIMPLE) {
            ResourceTable.SimpleTable simpleTable = resourceTable.getSimpleTable();
            ResourceTable.SimpleTable newSimpleTable = new ResourceTable.SimpleTable();
            ResourceOperation resourceOperation = simpleTable.getResourceOperation();
            ResourceOperation resourceOperation1 = transfer(resourceOperation, func);
            newSimpleTable.setResourceOperation(resourceOperation1);
            newResourceTable.setSimpleTable(newSimpleTable);
        } else if (type == ResourceTable.Type.SHADING) {
            ResourceTable.ShardingTable shardingTable = resourceTable.getShardingTable();
            ResourceTable.ShardingTable newShardingTable = new ResourceTable.ShardingTable();
            newShardingTable.setBucketSize(shardingTable.getBucketSize());
            Map<Integer, ResourceOperation> map = new HashMap<>();
            for (Map.Entry<Integer, ResourceOperation> entry : shardingTable.getResourceOperationMap().entrySet()) {
                Integer key = entry.getKey();
                ResourceOperation resourceOperation = entry.getValue();
                ResourceOperation resourceOperation1 = transfer(resourceOperation, func);
                map.put(key, resourceOperation1);
            }
            newShardingTable.setResourceOperationMap(map);
            newResourceTable.setShardingTable(newShardingTable);
        }
        return newResourceTable;
    }

    private static ResourceOperation transfer(ResourceOperation resourceOperation, ResourceTransferFunc func) {
        if (resourceOperation == null) return null;
        ResourceOperation newResourceOperation = new ResourceOperation();
        ResourceOperation.Type resourceOperationType = resourceOperation.getType();
        newResourceOperation.setType(resourceOperationType);
        if (resourceOperationType == ResourceOperation.Type.SIMPLE) {
            newResourceOperation.setResource(func.transfer(resourceOperation.getResource()));
        } else if (resourceOperationType == ResourceOperation.Type.RW_SEPARATE) {
            ResourceReadOperation readOperation = resourceOperation.getReadOperation();
            ResourceReadOperation newReadOperation = new ResourceReadOperation();
            ResourceReadOperation.Type readOperationType = readOperation.getType();
            newReadOperation.setType(readOperationType);
            if (readOperationType == ResourceReadOperation.Type.SIMPLE) {
                newReadOperation.setReadResource(func.transfer(readOperation.getReadResource()));
            } else if (readOperationType == ResourceReadOperation.Type.ORDER || readOperationType == ResourceReadOperation.Type.RANDOM) {
                List<Resource> readResources = readOperation.getReadResources();
                List<Resource> list = new ArrayList<>();
                for (Resource readResource : readResources) {
                    list.add(func.transfer(readResource));
                }
                newReadOperation.setReadResources(list);
            }

            ResourceWriteOperation writeOperation = resourceOperation.getWriteOperation();
            ResourceWriteOperation newWriteOperation = new ResourceWriteOperation();
            ResourceWriteOperation.Type writeOperationType = writeOperation.getType();
            newWriteOperation.setType(writeOperationType);
            if (writeOperationType == ResourceWriteOperation.Type.SIMPLE) {
                newWriteOperation.setWriteResource(func.transfer(writeOperation.getWriteResource()));
            } else if (writeOperationType == ResourceWriteOperation.Type.MULTI) {
                List<Resource> writeResources = writeOperation.getWriteResources();
                List<Resource> list = new ArrayList<>();
                for (Resource writeResource : writeResources) {
                    list.add(func.transfer(writeResource));
                }
                newWriteOperation.setWriteResources(list);
            }

            newResourceOperation.setReadOperation(newReadOperation);
            newResourceOperation.setWriteOperation(newWriteOperation);
        }
        return newResourceOperation;
    }
}
