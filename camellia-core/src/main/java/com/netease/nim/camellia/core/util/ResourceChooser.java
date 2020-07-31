package com.netease.nim.camellia.core.util;

import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.model.operation.ResourceOperation;
import com.netease.nim.camellia.core.model.operation.ResourceReadOperation;
import com.netease.nim.camellia.core.model.operation.ResourceWriteOperation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * Created by caojiajun on 2019/12/13.
 */
public class ResourceChooser {
    private final ResourceTable resourceTable;
    private final ProxyEnv proxyEnv;

    private List<Resource> readResources = null;
    private List<Resource> writeResources = null;

    private boolean bucketSizeIs2Power = false;

    public ResourceChooser(ResourceTable resourceTable, ProxyEnv proxyEnv) {
        this.resourceTable = resourceTable;
        this.proxyEnv = proxyEnv;
        ResourceTable.Type type = resourceTable.getType();
        if (type == ResourceTable.Type.SHADING) {
            int bucketSize = resourceTable.getShadingTable().getBucketSize();
            bucketSizeIs2Power = MathUtil.is2Power(bucketSize);
        }
    }

    public ResourceTable.Type getType() {
        return resourceTable.getType();
    }

    public List<Resource> getReadResources(byte[]... shadingParam) {
        if (readResources != null) return readResources;
        ResourceTable.Type type = resourceTable.getType();
        if (type == ResourceTable.Type.SIMPLE) {
            ResourceTable.SimpleTable simpleTable = resourceTable.getSimpleTable();
            ResourceOperation resourceOperation = simpleTable.getResourceOperation();
            List<Resource> readResources = getReadResourcesFromOperation(resourceOperation);
            if (simpleTable.getResourceOperation().getType() == ResourceOperation.Type.SIMPLE) {
                this.readResources = readResources;
            }
            if (simpleTable.getResourceOperation().getType() == ResourceOperation.Type.RW_SEPARATE) {
                ResourceReadOperation readOperation = simpleTable.getResourceOperation().getReadOperation();
                if (readOperation.getType() == ResourceReadOperation.Type.SIMPLE) {
                    this.readResources = readResources;
                } else if (readOperation.getType() == ResourceReadOperation.Type.RANDOM) {
                    if (readOperation.getReadResources().size() == 1) {
                        this.readResources = readResources;
                    }
                } else if (readOperation.getType() == ResourceReadOperation.Type.ORDER) {
                    this.readResources = readResources;
                }
            }
            return readResources;
        } else {
            int shadingCode = proxyEnv.getShadingFunc().shadingCode(shadingParam);
            ResourceTable.ShadingTable shadingTable = resourceTable.getShadingTable();
            int bucketSize = shadingTable.getBucketSize();
            Map<Integer, ResourceOperation> operationMap = shadingTable.getResourceOperationMap();
            int index = MathUtil.mod(bucketSizeIs2Power, Math.abs(shadingCode), bucketSize);
            ResourceOperation resourceOperation = operationMap.get(index);
            return getReadResourcesFromOperation(resourceOperation);
        }
    }

    public List<Resource> getWriteResources(byte[]... shadingParam) {
        if (writeResources != null) return writeResources;
        ResourceTable.Type type = resourceTable.getType();
        if (type == ResourceTable.Type.SIMPLE) {
            ResourceTable.SimpleTable simpleTable = resourceTable.getSimpleTable();
            ResourceOperation resourceOperation = simpleTable.getResourceOperation();
            this.writeResources = getWriteResourcesFromOperation(resourceOperation);
            return this.writeResources;
        } else {
            int shadingCode = proxyEnv.getShadingFunc().shadingCode(shadingParam);
            ResourceTable.ShadingTable shadingTable = resourceTable.getShadingTable();
            int bucketSize = shadingTable.getBucketSize();
            Map<Integer, ResourceOperation> operationMap = shadingTable.getResourceOperationMap();
            int index = MathUtil.mod(bucketSizeIs2Power, Math.abs(shadingCode), bucketSize);
            ResourceOperation resourceOperation = operationMap.get(index);
            return getWriteResourcesFromOperation(resourceOperation);
        }
    }

    private List<Resource> getReadResourcesFromOperation(ResourceOperation resourceOperation) {
        ResourceOperation.Type resourceOperationType = resourceOperation.getType();
        if (resourceOperationType == ResourceOperation.Type.SIMPLE) {
            return Collections.singletonList(resourceOperation.getResource());
        } else if (resourceOperationType == ResourceOperation.Type.RW_SEPARATE) {
            ResourceReadOperation readOperation = resourceOperation.getReadOperation();
            ResourceReadOperation.Type readOperationType = readOperation.getType();
            if (readOperationType == ResourceReadOperation.Type.SIMPLE) {
                return Collections.singletonList(readOperation.getReadResource());
            } else if (readOperationType == ResourceReadOperation.Type.RANDOM) {
                int size = readOperation.getReadResources().size();
                if (size == 1) {
                    return Collections.singletonList(readOperation.getReadResources().get(0));
                }
                int index = ThreadLocalRandom.current().nextInt(size);
                return Collections.singletonList(readOperation.getReadResources().get(index));
            } else if (readOperationType == ResourceReadOperation.Type.ORDER) {
                List<Resource> readResources = readOperation.getReadResources();
                return new ArrayList<>(readResources);
            }
        }
        throw new IllegalArgumentException();
    }

    private List<Resource> getWriteResourcesFromOperation(ResourceOperation resourceOperation) {
        ResourceOperation.Type resourceOperationType = resourceOperation.getType();
        if (resourceOperationType == ResourceOperation.Type.SIMPLE) {
            return Collections.singletonList(resourceOperation.getResource());
        } else if (resourceOperationType == ResourceOperation.Type.RW_SEPARATE) {
            ResourceWriteOperation writeOperation = resourceOperation.getWriteOperation();
            ResourceWriteOperation.Type writeOperationType = writeOperation.getType();
            if (writeOperationType == ResourceWriteOperation.Type.SIMPLE) {
                return Collections.singletonList(writeOperation.getWriteResource());
            } else if (writeOperationType == ResourceWriteOperation.Type.MULTI) {
                List<Resource> writeResources = writeOperation.getWriteResources();
                return new ArrayList<>(writeResources);
            }
        }
        throw new IllegalArgumentException();
    }
}
