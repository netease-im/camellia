
### 生成其他复杂的ResourceTable的方法

ResourceTableUtil提供几个常用的工具方法去生成不同的ResourceTable，如：单写单读、双写单读、读写分离、简单分片等，如下：  
```java
public class ResourceTableUtil {

    /**
     * 单写单读
     */
    public static ResourceTable simpleTable(Resource resource) {
        ResourceOperation resourceOperation = new ResourceOperation(resource);

        ResourceTable.SimpleTable simpleTable = new ResourceTable.SimpleTable();
        simpleTable.setResourceOperation(resourceOperation);

        ResourceTable table = new ResourceTable();
        table.setType(ResourceTable.Type.SIMPLE);
        table.setSimpleTable(simpleTable);
        return table;
    }

    /**
     * 双写单读
     */
    public static ResourceTable simple2W1RTable(Resource readResource, Resource writeResource1, Resource writeResource2) {
        ResourceReadOperation readOperation = new ResourceReadOperation(readResource);
        ResourceWriteOperation writeOperation = new ResourceWriteOperation(Arrays.asList(writeResource1, writeResource2));

        ResourceOperation resourceOperation = new ResourceOperation(readOperation, writeOperation);

        ResourceTable.SimpleTable simpleTable = new ResourceTable.SimpleTable();
        simpleTable.setResourceOperation(resourceOperation);

        ResourceTable table = new ResourceTable();
        table.setType(ResourceTable.Type.SIMPLE);
        table.setSimpleTable(simpleTable);
        return table;
    }

    /**
     * 读写分离
     */
    public static ResourceTable simpleRwSeparateTable(Resource readResource, Resource writeResource) {
        ResourceReadOperation readOperation = new ResourceReadOperation(readResource);
        ResourceWriteOperation writeOperation = new ResourceWriteOperation(writeResource);

        ResourceOperation resourceOperation = new ResourceOperation(readOperation, writeOperation);

        ResourceTable.SimpleTable simpleTable = new ResourceTable.SimpleTable();
        simpleTable.setResourceOperation(resourceOperation);

        ResourceTable table = new ResourceTable();
        table.setType(ResourceTable.Type.SIMPLE);
        table.setSimpleTable(simpleTable);
        return table;
    }

    /**
     * 不带N读N写的分片
     */
    public static ResourceTable simpleShardingTable(Map<Integer, Resource> resourceMap, int bucketSize) {
        ResourceTable.ShardingTable shardingTable = new ResourceTable.ShardingTable();
        shardingTable.setBucketSize(bucketSize);
        Map<Integer, ResourceOperation> resourceOperationMap = new HashMap<>();
        for (int i=0; i<bucketSize; i++) {
            Resource resource = resourceMap.get(i);
            if (resource == null) {
                throw new IllegalArgumentException("resourceMap/bucketSize not match");
            }
            resourceOperationMap.put(i, new ResourceOperation(resource));
        }
        shardingTable.setResourceOperationMap(resourceOperationMap);

        ResourceTable table = new ResourceTable();
        table.setType(ResourceTable.Type.SHARDING);
        table.setShardingTable(shardingTable);
        return table;
    }
}

```
更普遍的生成自定义的ResourceTable的方法是使用json去描述，然后调用ReadableResourceTableUtil的parse方法去解析，如下：  
```java
public class TestJsonResourceTable {
    public static void testJsonResourceTable() {
        String json = "{\n" +
                "  \"type\": \"sharding\",\n" +
                "  \"operation\": {\n" +
                "    \"operationMap\": {\n" +
                "      \"4\": {\n" +
                "        \"read\": \"redis://password1@127.0.0.1:6379\",\n" +
                "        \"type\": \"rw_separate\",\n" +
                "        \"write\": {\n" +
                "          \"resources\": [\n" +
                "            \"redis://password1@127.0.0.1:6379\",\n" +
                "            \"redis://password2@127.0.0.1:6380\"\n" +
                "          ],\n" +
                "          \"type\": \"multi\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"0-2\": \"redis-cluster://@127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381\",\n" +
                "      \"1-3-5\": \"redis://password2@127.0.0.1:6380\"\n" +
                "    },\n" +
                "    \"bucketSize\": 6\n" +
                "  }\n" +
                "}";
        //ReadableResourceTableUtil的parseTable方法传入的字符串也可以是单个的地址，如：
        //ReadableResourceTableUtil.parseTable("redis://@127.0.0.1:6379");
        ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(json);
        System.out.println(ReadableResourceTableUtil.readableResourceTable(resourceTable));
    }

    public static void main(String[] args) {
        testJsonResourceTable();
    }
}

```
更多的json格式参见[resource-table-samples](resource-table-samples.md)