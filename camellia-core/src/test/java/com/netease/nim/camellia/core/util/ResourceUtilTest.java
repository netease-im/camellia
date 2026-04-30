package com.netease.nim.camellia.core.util;

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
 * Created by caojiajun on 2026/4/29
 */
public class ResourceUtilTest {

    @Test
    public void shouldCompareResourcesByUrl() {
        Resource resource1 = new Resource("redis://a");
        Resource resource2 = new Resource("redis://a");
        Resource resource3 = new Resource("redis://b");

        Assert.assertEquals(resource1, resource2);
        Assert.assertEquals(resource1.hashCode(), resource2.hashCode());
        Assert.assertNotEquals(resource1, resource3);
        Assert.assertTrue(resource1.compareTo(resource3) < 0);
        Assert.assertTrue(ResourceUtil.resourceEquals(null, null));
        Assert.assertFalse(ResourceUtil.resourceEquals(resource1, "redis://a"));
        Assert.assertEquals(0, ResourceUtil.resourceHashCode(null));
    }

    @Test
    public void shouldCollectResourcesFromReadWriteSeparateSimpleTable() {
        Resource read1 = new Resource("redis://read-1");
        Resource read2 = new Resource("redis://read-2");
        Resource write1 = new Resource("redis://write-1");
        Resource write2 = new Resource("redis://write-2");
        ResourceReadOperation readOperation = new ResourceReadOperation(ResourceReadOperation.Type.ORDER,
                Arrays.asList(read1, read2));
        ResourceWriteOperation writeOperation = new ResourceWriteOperation(Arrays.asList(write1, write2));
        ResourceTable table = simpleTable(new ResourceOperation(readOperation, writeOperation));

        Assert.assertEquals(setOf(read1, read2, write1, write2), ResourceUtil.getAllResources(table));
        Assert.assertEquals(setOf(read1, read2), ResourceUtil.getAllReadResources(table));
        Assert.assertEquals(setOf(write1, write2), ResourceUtil.getAllWriteResources(table));

        List<Set<Resource>> readResourceList = ResourceUtil.getAllReadResourceList(table);
        Assert.assertEquals(1, readResourceList.size());
        Assert.assertEquals(setOf(read1, read2), readResourceList.get(0));
    }

    @Test
    public void shouldCollectResourcesFromShardingTable() {
        Resource bucket0 = new Resource("redis://bucket-0");
        Resource read1 = new Resource("redis://read-1");
        Resource write1 = new Resource("redis://write-1");
        ResourceOperation rwOperation = new ResourceOperation(new ResourceReadOperation(read1),
                new ResourceWriteOperation(write1));
        Map<Integer, ResourceOperation> resourceOperationMap = new HashMap<>();
        resourceOperationMap.put(0, new ResourceOperation(bucket0));
        resourceOperationMap.put(1, rwOperation);

        ResourceTable.ShadingTable shadingTable = new ResourceTable.ShadingTable();
        shadingTable.setBucketSize(2);
        shadingTable.setResourceOperationMap(resourceOperationMap);
        ResourceTable table = new ResourceTable(shadingTable);

        Assert.assertEquals(setOf(bucket0, read1, write1), ResourceUtil.getAllResources(table));
        Assert.assertEquals(setOf(bucket0, read1), ResourceUtil.getAllReadResources(table));
        Assert.assertEquals(setOf(bucket0, write1), ResourceUtil.getAllWriteResources(table));
    }

    @Test
    public void shouldReturnEmptySetForNullSimpleOrShardingTable() {
        Assert.assertTrue(ResourceUtil.getAllResources((ResourceTable.SimpleTable) null).isEmpty());
        Assert.assertTrue(ResourceUtil.getAllResources((ResourceTable.ShadingTable) null).isEmpty());
    }

    private ResourceTable simpleTable(ResourceOperation operation) {
        ResourceTable.SimpleTable simpleTable = new ResourceTable.SimpleTable();
        simpleTable.setResourceOperation(operation);
        return new ResourceTable(simpleTable);
    }

    private Set<Resource> setOf(Resource... resources) {
        return new HashSet<>(Arrays.asList(resources));
    }
}
