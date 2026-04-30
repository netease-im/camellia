package com.netease.nim.camellia.core.util;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.model.operation.ResourceOperation;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by caojiajun on 2026/4/29
 */
public class ReadableResourceTableUtilTest {

    @Test
    public void shouldParsePlainUrlAsSimpleTable() {
        ResourceTable table = ReadableResourceTableUtil.parseTable("redis://127.0.0.1:6379");

        Assert.assertEquals(ResourceTable.Type.SIMPLE, table.getType());
        Assert.assertTrue(CheckUtil.checkResourceTable(table));
        Assert.assertEquals(new Resource("redis://127.0.0.1:6379"),
                table.getSimpleTable().getResourceOperation().getResource());
        Assert.assertEquals("redis://127.0.0.1:6379", ReadableResourceTableUtil.readableResourceTable(table));
    }

    @Test
    public void shouldParseReadWriteSeparateSimpleTable() {
        String tableJson = "{\"type\":\"simple\",\"operation\":{\"type\":\"rw_separate\","
                + "\"read\":{\"type\":\"order\",\"resources\":[\"redis://read-1\",\"redis://read-2\"]},"
                + "\"write\":{\"type\":\"multi\",\"resources\":[\"redis://write-1\",\"redis://write-2\"]}}}";

        ResourceTable table = ReadableResourceTableUtil.parseTable(tableJson);

        Assert.assertEquals(ResourceTable.Type.SIMPLE, table.getType());
        Assert.assertTrue(CheckUtil.checkResourceTable(table));
        Assert.assertEquals(ResourceOperation.Type.RW_SEPARATE,
                table.getSimpleTable().getResourceOperation().getType());
        Assert.assertEquals(2,
                table.getSimpleTable().getResourceOperation().getReadOperation().getReadResources().size());
        Assert.assertEquals(2,
                table.getSimpleTable().getResourceOperation().getWriteOperation().getWriteResources().size());
    }

    @Test
    public void shouldParseShardingAliasAndExpandedBucketKeys() {
        String tableJson = "{\"type\":\"sharding\",\"operation\":{\"bucketSize\":3,"
                + "\"operationMap\":{\"0-2\":\"redis://bucket-a\","
                + "\"1\":{\"type\":\"simple\",\"resource\":\"redis://bucket-b\"}}}}";

        ResourceTable table = ReadableResourceTableUtil.parseTable(tableJson);

        Assert.assertEquals(ResourceTable.Type.SHADING, table.getType());
        Assert.assertTrue(CheckUtil.checkResourceTable(table));
        Assert.assertEquals(3, table.getShadingTable().getBucketSize());
        Assert.assertEquals(new Resource("redis://bucket-a"),
                table.getShadingTable().getResourceOperationMap().get(0).getResource());
        Assert.assertEquals(new Resource("redis://bucket-b"),
                table.getShadingTable().getResourceOperationMap().get(1).getResource());
        Assert.assertEquals(new Resource("redis://bucket-a"),
                table.getShadingTable().getResourceOperationMap().get(2).getResource());
    }

    @Test
    public void shouldRejectUnknownResourceTableType() {
        try {
            ReadableResourceTableUtil.parseTable("{\"type\":\"unknown\",\"operation\":{}}");
            Assert.fail("unknown table type should be rejected");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("unknown resource table type, only support [simple|sharding]", e.getMessage());
        }
    }
}
