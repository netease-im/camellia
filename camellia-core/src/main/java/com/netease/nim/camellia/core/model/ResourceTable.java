package com.netease.nim.camellia.core.model;


import com.netease.nim.camellia.core.model.operation.ResourceOperation;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2019/5/15.
 */
public class ResourceTable {

    //类型
    private Type type;
    //simple
    private SimpleTable simpleTable;
    //分片
    private ShadingTable shadingTable;

    public ResourceTable() {
    }

    public ResourceTable(SimpleTable simpleTable) {
        this.type = Type.SIMPLE;
        this.simpleTable = simpleTable;
    }

    public ResourceTable(ShadingTable shadingTable) {
        this.type = Type.SHADING;
        this.shadingTable = shadingTable;
    }

    public static enum Type {

        SIMPLE(1),
        SHADING(2),
        ;

        private final int value;

        Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Type getByValue(int value) {
            for (Type type : Type.values()) {
                if (type.getValue() == value) {
                    return type;
                }
            }
            return null;
        }
    }

    public static class ShadingTable {
        private int bucketSize;
        private Map<Integer, ResourceOperation> resourceOperationMap = new HashMap<>();

        public int getBucketSize() {
            return bucketSize;
        }

        public void setBucketSize(int bucketSize) {
            this.bucketSize = bucketSize;
        }

        public Map<Integer, ResourceOperation> getResourceOperationMap() {
            return resourceOperationMap;
        }

        public void setResourceOperationMap(Map<Integer, ResourceOperation> resourceOperationMap) {
            this.resourceOperationMap = resourceOperationMap;
        }
    }

    public static class SimpleTable {

        private ResourceOperation resourceOperation;

        public ResourceOperation getResourceOperation() {
            return resourceOperation;
        }

        public void setResourceOperation(ResourceOperation resourceOperation) {
            this.resourceOperation = resourceOperation;
        }
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public ShadingTable getShadingTable() {
        return shadingTable;
    }

    public void setShadingTable(ShadingTable shadingTable) {
        this.shadingTable = shadingTable;
    }

    public SimpleTable getSimpleTable() {
        return simpleTable;
    }

    public void setSimpleTable(SimpleTable simpleTable) {
        this.simpleTable = simpleTable;
    }
}
