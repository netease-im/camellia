package com.netease.nim.camellia.console.model;

import java.util.Map;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class TableDetail {
    private String type;
    private Object operation;

    public static SimpleTable simpleTable(String o) {
        SimpleTable simpleTable=new SimpleTable();
        simpleTable.setType("simple");
        simpleTable.setResource(o);
        return simpleTable;
    }

    public static ShardingSimpleTable shardingSimpleTable(String o){
        ShardingSimpleTable shardingSimpleTable=new ShardingSimpleTable();
        shardingSimpleTable.setType("simple");
        shardingSimpleTable.setResource(o);
        return shardingSimpleTable;

    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getOperation() {
        return operation;
    }

    public void setOperation(Object operation) {
        this.operation = operation;
    }

    public static class SimpleTable{
        private String type;
        private String resource;
        private ReadSource read;
        private WriteSource write;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public ReadSource getRead() {
            return read;
        }

        public void setRead(ReadSource read) {
            this.read = read;
        }

        public WriteSource getWrite() {
            return write;
        }

        public void setWrite(WriteSource write) {
            this.write = write;
        }
    }

    public static class ReadSource{
        private String[] resources;
        private String type;

        public String[] getResources() {
            return resources;
        }

        public void setResources(String[] resources) {
            this.resources = resources;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class WriteSource{
        private String[] resources;
        private String type;

        public String[] getResources() {
            return resources;
        }

        public void setResources(String[] resources) {
            this.resources = resources;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class ShardingTable{
        private Map<String,SimpleTable> operationMap;
        private Integer bucketSize;

        public Map<String, SimpleTable> getOperationMap() {
            return operationMap;
        }

        public void setOperationMap(Map<String, SimpleTable> operationMap) {
            this.operationMap = operationMap;
        }

        public Integer getBucketSize() {
            return bucketSize;
        }

        public void setBucketSize(Integer bucketSize) {
            this.bucketSize = bucketSize;
        }
    }

    public static class ShardingTableConsole{
        private  ShardingSimpleTable[]  operationMap;
        private Integer bucketSize;

        public ShardingSimpleTable[] getOperationMap() {
            return operationMap;
        }

        public void setOperationMap(ShardingSimpleTable[] operationMap) {
            this.operationMap = operationMap;
        }

        public Integer getBucketSize() {
            return bucketSize;
        }

        public void setBucketSize(Integer bucketSize) {
            this.bucketSize = bucketSize;
        }
    }

    public static class ShardingSimpleTable extends SimpleTable{
        private String key;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }

}
