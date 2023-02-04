package com.netease.nim.camellia.core.util;

import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.model.operation.ResourceOperation;
import com.netease.nim.camellia.core.model.operation.ResourceReadOperation;
import com.netease.nim.camellia.core.model.operation.ResourceWriteOperation;
import com.netease.nim.camellia.tools.utils.MathUtil;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * Created by caojiajun on 2019/12/13.
 */
public class ResourceChooser {

    public static final byte[] EMPTY_ARRAY = new byte[0];

    private final ResourceTable resourceTable;
    private final ProxyEnv proxyEnv;

    private ReadResourceBean readResourceBean = null;
    private Resource readResource = null;
    private List<Resource> writeResources = null;

    private boolean bucketSizeIs2Power = false;
    private final boolean needResourceChecker;

    private final Set<Resource> allResources;
    private final List<Resource> allReadResources;
    private final List<Resource> allWriteResources;

    private final long createTime = System.currentTimeMillis();

    private ResourceChecker resourceChecker;

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
        this.allWriteResources = new ArrayList<>(writeResources);
        this.needResourceChecker = calcIsNeedResourceChecker(resourceTable);
    }

    public void setResourceChecker(ResourceChecker resourceChecker) {
        this.resourceChecker = resourceChecker;
        if (needResourceChecker) {
            for (Resource resource : allReadResources) {
                resourceChecker.addResource(resource);
            }
        }
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
        return allWriteResources;
    }

    public Set<Resource> getAllResources() {
        return allResources;
    }

    public static interface ResourceChecker {
        void addResource(Resource resource);
        boolean checkValid(Resource resource);
    }

    /**
     * 获取read resource
     * @param shardingParam shardingParam
     * @return resource
     */
    public Resource getReadResource(byte[]... shardingParam) {
        if (readResource != null) {
            return readResource;
        }
        ResourceTable.Type type = resourceTable.getType();
        ReadResourceBean readResourceBean = getReadResources(shardingParam);
        if (readResourceBean == null) {
            return null;
        }
        Resource readResource = getReadResource(readResourceBean);
        if (type == ResourceTable.Type.SIMPLE && readResourceBean.getResources().size() == 1) {
            //下次就可以走缓存了
           this.readResource = readResource;
        }
        return readResource;
    }

    /**
     * 获取read resource 并且判断shadingKeys对于的resource是否一致，如果不一致，则返回null
     * @param shadingKeys shadingKeys
     * @return resource
     */
    public Resource getReadResourceWithCheckEqual(List<byte[]> shadingKeys) {
        ResourceChooser.ReadResourceBean resources = null;
        for (byte[] key : shadingKeys) {
            ResourceChooser.ReadResourceBean nextResources = getReadResources(key);
            if (resources != null) {
                boolean checkReadResourcesEqual = getReadResourceWithCheckEqual(resources, nextResources);
                if (!checkReadResourcesEqual) {
                    return null;
                }
            }
            resources = nextResources;
        }
        return getReadResource(resources);
    }

    /**
     * 获取write resource
     * @param shardingParam shardingParam
     * @return resource
     */
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

    /**
     * 获取write resource list 并且判断shadingKeys对于的resource是否一致，如果不一致，则返回null
     * @param shadingKeys shadingKeys
     * @return resource
     */
    public List<Resource> getWriteResourcesWithCheckEqual(List<byte[]> shadingKeys) {
        List<Resource> resources = null;
        for (byte[] key : shadingKeys) {
            List<Resource> nextResources = getWriteResources(key);
            if (resources != null) {
                boolean checkWriteResourcesEqual = checkWriteResourcesEqual(resources, nextResources);
                if (!checkWriteResourcesEqual) {
                    return null;
                }
            }
            resources = nextResources;
        }
        return resources;
    }

    private Resource getReadResource(ReadResourceBean readResourceBean) {
        if (readResourceBean == null) return null;
        List<Resource> resources = readResourceBean.getValidResource();
        if (resources == null || resources.isEmpty()) {
            return null;
        }
        if (resources.size() == 1) {
            return resources.get(0);
        }
        if (readResourceBean.getType() == ResourceReadOperation.Type.SIMPLE || readResourceBean.getType() == ResourceReadOperation.Type.ORDER) {
            return resources.get(0);
        } else if (readResourceBean.getType() == ResourceReadOperation.Type.RANDOM) {
            int index = ThreadLocalRandom.current().nextInt(resources.size());
            return resources.get(index);
        } else {
            return null;
        }
    }

    private ReadResourceBean getReadResources(byte[]... shardingParam) {
        if (readResourceBean != null) {
            return readResourceBean;
        }
        ResourceTable.Type type = resourceTable.getType();
        if (type == ResourceTable.Type.SIMPLE) {
            ResourceTable.SimpleTable simpleTable = resourceTable.getSimpleTable();
            ResourceOperation resourceOperation = simpleTable.getResourceOperation();
            this.readResourceBean = getReadResourcesFromOperation(resourceOperation);
            return this.readResourceBean;
        } else if (type == ResourceTable.Type.SHADING) {
            int shardingCode = proxyEnv.getShardingFunc().shardingCode(shardingParam);
            ResourceTable.ShadingTable shardingTable = resourceTable.getShadingTable();
            int bucketSize = shardingTable.getBucketSize();
            Map<Integer, ResourceOperation> operationMap = shardingTable.getResourceOperationMap();
            int index = MathUtil.mod(bucketSizeIs2Power, Math.abs(shardingCode), bucketSize);
            ResourceOperation resourceOperation = operationMap.get(index);
            return getReadResourcesFromOperation(resourceOperation);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private ReadResourceBean getReadResourcesFromOperation(ResourceOperation resourceOperation) {
        ResourceOperation.Type resourceOperationType = resourceOperation.getType();
        if (resourceOperationType == ResourceOperation.Type.SIMPLE) {
            return new ReadResourceBean(resourceChecker, ResourceReadOperation.Type.SIMPLE, Collections.singletonList(resourceOperation.getResource()));
        } else if (resourceOperationType == ResourceOperation.Type.RW_SEPARATE) {
            ResourceReadOperation readOperation = resourceOperation.getReadOperation();
            ResourceReadOperation.Type readOperationType = readOperation.getType();
            if (readOperationType == ResourceReadOperation.Type.SIMPLE) {
                return new ReadResourceBean(resourceChecker, readOperationType, Collections.singletonList(readOperation.getReadResource()));
            } else if (readOperationType == ResourceReadOperation.Type.ORDER || readOperationType == ResourceReadOperation.Type.RANDOM) {
                List<Resource> readResources = readOperation.getReadResources();
                return new ReadResourceBean(resourceChecker, readOperationType, readResources);
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

    private boolean getReadResourceWithCheckEqual(ResourceChooser.ReadResourceBean readResourceBean1, ResourceChooser.ReadResourceBean readResourceBean2) {
        if (readResourceBean1 == null || readResourceBean2 == null) {
            return false;
        }
        if (readResourceBean1.getType() != readResourceBean2.getType()) {
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

    private boolean checkWriteResourcesEqual(List<Resource> resources1, List<Resource> resources2) {
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

    private boolean calcIsNeedResourceChecker(ResourceTable resourceTable) {
        ResourceTable.Type type = resourceTable.getType();
        if (type == ResourceTable.Type.SIMPLE) {
            ResourceTable.SimpleTable simpleTable = resourceTable.getSimpleTable();
            ResourceOperation resourceOperation = simpleTable.getResourceOperation();
            ReadResourceBean bean = getReadResourcesFromOperation(resourceOperation);
            return bean.getResources().size() > 1;
        } else if (type == ResourceTable.Type.SHADING) {
            ResourceTable.ShadingTable shadingTable = resourceTable.getShadingTable();
            for (ResourceOperation resourceOperation : shadingTable.getResourceOperationMap().values()) {
                ReadResourceBean bean = getReadResourcesFromOperation(resourceOperation);
                if (bean.getResources().size() > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    public static class ReadResourceBean {
        private final ResourceReadOperation.Type type;
        private final List<Resource> resources;
        private List<Resource> validResources;

        public ReadResourceBean(ResourceChecker resourceChecker, ResourceReadOperation.Type type, List<Resource> resources) {
            this.type = type;
            this.resources = resources;
            this.validResources = resources;
            if (resourceChecker != null && resources.size() > 1) {
                this.validResources = new ArrayList<>();
                for (Resource resource : resources) {
                    if (resourceChecker.checkValid(resource)) {
                        validResources.add(resource);
                    }
                }
                if (validResources.isEmpty()) {
                    validResources = resources;
                }
            }
        }

        public ResourceReadOperation.Type getType() {
            return type;
        }

        public List<Resource> getResources() {
            return resources;
        }

        public List<Resource> getValidResource() {
            return validResources;
        }

    }
}
