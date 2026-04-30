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
import java.util.Map;

/**
 * Created by caojiajun on 2026/4/29
 */
public class CheckUtilTest {

    @Test
    public void shouldAcceptValidSimpleTable() {
        ResourceTable table = ResourceTableUtil.simpleTable(new Resource("redis://127.0.0.1:6379"));

        Assert.assertTrue(CheckUtil.checkResourceTable(table));
        Assert.assertTrue(CheckUtil.checkSimpleTable(table.getSimpleTable()));
    }

    @Test
    public void shouldRejectIncompleteSimpleTable() {
        ResourceTable.SimpleTable simpleTable = new ResourceTable.SimpleTable();
        ResourceTable table = new ResourceTable();
        table.setType(ResourceTable.Type.SIMPLE);
        table.setSimpleTable(simpleTable);

        Assert.assertFalse(CheckUtil.checkResourceTable(table));
        Assert.assertFalse(CheckUtil.checkResourceOperation(null));
        Assert.assertFalse(CheckUtil.checkResource(new Resource("")));
    }

    @Test
    public void shouldAcceptReadWriteSeparateOperation() {
        ResourceReadOperation readOperation = new ResourceReadOperation(ResourceReadOperation.Type.ORDER,
                Arrays.asList(new Resource("redis://read-1"), new Resource("redis://read-2")));
        ResourceWriteOperation writeOperation = new ResourceWriteOperation(
                Arrays.asList(new Resource("redis://write-1"), new Resource("redis://write-2")));
        ResourceOperation operation = new ResourceOperation(readOperation, writeOperation);

        Assert.assertTrue(CheckUtil.checkResourceOperation(operation));
        Assert.assertTrue(CheckUtil.checkResourceReadOperation(readOperation));
        Assert.assertTrue(CheckUtil.checkResourceWriteOperation(writeOperation));
    }

    @Test
    public void shouldRejectReadWriteOperationWithEmptyResourceList() {
        ResourceReadOperation readOperation = new ResourceReadOperation(ResourceReadOperation.Type.RANDOM,
                Arrays.<Resource>asList());
        ResourceWriteOperation writeOperation = new ResourceWriteOperation(Arrays.<Resource>asList());

        Assert.assertFalse(CheckUtil.checkResourceReadOperation(readOperation));
        Assert.assertFalse(CheckUtil.checkResourceWriteOperation(writeOperation));
    }

    @Test
    public void shouldValidateShardingTableBuckets() {
        Map<Integer, ResourceOperation> resourceOperationMap = new HashMap<>();
        resourceOperationMap.put(0, new ResourceOperation(new Resource("redis://bucket-0")));
        resourceOperationMap.put(1, new ResourceOperation(new Resource("redis://bucket-1")));

        ResourceTable.ShadingTable table = new ResourceTable.ShadingTable();
        table.setBucketSize(2);
        table.setResourceOperationMap(resourceOperationMap);

        Assert.assertTrue(CheckUtil.checkShardingTable(table));
    }

    @Test
    public void shouldRejectShardingTableWhenBucketIndexIsMissing() {
        Map<Integer, ResourceOperation> resourceOperationMap = new HashMap<>();
        resourceOperationMap.put(0, new ResourceOperation(new Resource("redis://bucket-0")));
        resourceOperationMap.put(2, new ResourceOperation(new Resource("redis://bucket-2")));

        ResourceTable.ShadingTable table = new ResourceTable.ShadingTable();
        table.setBucketSize(2);
        table.setResourceOperationMap(resourceOperationMap);

        Assert.assertFalse(CheckUtil.checkShardingTable(table));
    }
}
