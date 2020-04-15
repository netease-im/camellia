package com.netease.nim.camellia.core.model.operation;


import com.netease.nim.camellia.core.model.Resource;

import java.util.List;

/**
 *
 * Created by caojiajun on 2019/5/16.
 */
public class ResourceWriteOperation {
    private Type type;
    private Resource writeResource;
    private List<Resource> writeResources;

    public ResourceWriteOperation() {
    }

    public ResourceWriteOperation(Resource writeResource) {
        this.type = Type.SIMPLE;
        this.writeResource = writeResource;
    }

    public ResourceWriteOperation(List<Resource> writeResources) {
        this.type = Type.MULTI;
        this.writeResources = writeResources;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Resource getWriteResource() {
        return writeResource;
    }

    public void setWriteResource(Resource writeResource) {
        this.writeResource = writeResource;
    }

    public List<Resource> getWriteResources() {
        return writeResources;
    }

    public void setWriteResources(List<Resource> writeResources) {
        this.writeResources = writeResources;
    }

    public static enum Type {

        //单写
        SIMPLE,

        //多写
        MULTI,
        ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceWriteOperation that = (ResourceWriteOperation) o;

        if (type != that.type) return false;
        if (writeResource != null ? !writeResource.equals(that.writeResource) : that.writeResource != null)
            return false;
        return writeResources != null ? writeResources.equals(that.writeResources) : that.writeResources == null;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (writeResource != null ? writeResource.hashCode() : 0);
        result = 31 * result + (writeResources != null ? writeResources.hashCode() : 0);
        return result;
    }
}
