package com.netease.nim.camellia.core.util;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.model.operation.ResourceOperation;
import com.netease.nim.camellia.core.model.operation.ResourceReadOperation;
import com.netease.nim.camellia.core.model.operation.ResourceWriteOperation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2019/5/16.
 */
public class ResourceTableUtil {

    /**
     * 单写单读
     */
    public static ResourceTable simpleTable(Resource resource) {
        ResourceOperation resourceOperation = new ResourceOperation(resource);

        ResourceTable.SimpleTable simpleTable = new ResourceTable.SimpleTable();
        simpleTable.setResourceOperation(resourceOperation);

        ResourceTable table = new ResourceTable();
        table.setType(ResourceTable.Type.SIMPLE);
        table.setSimpleTable(simpleTable);
        return table;
    }

    /**
     * 双写单读
     */
    public static ResourceTable simple2W1RTable(Resource readResource, Resource writeResource1, Resource writeResource2) {
        ResourceReadOperation readOperation = new ResourceReadOperation(readResource);
        ResourceWriteOperation writeOperation = new ResourceWriteOperation(Arrays.asList(writeResource1, writeResource2));

        ResourceOperation resourceOperation = new ResourceOperation(readOperation, writeOperation);

        ResourceTable.SimpleTable simpleTable = new ResourceTable.SimpleTable();
        simpleTable.setResourceOperation(resourceOperation);

        ResourceTable table = new ResourceTable();
        table.setType(ResourceTable.Type.SIMPLE);
        table.setSimpleTable(simpleTable);
        return table;
    }

    /**
     * 读写分离
     */
    public static ResourceTable simpleRwSeparateTable(Resource readResource, Resource writeResource) {
        ResourceReadOperation readOperation = new ResourceReadOperation(readResource);
        ResourceWriteOperation writeOperation = new ResourceWriteOperation(writeResource);

        ResourceOperation resourceOperation = new ResourceOperation(readOperation, writeOperation);

        ResourceTable.SimpleTable simpleTable = new ResourceTable.SimpleTable();
        simpleTable.setResourceOperation(resourceOperation);

        ResourceTable table = new ResourceTable();
        table.setType(ResourceTable.Type.SIMPLE);
        table.setSimpleTable(simpleTable);
        return table;
    }

    /**
     * 不带N读N写的分片
     */
    public static ResourceTable simpleShadingTable(Map<Integer, Resource> resourceMap, int bucketSize) {
        ResourceTable.ShadingTable shadingTable = new ResourceTable.ShadingTable();
        shadingTable.setBucketSize(bucketSize);
        Map<Integer, ResourceOperation> resourceOperationMap = new HashMap<>();
        for (int i=0; i<bucketSize; i++) {
            Resource resource = resourceMap.get(i);
            if (resource == null) {
                throw new IllegalArgumentException("resourceMap/bucketSize not match");
            }
            resourceOperationMap.put(i, new ResourceOperation(resource));
        }
        shadingTable.setResourceOperationMap(resourceOperationMap);

        ResourceTable table = new ResourceTable();
        table.setType(ResourceTable.Type.SHADING);
        table.setShadingTable(shadingTable);
        return table;
    }
}
