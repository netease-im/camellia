package com.netease.nim.camellia.core.util;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.model.operation.ResourceOperation;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by caojiajun on 2026/4/29
 */
public class ResourceTableUtilTest {

    @Test
    public void shouldBuildValidHelperTables() {
        Resource read = new Resource("redis://read");
        Resource write1 = new Resource("redis://write-1");
        Resource write2 = new Resource("redis://write-2");

        Assert.assertTrue(CheckUtil.checkResourceTable(ResourceTableUtil.simpleTable(read)));
        Assert.assertTrue(CheckUtil.checkResourceTable(ResourceTableUtil.simpleRwSeparateTable(read, write1)));
        Assert.assertTrue(CheckUtil.checkResourceTable(ResourceTableUtil.simple2W1RTable(read, write1, write2)));
    }

    @Test
    public void shouldCreateImmutableResourceTableCopies() {
        ResourceTable table = ResourceTableUtil.simple2W1RTable(new Resource("redis://read"),
                new Resource("redis://write-1"), new Resource("redis://write-2"));

        ResourceTable immutableTable = ResourceTableUtil.immutableResourceTable(table);

        try {
            immutableTable.getSimpleTable().getResourceOperation().getWriteOperation()
                    .getWriteResources().add(new Resource("redis://write-3"));
            Assert.fail("write resources should be immutable");
        } catch (UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void shouldCreateImmutableShardingOperationMap() {
        Map<Integer, Resource> resourceMap = new HashMap<>();
        resourceMap.put(0, new Resource("redis://bucket-0"));
        resourceMap.put(1, new Resource("redis://bucket-1"));
        ResourceTable table = ResourceTableUtil.simpleShardingTable(resourceMap, 2);

        ResourceTable immutableTable = ResourceTableUtil.immutableResourceTable(table);

        try {
            immutableTable.getShadingTable().getResourceOperationMap()
                    .put(2, new ResourceOperation(new Resource("redis://bucket-2")));
            Assert.fail("sharding operation map should be immutable");
        } catch (UnsupportedOperationException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void shouldRejectShardingTableWhenResourceMapDoesNotCoverBucketSize() {
        Map<Integer, Resource> resourceMap = new HashMap<>();
        resourceMap.put(0, new Resource("redis://bucket-0"));

        try {
            ResourceTableUtil.simpleShardingTable(resourceMap, 2);
            Assert.fail("resourceMap and bucketSize mismatch should be rejected");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("resourceMap/bucketSize not match", e.getMessage());
        }
    }
}
