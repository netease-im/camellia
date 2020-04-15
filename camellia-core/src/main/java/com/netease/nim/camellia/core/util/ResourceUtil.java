package com.netease.nim.camellia.core.util;


import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.model.operation.ResourceOperation;
import com.netease.nim.camellia.core.model.operation.ResourceReadOperation;
import com.netease.nim.camellia.core.model.operation.ResourceWriteOperation;

import java.util.*;

/**
 *
 * Created by caojiajun on 2019/5/16.
 */
public class ResourceUtil {

    public static int resourceHashCode(Resource resource) {
        if (resource == null) return 0;
        if (resource.getUrl() == null) return 0;
        return resource.getUrl().hashCode();
    }

    public static boolean resourceEquals(Object object1, Object object2) {
        if (object1 == null && object2 == null) return true;
        if (object1 == object2) return true;
        if (object1 instanceof Resource && object2 instanceof Resource) {
            String url1 = ((Resource) object1).getUrl();
            String url2 = ((Resource) object2).getUrl();
            if (url1 == null && url2 == null) return true;
            if (url1 != null && url2 != null) {
                return url1.equals(url2);
            }
        }
        return false;
    }

    public static Set<Resource> getAllResources(ResourceTable resourceTable) {
        ResourceTable.Type type = resourceTable.getType();
        switch (type) {
            case SIMPLE:
                return getAllResources(resourceTable.getSimpleTable());
            case SHADING:
                return getAllResources(resourceTable.getShadingTable());
            default:
                return Collections.emptySet();
        }
    }

    public static Set<Resource> getAllResources(ResourceTable.SimpleTable simpleTable) {
        if (simpleTable == null) {
            return Collections.emptySet();
        }
        return getAllResources(simpleTable.getResourceOperation());
    }

    public static Set<Resource> getAllResources(ResourceOperation operation) {
        Set<Resource> allResource = new HashSet<>();
        ResourceOperation.Type type = operation.getType();
        switch (type) {
            case SIMPLE:
                allResource.add(operation.getResource());
                break;
            case RW_SEPARATE:
                ResourceWriteOperation writeOperation = operation.getWriteOperation();
                ResourceWriteOperation.Type writeOperationType = writeOperation.getType();
                switch (writeOperationType) {
                    case SIMPLE:
                        Resource writeResource = writeOperation.getWriteResource();
                        allResource.add(writeResource);
                        break;
                    case MULTI:
                        List<Resource> writeResources = writeOperation.getWriteResources();
                        for (Resource resource : writeResources) {
                            allResource.add(resource);
                        }
                        break;
                    default:
                        break;
                }
                ResourceReadOperation readOperation = operation.getReadOperation();
                ResourceReadOperation.Type readOperationType = readOperation.getType();
                switch (readOperationType) {
                    case SIMPLE:
                        Resource readResource = readOperation.getReadResource();
                        allResource.add(readResource);
                        break;
                    case RANDOM:
                    case ORDER:
                        List<Resource> readResources = readOperation.getReadResources();
                        for (Resource resource : readResources) {
                            allResource.add(resource);
                        }
                        break;
                    default:
                        break;
                }
        }
        return allResource;
    }

    public static Set<Resource> getAllResources(ResourceTable.ShadingTable shadingTable) {
        if (shadingTable == null) {
            return Collections.emptySet();
        }
        Set<Resource> allResource = new HashSet<>();
        for (Map.Entry<Integer, ResourceOperation> entry : shadingTable.getResourceOperationMap().entrySet()) {
            allResource.addAll(getAllResources(entry.getValue()));
        }
        return allResource;
    }
}
