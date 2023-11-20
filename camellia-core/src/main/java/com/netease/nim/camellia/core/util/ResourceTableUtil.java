package com.netease.nim.camellia.core.util;

import com.netease.nim.camellia.core.api.CamelliaApiResponse;
import com.netease.nim.camellia.core.api.CamelliaApiV2Response;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.model.operation.ResourceOperation;
import com.netease.nim.camellia.core.model.operation.ResourceReadOperation;
import com.netease.nim.camellia.core.model.operation.ResourceWriteOperation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2019/5/16.
 */
public class ResourceTableUtil {

    public static CamelliaApiV2Response toV2Response(CamelliaApiResponse response) {
        CamelliaApiV2Response v2Response = new CamelliaApiV2Response();
        v2Response.setCode(response.getCode());
        v2Response.setMd5(response.getMd5());
        ResourceTable resourceTable = response.getResourceTable();
        if (resourceTable != null) {
            String table = ReadableResourceTableUtil.readableResourceTable(resourceTable);
            v2Response.setRouteTable(table);
        }
        return v2Response;
    }

    public static CamelliaApiResponse toV1Response(CamelliaApiV2Response response) {
        CamelliaApiResponse v1Response = new CamelliaApiResponse();
        v1Response.setCode(response.getCode());
        v1Response.setMd5(response.getMd5());
        String routeTable = response.getRouteTable();
        if (routeTable != null) {
            ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(routeTable);
            v1Response.setResourceTable(resourceTable);
        }
        return v1Response;
    }

    public static ResourceTable immutableResourceTable(ResourceTable resourceTable) {
        if (resourceTable == null) return null;
        ResourceTable immutableResourceTable = new ResourceTable();
        immutableResourceTable.setType(resourceTable.getType());
        ResourceTable.SimpleTable simpleTable = resourceTable.getSimpleTable();
        if (simpleTable != null) {
            ResourceTable.SimpleTable immutableSimpleTable = new ResourceTable.SimpleTable();
            ResourceOperation resourceOperation = simpleTable.getResourceOperation();
            if (resourceOperation != null) {
                ResourceOperation immutableResourceOperation = immutableResourceOperation(resourceOperation);
                immutableSimpleTable.setResourceOperation(immutableResourceOperation);
            }
            immutableResourceTable.setSimpleTable(immutableSimpleTable);
        }
        ResourceTable.ShadingTable shadingTable = resourceTable.getShadingTable();
        if (shadingTable != null) {
            ResourceTable.ShadingTable immutableShadingTable = new ResourceTable.ShadingTable();
            immutableShadingTable.setBucketSize(shadingTable.getBucketSize());
            Map<Integer, ResourceOperation> immutableResourceOperationMap = new HashMap<>();
            for (Map.Entry<Integer, ResourceOperation> entry : shadingTable.getResourceOperationMap().entrySet()) {
                immutableResourceOperationMap.put(entry.getKey(), immutableResourceOperation(entry.getValue()));
            }
            immutableShadingTable.setResourceOperationMap(Collections.unmodifiableMap(immutableResourceOperationMap));
            immutableResourceTable.setShadingTable(immutableShadingTable);
        }
        return immutableResourceTable;
    }

    public static ResourceOperation immutableResourceOperation(ResourceOperation resourceOperation) {
        if (resourceOperation == null) return null;
        ResourceOperation immutableResourceOperation = new ResourceOperation();
        immutableResourceOperation.setType(resourceOperation.getType());
        immutableResourceOperation.setResource(resourceOperation.getResource());
        ResourceReadOperation readOperation = resourceOperation.getReadOperation();
        if (readOperation != null) {
            ResourceReadOperation immutableReadOperation = new ResourceReadOperation();
            immutableReadOperation.setType(readOperation.getType());
            immutableReadOperation.setReadResource(readOperation.getReadResource());
            if (readOperation.getReadResources() != null) {
                immutableReadOperation.setReadResources(Collections.unmodifiableList(readOperation.getReadResources()));
            }
            immutableResourceOperation.setReadOperation(immutableReadOperation);
        }
        ResourceWriteOperation writeOperation = resourceOperation.getWriteOperation();
        if (writeOperation != null) {
            ResourceWriteOperation immutableWriteOperation = new ResourceWriteOperation();
            immutableWriteOperation.setType(writeOperation.getType());
            immutableWriteOperation.setWriteResource(writeOperation.getWriteResource());
            if (writeOperation.getWriteResources() != null) {
                immutableWriteOperation.setWriteResources(Collections.unmodifiableList(writeOperation.getWriteResources()));
            }
            immutableResourceOperation.setWriteOperation(immutableWriteOperation);
        }
        return immutableResourceOperation;
    }


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
    public static ResourceTable simpleShardingTable(Map<Integer, Resource> resourceMap, int bucketSize) {
        ResourceTable.ShadingTable shardingTable = new ResourceTable.ShadingTable();
        shardingTable.setBucketSize(bucketSize);
        Map<Integer, ResourceOperation> resourceOperationMap = new HashMap<>();
        for (int i=0; i<bucketSize; i++) {
            Resource resource = resourceMap.get(i);
            if (resource == null) {
                throw new IllegalArgumentException("resourceMap/bucketSize not match");
            }
            resourceOperationMap.put(i, new ResourceOperation(resource));
        }
        shardingTable.setResourceOperationMap(resourceOperationMap);

        ResourceTable table = new ResourceTable();
        table.setType(ResourceTable.Type.SHADING);
        table.setShadingTable(shardingTable);
        return table;
    }
}
