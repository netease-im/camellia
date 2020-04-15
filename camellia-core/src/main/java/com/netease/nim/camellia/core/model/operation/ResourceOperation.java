package com.netease.nim.camellia.core.model.operation;


import com.netease.nim.camellia.core.model.Resource;

/**
 *
 * Created by caojiajun on 2019/5/15.
 */
public class ResourceOperation {

    private Type type;
    private Resource resource;
    private ResourceReadOperation readOperation;
    private ResourceWriteOperation writeOperation;

    public ResourceOperation() {
    }

    public ResourceOperation(Resource resource) {
        this.type = Type.SIMPLE;
        this.resource = resource;
    }

    public ResourceOperation(ResourceReadOperation readOperation, ResourceWriteOperation writeOperation) {
        this.type = Type.RW_SEPARATE;
        this.readOperation = readOperation;
        this.writeOperation = writeOperation;
    }

    public static enum Type {

        //单个资源
        SIMPLE,

        //读写分离
        RW_SEPARATE,
        ;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public ResourceReadOperation getReadOperation() {
        return readOperation;
    }

    public void setReadOperation(ResourceReadOperation readOperation) {
        this.readOperation = readOperation;
    }

    public ResourceWriteOperation getWriteOperation() {
        return writeOperation;
    }

    public void setWriteOperation(ResourceWriteOperation writeOperation) {
        this.writeOperation = writeOperation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceOperation that = (ResourceOperation) o;

        if (type != that.type) return false;
        if (resource != null ? !resource.equals(that.resource) : that.resource != null) return false;
        if (readOperation != null ? !readOperation.equals(that.readOperation) : that.readOperation != null)
            return false;
        return writeOperation != null ? writeOperation.equals(that.writeOperation) : that.writeOperation == null;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (resource != null ? resource.hashCode() : 0);
        result = 31 * result + (readOperation != null ? readOperation.hashCode() : 0);
        result = 31 * result + (writeOperation != null ? writeOperation.hashCode() : 0);
        return result;
    }
}
