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
    private final List<Resource> allReadResources;
    private final List<Resource> allWriteReources;

    private final long createTime = System.currentTimeMillis();

    public ResourceChooser(ResourceTable resourceTable, ProxyEnv proxyEnv) {
        this.resourceTable = resourceTable;
        this.proxyEnv = proxyEnv;
        ResourceTable.Type type = resourceTable.getType();
        if (type == ResourceTable.Type.SHADING) {
            int bucketSize = resourceTable.getShadingTable().getBucketSize();
            bucketSizeIs2Power = MathUtil.is2Power(bucketSize);
        }
        this.allResources = ResourceUtil.getAllResources(resourceTable);
        Set<Resource> readResources = new TreeSet<>(Comparator.comparing(Resource::getUrl));
        readResources.addAll(ResourceUtil.getAllReadResources(resourceTable));
        Set<Resource> writeResources = new TreeSet<>(Comparator.comparing(Resource::getUrl));
        readResources.addAll(ResourceUtil.getAllReadResources(resourceTable));
        writeResources.addAll(ResourceUtil.getAllWriteResources(resourceTable));
        this.allReadResources = new ArrayList<>(readResources);
        this.allWriteReources = new ArrayList<>(writeResources);
    }

    public ResourceTable getResourceTable() {
        return resourceTable;
    }

    public long getCreateTime() {
        return createTime;
    }

    public ResourceTable.Type getType() {
        return resourceTable.getType();
    }

    public List<Resource> getAllReadResources() {
        return allReadResources;
    }

    public List<Resource> getAllWriteResources() {
        return allWriteReources;
    }

    public Set<Resource> getAllResources() {
        return allResources;
    }

    public static Resource getReadResource(ReadResourceBean readResourceBean) {
        if (readResourceBean == null) return null;
        if (readResourceBean.resources == null || readResourceBean.resources.isEmpty()) {
            return null;
        }
        if (!readResourceBean.needRandom || readResourceBean.resources.size() == 1) {
            return readResourceBean.getResources().get(0);
        }
        int index = ThreadLocalRandom.current().nextInt(readResourceBean.resources.size());
        return readResourceBean.resources.get(index);
    }

    public Resource getReadResource(byte[]... shardingParam) {
        if (readResource != null) {
            return readResource;
        }
        ResourceTable.Type type = resourceTable.getType();
        if (type == ResourceTable.Type.SIMPLE) {
            ReadResourceBean readResourceBean = getReadResources(shardingParam);
            if (readResourceBean == null) return null;
            if (!readResourceBean.needRandom || readResourceBean.resources.size() == 1) {
                readResource = readResourceBean.resources.get(0);
                return readResource;
            } else {
                int index = ThreadLocalRandom.current().nextInt(readResourceBean.resources.size());
                return readResourceBean.resources.get(index);
            }
        } else {
            ReadResourceBean readResourceBean = getReadResources(shardingParam);
            return getReadResource(readResourceBean);
        }
    }

    public ReadResourceBean getReadResources(byte[]... shardingParam) {
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
            int shardingCode = proxyEnv.getShardingFunc().shardingCode(shardingParam);
            ResourceTable.ShadingTable shardingTable = resourceTable.getShadingTable();
            int bucketSize = shardingTable.getBucketSize();
            Map<Integer, ResourceOperation> operationMap = shardingTable.getResourceOperationMap();
            int index = MathUtil.mod(bucketSizeIs2Power, Math.abs(shardingCode), bucketSize);
            ResourceOperation resourceOperation = operationMap.get(index);
            return getReadResourcesFromOperation(resourceOperation);
        }
    }

    public List<Resource> getWriteResources(byte[]... shardingParam) {
        if (writeResources != null) return writeResources;
        ResourceTable.Type type = resourceTable.getType();
        if (type == ResourceTable.Type.SIMPLE) {
            ResourceTable.SimpleTable simpleTable = resourceTable.getSimpleTable();
            ResourceOperation resourceOperation = simpleTable.getResourceOperation();
            this.writeResources = getWriteResourcesFromOperation(resourceOperation);
            return this.writeResources;
        } else {
            int shardingCode = proxyEnv.getShardingFunc().shardingCode(shardingParam);
            ResourceTable.ShadingTable shardingTable = resourceTable.getShadingTable();
            int bucketSize = shardingTable.getBucketSize();
            Map<Integer, ResourceOperation> operationMap = shardingTable.getResourceOperationMap();
            int index = MathUtil.mod(bucketSizeIs2Power, Math.abs(shardingCode), bucketSize);
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

    public static boolean checkReadResourcesEqual(ResourceChooser.ReadResourceBean readResourceBean1, ResourceChooser.ReadResourceBean readResourceBean2) {
        if (readResourceBean1 == null || readResourceBean2 == null) {
            return false;
        }
        if (readResourceBean1.isNeedRandom() && !readResourceBean2.isNeedRandom()) {
            return false;
        }
        if (!readResourceBean1.isNeedRandom() && readResourceBean2.isNeedRandom()) {
            return false;
        }
        if (readResourceBean1.getResources() == null || readResourceBean2.getResources() == null) {
            return false;
        }
        if (readResourceBean1.getResources().size() != readResourceBean2.getResources().size()) {
            return false;
        }
        for (int i = 0; i < readResourceBean1.getResources().size(); i++) {
            Resource resource1 = readResourceBean1.getResources().get(i);
            Resource resource2 = readResourceBean2.getResources().get(i);
            if (!resource1.getUrl().equals(resource2.getUrl())) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkWriteResourcesEqual(List<Resource> resources1, List<Resource> resources2) {
        if (resources1 == null || resources2 == null) {
            return false;
        }
        if (resources1.size() != resources2.size()) {
            return false;
        }
        for (int i = 0; i < resources1.size(); i++) {
            Resource resource1 = resources1.get(i);
            Resource resource2 = resources2.get(i);
            if (!resource1.getUrl().equals(resource2.getUrl())) {
                return false;
            }
        }
        return true;
    }

    public static class ReadResourceBean {
        private final boolean needRandom;
        private final List<Resource> resources;

        public ReadResourceBean(boolean needRandom, List<Resource> resources) {
            this.needRandom = needRandom;
            this.resources = resources;
        }

        public boolean isNeedRandom() {
            return needRandom;
        }

        public List<Resource> getResources() {
            return resources;
        }

    }
}
