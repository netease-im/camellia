package com.netease.nim.camellia.core.util;

import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.model.operation.ResourceOperation;
import com.netease.nim.camellia.core.model.operation.ResourceReadOperation;
import com.netease.nim.camellia.core.model.operation.ResourceWriteOperation;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by caojiajun on 2026/9/23
 */
public class ResourceSelectorTest {

    @Test
    public void shouldReturnSingleWriteResourceForSimpleTable() {
        Resource write1 = new Resource("redis://write-1");
        ResourceSelector selector = selector(simpleTable(singleWriteOperation(write1)));

        Assert.assertEquals(setOf(write1), setOf(selector.getAllWriteResources()));
    }

    @Test
    public void shouldSumWriteResourcesForMultiWriteToDifferentClusters() {
        Resource write1 = new Resource("redis://write-1");
        Resource write2 = new Resource("redis://write-2");
        ResourceSelector selector = selector(simpleTable(multiWriteOperation(Arrays.asList(write1, write2))));

        //双写到2个不同的集群，各算1次
        Assert.assertEquals(setOf(write1, write2), setOf(selector.getAllWriteResources()));
    }

    @Test
    public void shouldCountSameClusterOnlyOnce() {
        Resource write1 = new Resource("redis://write-1");
        ResourceSelector selector = selector(simpleTable(multiWriteOperation(Arrays.asList(write1, write1))));

        //同一个集群配置了多次，只算1次
        Assert.assertEquals(setOf(write1), setOf(selector.getAllWriteResources()));
    }

    @Test
    public void shouldSumDistinctWriteResourcesForShardingTable() {
        Resource write1 = new Resource("redis://write-1");
        Resource write2 = new Resource("redis://write-2");
        Map<Integer, ResourceOperation> resourceOperationMap = new HashMap<>();
        resourceOperationMap.put(0, singleWriteOperation(write1));
        resourceOperationMap.put(1, singleWriteOperation(write2));
        ResourceSelector selector = selector(shardingTable(2, resourceOperationMap));

        //分片之间数据不重叠，需要汇总
        Assert.assertEquals(setOf(write1, write2), setOf(selector.getAllWriteResources()));
    }

    @Test
    public void shouldCountSameClusterOnlyOnceAcrossShards() {
        Resource write1 = new Resource("redis://write-1");
        Resource mirror1 = new Resource("redis://mirror-1");
        Map<Integer, ResourceOperation> resourceOperationMap = new HashMap<>();
        resourceOperationMap.put(0, multiWriteOperation(Arrays.asList(write1, mirror1)));
        resourceOperationMap.put(1, multiWriteOperation(Arrays.asList(write1, mirror1)));
        ResourceSelector selector = selector(shardingTable(2, resourceOperationMap));

        //多个分片配置了同一组写资源，每个集群只算1次
        Assert.assertEquals(setOf(write1, mirror1), setOf(selector.getAllWriteResources()));
    }

    private ResourceSelector selector(ResourceTable resourceTable) {
        return new ResourceSelector(resourceTable, new ProxyEnv.Builder().build());
    }

    private Set<Resource> setOf(Resource... resources) {
        return new HashSet<>(Arrays.asList(resources));
    }

    private Set<Resource> setOf(List<Resource> resources) {
        return new HashSet<>(resources);
    }

    private ResourceOperation singleWriteOperation(Resource writeResource) {
        return new ResourceOperation(new ResourceReadOperation(writeResource), new ResourceWriteOperation(writeResource));
    }

    private ResourceOperation multiWriteOperation(List<Resource> writeResources) {
        return new ResourceOperation(new ResourceReadOperation(writeResources.get(0)), new ResourceWriteOperation(writeResources));
    }

    private ResourceTable simpleTable(ResourceOperation operation) {
        ResourceTable.SimpleTable simpleTable = new ResourceTable.SimpleTable();
        simpleTable.setResourceOperation(operation);
        return new ResourceTable(simpleTable);
    }

    private ResourceTable shardingTable(int bucketSize, Map<Integer, ResourceOperation> resourceOperationMap) {
        ResourceTable.ShadingTable shadingTable = new ResourceTable.ShadingTable();
        shadingTable.setBucketSize(bucketSize);
        shadingTable.setResourceOperationMap(resourceOperationMap);
        return new ResourceTable(shadingTable);
    }
}
