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

    public static int resourceCompare(Resource resource1, Resource resource2) {
        String url1 = resource1 == null ? "" : resource1.getUrl();
        if (url1 == null) {
            url1 = "";
        }
        String url2 = resource2 == null ? "" : resource2.getUrl();
        if (url2 == null) {
            url2 = "";
        }
        return url1.compareTo(url2);
    }

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

    public static Set<Resource> getAllReadResources(ResourceTable resourceTable) {
        Set<Resource> readResources = new HashSet<>();
        ResourceTable.Type type = resourceTable.getType();
        if (type == ResourceTable.Type.SIMPLE) {
            ResourceTable.SimpleTable simpleTable = resourceTable.getSimpleTable();
            ResourceOperation resourceOperation = simpleTable.getResourceOperation();
            Set<Resource> set = getAllReadResources(resourceOperation);
            readResources.addAll(set);
        } else if (type == ResourceTable.Type.SHADING) {
            ResourceTable.ShadingTable shadingTable = resourceTable.getShadingTable();
            Map<Integer, ResourceOperation> resourceOperationMap = shadingTable.getResourceOperationMap();
            for (ResourceOperation resourceOperation : resourceOperationMap.values()) {
                Set<Resource> set = getAllReadResources(resourceOperation);
                readResources.addAll(set);
            }
        }
        return readResources;
    }

    public static Set<Resource> getAllWriteResources(ResourceTable resourceTable) {
        Set<Resource> writeResources = new HashSet<>();
        ResourceTable.Type type = resourceTable.getType();
        if (type == ResourceTable.Type.SIMPLE) {
            ResourceTable.SimpleTable simpleTable = resourceTable.getSimpleTable();
            ResourceOperation resourceOperation = simpleTable.getResourceOperation();
            Set<Resource> set = getAllWriteResources(resourceOperation);
            writeResources.addAll(set);
        } else if (type == ResourceTable.Type.SHADING) {
            ResourceTable.ShadingTable shadingTable = resourceTable.getShadingTable();
            Map<Integer, ResourceOperation> resourceOperationMap = shadingTable.getResourceOperationMap();
            for (ResourceOperation resourceOperation : resourceOperationMap.values()) {
                Set<Resource> set = getAllWriteResources(resourceOperation);
                writeResources.addAll(set);
            }
        }
        return writeResources;
    }

    public static Set<Resource> getAllReadResources(ResourceOperation resourceOperation) {
        Set<Resource> readResources = new HashSet<>();
        ResourceOperation.Type operationType = resourceOperation.getType();
        if (operationType == ResourceOperation.Type.SIMPLE) {
            readResources.add(resourceOperation.getResource());
        } else if (operationType == ResourceOperation.Type.RW_SEPARATE) {
            ResourceReadOperation readOperation = resourceOperation.getReadOperation();
            ResourceReadOperation.Type readOperationType = readOperation.getType();
            if (readOperationType == ResourceReadOperation.Type.SIMPLE) {
                readResources.add(readOperation.getReadResource());
            } else if (readOperationType == ResourceReadOperation.Type.ORDER || readOperationType == ResourceReadOperation.Type.RANDOM) {
                readResources.addAll(readOperation.getReadResources());
            }
        }
        return readResources;
    }

    public static Set<Resource> getAllWriteResources(ResourceOperation resourceOperation) {
        Set<Resource> writeResources = new HashSet<>();
        ResourceOperation.Type operationType = resourceOperation.getType();
        if (operationType == ResourceOperation.Type.SIMPLE) {
            writeResources.add(resourceOperation.getResource());
        } else if (operationType == ResourceOperation.Type.RW_SEPARATE) {
            ResourceWriteOperation writeOperation = resourceOperation.getWriteOperation();
            ResourceWriteOperation.Type writeOperationType = writeOperation.getType();
            if (writeOperationType == ResourceWriteOperation.Type.SIMPLE) {
                writeResources.add(writeOperation.getWriteResource());
            } else if (writeOperationType == ResourceWriteOperation.Type.MULTI) {
                writeResources.addAll(writeOperation.getWriteResources());
            }
        }
        return writeResources;
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
                        allResource.addAll(writeResources);
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
                        allResource.addAll(readResources);
                        break;
                    default:
                        break;
                }
        }
        return allResource;
    }

    public static Set<Resource> getAllResources(ResourceTable.ShadingTable shardingTable) {
        if (shardingTable == null) {
            return Collections.emptySet();
        }
        Set<Resource> allResource = new HashSet<>();
        for (Map.Entry<Integer, ResourceOperation> entry : shardingTable.getResourceOperationMap().entrySet()) {
            allResource.addAll(getAllResources(entry.getValue()));
        }
        return allResource;
    }
}
