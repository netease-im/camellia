package com.netease.nim.camellia.core.util;

import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.model.operation.ResourceOperation;
import com.netease.nim.camellia.core.model.operation.ResourceReadOperation;
import com.netease.nim.camellia.core.model.operation.ResourceWriteOperation;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * Created by caojiajun on 2019/12/13.
 */
public class ResourceChooser {
    private final ResourceTable resourceTable;
    private final ProxyEnv proxyEnv;

    private ReadResourceBean readResourceBean = null;
    private Resource readResource = null;
    private List<Resource> writeResources = null;

    private boolean bucketSizeIs2Power = false;

    private final Set<Resource> allResources;

    public ResourceChooser(ResourceTable resourceTable, ProxyEnv proxyEnv) {
        this.resourceTable = resourceTable;
        this.proxyEnv = proxyEnv;
        ResourceTable.Type type = resourceTable.getType();
        if (type == ResourceTable.Type.SHADING) {
            int bucketSize = resourceTable.getShadingTable().getBucketSize();
            bucketSizeIs2Power = MathUtil.is2Power(bucketSize);
        }
        this.allResources = ResourceUtil.getAllResources(resourceTable);
    }

    public ResourceTable.Type getType() {
        return resourceTable.getType();
    }

    public Set<Resource> getAllResources() {
        return allResources;
    }

    public Resource getReadResource(byte[]... shadingParam) {
        if (readResource != null) {
            return readResource;
        }
        ResourceTable.Type type = resourceTable.getType();
        if (type == ResourceTable.Type.SIMPLE) {
            ReadResourceBean readResourceBean = _getReadResources(shadingParam);
            if (readResourceBean == null) return null;
            if (!readResourceBean.needRandom || readResourceBean.resources.size() == 1) {
                readResource = readResourceBean.resources.get(0);
                return readResource;
            } else {
                int index = ThreadLocalRandom.current().nextInt(readResourceBean.resources.size());
                return readResourceBean.resources.get(index);
            }
        } else {
            ReadResourceBean readResourceBean = _getReadResources(shadingParam);
            if (readResourceBean == null) return null;
            if (!readResourceBean.needRandom || readResourceBean.resources.size() == 1) {
                return readResourceBean.resources.get(0);
            } else {
                int index = ThreadLocalRandom.current().nextInt(readResourceBean.resources.size());
                return readResourceBean.resources.get(index);
            }
        }
    }

    private ReadResourceBean _getReadResources(byte[]... shadingParam) {
        if (readResourceBean != null) {
            return readResourceBean;
        }
        ResourceTable.Type type = resourceTable.getType();
        if (type == ResourceTable.Type.SIMPLE) {
            ResourceTable.SimpleTable simpleTable = resourceTable.getSimpleTable();
            ResourceOperation resourceOperation = simpleTable.getResourceOperation();
            this.readResourceBean = getReadResourcesFromOperation(resourceOperation);
            return this.readResourceBean;
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

    private ReadResourceBean getReadResourcesFromOperation(ResourceOperation resourceOperation) {
        ResourceOperation.Type resourceOperationType = resourceOperation.getType();
        if (resourceOperationType == ResourceOperation.Type.SIMPLE) {
            return new ReadResourceBean(false, Collections.singletonList(resourceOperation.getResource()));
        } else if (resourceOperationType == ResourceOperation.Type.RW_SEPARATE) {
            ResourceReadOperation readOperation = resourceOperation.getReadOperation();
            ResourceReadOperation.Type readOperationType = readOperation.getType();
            if (readOperationType == ResourceReadOperation.Type.SIMPLE) {
                return new ReadResourceBean(false, Collections.singletonList(readOperation.getReadResource()));
            } else if (readOperationType == ResourceReadOperation.Type.ORDER) {
                List<Resource> readResources = readOperation.getReadResources();
                return new ReadResourceBean(false, readResources);
            } else if (readOperationType == ResourceReadOperation.Type.RANDOM) {
                List<Resource> readResources = readOperation.getReadResources();
                return new ReadResourceBean(true, readResources);
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

    private static class ReadResourceBean {
        private final boolean needRandom;
        private final List<Resource> resources;

        public ReadResourceBean(boolean needRandom, List<Resource> resources) {
            this.needRandom = needRandom;
            this.resources = resources;
        }
    }
}
